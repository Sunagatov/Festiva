package com.festiva.command.handler;

import com.festiva.command.MessageBuilder;
import com.festiva.command.StatefulCommandHandler;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImportIcsCommandHandler implements StatefulCommandHandler {

    public static final String CALLBACK_ICS_CONFIRM = "ICS_CONFIRM";
    public static final String CALLBACK_ICS_CANCEL  = "ICS_CANCEL";

    private static final DateTimeFormatter ICS_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.ROOT);
    private static final DateTimeFormatter CSV_DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.ROOT);

    private final FriendService friendService;
    private final UserStateService userStateService;
    private final TelegramClient telegramClient;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Override
    public String command() { return "/importics"; }

    @Override
    public Set<BotState> handledStates() { return Set.of(BotState.WAITING_FOR_ICS_FILE); }

    @Override
    public SendMessage handle(Update update) {
        long userId = update.getMessage().getFrom().getId();
        long chatId = update.getMessage().getChatId();
        Lang lang = userStateService.getLanguage(userId);
        userStateService.setState(userId, BotState.WAITING_FOR_ICS_FILE);
        return MessageBuilder.html(chatId, Messages.get(lang, Messages.ICS_PROMPT));
    }

    @Override
    public SendMessage handleState(Update update) {
        long userId = update.getMessage().getFrom().getId();
        long chatId = update.getMessage().getChatId();
        Lang lang = userStateService.getLanguage(userId);

        if (!update.getMessage().hasDocument()) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.ICS_NOT_A_FILE));
        }

        var doc = update.getMessage().getDocument();
        String mime = doc.getMimeType();
        if (mime != null && !mime.startsWith("text/") && !mime.equals("application/octet-stream")) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.ICS_WRONG_TYPE));
        }
        if (doc.getFileSize() != null && doc.getFileSize() > 512_000) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.ICS_TOO_LARGE));
        }

        List<String> lines = downloadLines(doc.getFileId());
        if (lines == null) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.ICS_PARSE_ERROR));
        }

        List<String> csvLines = extractYearlyCsvLines(lines);
        if (csvLines.isEmpty()) {
            userStateService.clearState(userId);
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.ICS_NO_EVENTS));
        }

        Set<String> existing = friendService.getFriends(userId).stream()
                .map(f -> f.getName().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        BulkAddParser.ParseResult result = BulkAddParser.parse(csvLines, existing, lang);

        // enforce friend cap on valid entries
        List<Friend> toSave = result.valid();
        List<String> errors = new ArrayList<>(result.errors());
        int currentCount = existing.size();
        if (currentCount + toSave.size() > FriendService.FRIEND_CAP) {
            int allowed = Math.max(0, FriendService.FRIEND_CAP - currentCount);
            toSave = toSave.subList(0, allowed);
        }

        int yearlyCount = csvLines.size();

        if (toSave.isEmpty()) {
            userStateService.clearState(userId);
            String preview = buildPreviewLines(List.of(), errors);
            return MessageBuilder.html(chatId,
                    Messages.get(lang, Messages.ICS_PREVIEW_NO_VALID, yearlyCount, preview));
        }

        userStateService.setPendingIcsImport(userId, toSave);
        userStateService.setState(userId, BotState.WAITING_FOR_ICS_CONFIRM);

        String preview = buildPreviewLines(toSave, errors);
        String text = Messages.get(lang, Messages.ICS_PREVIEW, yearlyCount, preview, toSave.size());
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .keyboard(List.of(new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text(Messages.get(lang, Messages.ICS_CONFIRM_BTN, toSave.size()))
                                .callbackData(CALLBACK_ICS_CONFIRM).build(),
                        InlineKeyboardButton.builder()
                                .text("\u274c Cancel")
                                .callbackData(CALLBACK_ICS_CANCEL).build()
                ))).build();
        return MessageBuilder.html(chatId, text, kb);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Unfolds ICS lines and extracts VEVENT blocks with RRULE:FREQ=YEARLY, returning "Name,DD.MM.YYYY" lines. */
    public static List<String> extractYearlyCsvLines(List<String> raw) {
        List<String> unfolded = unfold(raw);
        List<String> result = new ArrayList<>();
        String summary = null, dtstart = null;
        boolean inEvent = false, yearly = false;

        for (String line : unfolded) {
            if (line.equals("BEGIN:VEVENT")) {
                inEvent = true; summary = null; dtstart = null; yearly = false;
            } else if (line.equals("END:VEVENT")) {
                if (inEvent && yearly && summary != null && dtstart != null) {
                    LocalDate date = parseIcsDate(dtstart);
                    if (date != null) {
                        result.add(summary.trim() + "," + date.format(CSV_DATE_FMT));
                    }
                }
                inEvent = false;
            } else if (inEvent) {
                if (line.startsWith("SUMMARY:")) {
                    summary = line.substring(8);
                } else if (line.startsWith("DTSTART")) {
                    // DTSTART:YYYYMMDD or DTSTART;VALUE=DATE:YYYYMMDD or DTSTART:YYYYMMDDTHHMMSSZ
                    int colon = line.indexOf(':');
                    if (colon >= 0) dtstart = line.substring(colon + 1).trim();
                } else if (line.startsWith("RRULE:") && line.contains("FREQ=YEARLY")) {
                    yearly = true;
                }
            }
        }
        return result;
    }

    private static List<String> unfold(List<String> lines) {
        List<String> out = new ArrayList<>();
        for (String line : lines) {
            if (!line.isEmpty() && (line.charAt(0) == ' ' || line.charAt(0) == '\t')) {
                if (!out.isEmpty()) out.set(out.size() - 1, out.get(out.size() - 1) + line.substring(1));
            } else {
                out.add(line.replace("\r", ""));
            }
        }
        return out;
    }

    private static LocalDate parseIcsDate(String value) {
        // strip time part if present: YYYYMMDDTHHMMSSZ → YYYYMMDD
        String datePart = value.length() >= 8 ? value.substring(0, 8) : value;
        try {
            return LocalDate.parse(datePart, ICS_DATE_FMT);
        } catch (Exception e) {
            return null;
        }
    }

    private static String buildPreviewLines(List<Friend> valid, List<String> errors) {
        StringBuilder sb = new StringBuilder();
        for (Friend f : valid) {
            sb.append("\u2705 ").append(f.getName())
              .append(" \u2014 ").append(f.getBirthDate().format(CSV_DATE_FMT)).append("\n");
        }
        for (String err : errors) {
            sb.append("\u274c ").append(err).append("\n");
        }
        return sb.toString().stripTrailing();
    }

    private List<String> downloadLines(String fileId) {
        try {
            org.telegram.telegrambots.meta.api.objects.File tgFile =
                    telegramClient.execute(GetFile.builder().fileId(fileId).build());
            String url = "https://api.telegram.org/file/bot" + botToken + "/" + tgFile.getFilePath();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(URI.create(url).toURL().openStream(), StandardCharsets.UTF_8))) {
                return reader.lines().toList();
            }
        } catch (TelegramApiException | IOException e) {
            log.warn("ics.import.invalid_file", e);
            return null;
        }
    }
}
