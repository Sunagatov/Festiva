package com.festiva.command.handler;

import com.festiva.command.CommandHandler;
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
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class BulkAddCommandHandler implements StatefulCommandHandler {

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
                .map(f -> f.getName().toLowerCase())
                .collect(Collectors.toSet());

        BulkAddParser.ParseResult result = BulkAddParser.parse(lines, existing);

        if (result.valid().isEmpty() && result.errors().isEmpty()) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.BULK_ADD_EMPTY));
        }

        result.valid().forEach(f -> friendService.addFriend(userId, f));
        userStateService.clearState(userId);
        log.debug("bulk.add.done: userId={}, added={}, errors={}", userId, result.valid().size(), result.errors().size());

        return MessageBuilder.html(chatId, buildResponse(lang, result));
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
                return reader.lines().collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("bulk.add.file.download.failed: message={}", e.getMessage());
            return null;
        }
    }
}
