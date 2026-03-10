package com.festiva.command.handler;

import com.festiva.command.MessageBuilder;
import com.festiva.command.StatefulCommandHandler;
import com.festiva.friend.api.FriendService;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.state.BotState;
import com.festiva.state.UserStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class BulkAddCommandHandler implements StatefulCommandHandler {

    public static final String CALLBACK_PASTE = "BULK_PASTE";
    public static final String CALLBACK_CSV   = "BULK_CSV";
    public static final String CALLBACK_ICS   = "BULK_ICS";

    private final FriendService friendService;
    private final UserStateService userStateService;
    private final TelegramClient telegramClient;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Override
    public String command() { return "/addmany"; }

    @Override
    public Set<BotState> handledStates() { return Set.of(BotState.WAITING_FOR_BULK_ADD); }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        Lang lang = userStateService.getLanguage(userId);

        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        new InlineKeyboardRow(
                                InlineKeyboardButton.builder().text(Messages.get(lang, Messages.BULK_ADD_PASTE_BTN)).callbackData(CALLBACK_PASTE).build(),
                                InlineKeyboardButton.builder().text(Messages.get(lang, Messages.BULK_ADD_CSV_BTN)).callbackData(CALLBACK_CSV).build()
                        ),
                        new InlineKeyboardRow(
                                InlineKeyboardButton.builder().text(Messages.get(lang, Messages.BULK_ADD_ICS_BTN)).callbackData(CALLBACK_ICS).build()
                        )
                )).build();

        return MessageBuilder.html(chatId, Messages.get(lang, Messages.BULK_ADD_CHOOSE), keyboard);
    }

    public void sendCsvTemplate(long chatId, Lang lang) {
        try {
            String csv = "name,birthday,relationship\nAlice,15.03.1990,friend\nBob,22.07.1985,\n";
            InputFile file = new InputFile(
                    new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)),
                    "friends_template.csv");
            telegramClient.execute(SendDocument.builder()
                    .chatId(chatId)
                    .document(file)
                    .caption(Messages.get(lang, Messages.BULK_ADD_CSV_CAPTION))
                    .build());
        } catch (TelegramApiException e) {
            log.warn("bulk.csv.template.failed: chatId={}", chatId, e);
        }
    }

    public SendMessage promptPaste(long chatId, long userId, Lang lang) {
        userStateService.setState(userId, BotState.WAITING_FOR_BULK_ADD);
        return MessageBuilder.html(chatId, Messages.get(lang, Messages.BULK_ADD_PROMPT));
    }

    @Override
    public SendMessage handleState(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        Lang lang = userStateService.getLanguage(userId);

        List<String> lines;

        if (update.getMessage().hasDocument()) {
            lines = downloadDocument(update);
            if (lines == null) {
                return MessageBuilder.html(chatId, Messages.get(lang, Messages.BULK_ADD_FILE_INVALID));
            }
        } else if (update.getMessage().hasText()) {
            lines = Arrays.asList(update.getMessage().getText().split("\n"));
        } else {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.BULK_ADD_FILE_INVALID));
        }

        Set<String> existing = friendService.getFriends(userId).stream()
                .map(f -> f.getName().toLowerCase(java.util.Locale.ROOT))
                .collect(Collectors.toSet());

        BulkAddParser.ParseResult result = BulkAddParser.parse(lines, existing, lang);

        if (result.noData()) {
            return MessageBuilder.html(chatId, result.errors().getFirst());
        }

        int currentCount = existing.size();
        List<com.festiva.friend.entity.Friend> toAdd = result.valid();
        List<String> errors = new ArrayList<>(result.errors());
        if (currentCount + toAdd.size() > FriendService.FRIEND_CAP) {
            int allowed = Math.max(0, FriendService.FRIEND_CAP - currentCount);
            if (allowed < toAdd.size()) {
                errors.add(Messages.get(lang, Messages.BULK_CAP_EXCEEDED, allowed, FriendService.FRIEND_CAP));
                toAdd = toAdd.subList(0, allowed);
            }
        }

        toAdd.forEach(f -> friendService.addFriend(userId, f));
        userStateService.clearState(userId);
        log.debug("bulk.add.done: userId={}, added={}, errors={}", userId, toAdd.size(), errors.size());

        return MessageBuilder.html(chatId, buildResponse(lang, new BulkAddParser.ParseResult(toAdd, errors, false)));
    }

    private String buildResponse(Lang lang, BulkAddParser.ParseResult result) {
        StringBuilder sb = new StringBuilder();
        if (!result.valid().isEmpty()) {
            sb.append(Messages.get(lang, Messages.BULK_ADD_SUCCESS, result.valid().size()));
        }
        if (!result.errors().isEmpty()) {
            String errorList = result.errors().stream()
                    .map(e -> "• " + e)
                    .collect(Collectors.joining("\n"));
            if (!sb.isEmpty()) sb.append("\n\n");
            sb.append(Messages.get(lang, Messages.BULK_ADD_ERRORS, result.errors().size(), errorList));
        }
        if (sb.isEmpty()) {
            sb.append(Messages.get(lang, Messages.BULK_ADD_EMPTY));
        }
        return sb.toString();
    }

    private List<String> downloadDocument(Update update) {
        try {
            var doc = update.getMessage().getDocument();
            String mime = doc.getMimeType();
            if (mime != null && !mime.startsWith("text/") && !mime.equals("application/octet-stream")) {
                log.warn("bulk.add.file.unsupported.mime: mime={}", mime);
                return null;
            }
            if (doc.getFileSize() != null && doc.getFileSize() > 512_000) {
                log.warn("bulk.add.file.too.large: size={}", doc.getFileSize());
                return null;
            }
            org.telegram.telegrambots.meta.api.objects.File tgFile =
                    telegramClient.execute(GetFile.builder().fileId(doc.getFileId()).build());
            String url = "https://api.telegram.org/file/bot" + botToken + "/" + tgFile.getFilePath();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(URI.create(url).toURL().openStream(), StandardCharsets.UTF_8))) {
                return reader.lines().toList();
            }
        } catch (TelegramApiException | IOException e) {
            log.warn("bulk.add.file.download.failed", e);
            return null;
        }
    }
}
