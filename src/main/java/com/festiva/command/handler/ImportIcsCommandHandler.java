package com.festiva.command.handler;

import com.festiva.ai.IcsNameExtractorService;
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
import org.springframework.beans.factory.annotation.Autowired;
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
import java.net.URLConnection;
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
@SuppressWarnings("unused")
public class ImportIcsCommandHandler implements StatefulCommandHandler {

    public static final String CALLBACK_ICS_CONFIRM = "ICS_CONFIRM";
    public static final String CALLBACK_ICS_CANCEL  = "ICS_CANCEL";

    private static final DateTimeFormatter ICS_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.ROOT);
    private static final DateTimeFormatter CSV_DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.ROOT);
    private static final int FILE_CONNECT_TIMEOUT_MILLIS = 10_000;
    private static final int FILE_READ_TIMEOUT_MILLIS = 10_000;
    private static final int MAX_ICS_FILE_SIZE_BYTES = 512 * 1024;

    private final FriendService friendService;
    private final UserStateService userStateService;
    private final TelegramClient telegramClient;

    @Autowired(required = false)
    private IcsNameExtractorService icsNameExtractorService;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Override
    public String command() {
        return "/importics";
    }

    @Override
    public Set<BotState> handledStates() {
        return Set.of(BotState.WAITING_FOR_ICS_FILE);
    }

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
        if (doc.getFileSize() != null && doc.getFileSize() > MAX_ICS_FILE_SIZE_BYTES) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.ICS_TOO_LARGE));
        }

        List<String> lines = downloadLines(doc.getFileId());
        if (lines == null) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.ICS_PARSE_ERROR));
        }

        List<IcsEntry> entries = extractYearlyEntries(lines);
        if (entries.isEmpty()) {
            userStateService.clearState(userId);
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.ICS_NO_EVENTS));
        }

        List<Friend> candidates = new ArrayList<>();
        int invalidEntryCount = 0;
        for (IcsEntry entry : entries) {
            try {
                candidates.add(convertToFriend(entry));
            } catch (IllegalArgumentException e) {
                invalidEntryCount++;
            }
        }

        Set<String> existing = friendService.getFriends(userId).stream()
                .map(friend -> Friend.normalizeName(friend.getName()))
                .collect(Collectors.toSet());

        List<Friend> toSave = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Set<String> seenInBatch = new java.util.HashSet<>();

        for (Friend friend : candidates) {
            String normalized = Friend.normalizeName(friend.getName());
            if (existing.contains(normalized)) {
                errors.add(Messages.get(lang, Messages.BULK_ERROR_EXISTS, 0, friend.getName()));
            } else if (seenInBatch.contains(normalized)) {
                errors.add(Messages.get(lang, Messages.BULK_ERROR_DUPLICATE, 0, friend.getName()));
            } else if (friend.getName().length() > 100) {
                errors.add(Messages.get(lang, Messages.BULK_ERROR_NAME_LONG, 0));
            } else {
                seenInBatch.add(normalized);
                toSave.add(friend);
            }
        }

        int currentCount = existing.size();
        if (currentCount + toSave.size() > FriendService.FRIEND_CAP) {
            int allowed = Math.max(0, FriendService.FRIEND_CAP - currentCount);
            if (allowed < toSave.size()) {
                errors.add(Messages.get(lang, Messages.BULK_CAP_EXCEEDED, allowed, FriendService.FRIEND_CAP));
                toSave = toSave.subList(0, allowed);
            }
        }

        if (toSave.isEmpty()) {
            userStateService.clearState(userId);
            String preview = buildPreviewLines(List.of(), errors);
            return MessageBuilder.html(chatId,
                    Messages.get(lang, Messages.ICS_PREVIEW_NO_VALID, entries.size(), preview));
        }

        userStateService.setPendingIcsImport(userId, toSave);
        userStateService.setState(userId, BotState.WAITING_FOR_ICS_CONFIRM);

        String preview = buildPreviewLines(toSave, errors);
        String text = Messages.get(lang, Messages.ICS_PREVIEW, entries.size(), preview, toSave.size());
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .keyboard(List.of(new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text(Messages.get(lang, Messages.ICS_CONFIRM_BTN, toSave.size()))
                                .callbackData(CALLBACK_ICS_CONFIRM).build(),
                        InlineKeyboardButton.builder()
                                .text(Messages.get(lang, Messages.ICS_CANCEL_BTN))
                                .callbackData(CALLBACK_ICS_CANCEL).build()
                ))).build();
        return MessageBuilder.html(chatId, text, kb);
    }

    public record IcsEntry(String summary, LocalDate date, boolean yearTrusted) {}

    private static boolean isSummaryLine(String line) {
        return line.regionMatches(true, 0, "SUMMARY", 0, "SUMMARY".length())
                && line.length() > "SUMMARY".length()
                && (line.charAt("SUMMARY".length()) == ':' || line.charAt("SUMMARY".length()) == ';');
    }

    private static String valueAfterColon(String line) {
        int colon = line.indexOf(':');
        return colon >= 0 ? line.substring(colon + 1) : null;
    }

    private static boolean looksLikeBirthdaySummary(String summary) {
        if (summary == null) {
            return false;
        }
        String s = summary.toLowerCase(Locale.ROOT);
        return s.matches(".*\\b(birthday|bday|born|день рождения|др)\\b.*");
    }

    public static List<IcsEntry> extractYearlyEntries(List<String> raw) {
        List<String> unfolded = unfold(raw);
        List<IcsEntry> result = new ArrayList<>();
        String summary = null;
        String dtstart = null;
        boolean inEvent = false;
        boolean yearly = false;
        boolean isBirthday = false;

        for (String line : unfolded) {
            if (line.equals("BEGIN:VEVENT")) {
                inEvent = true;
                summary = null;
                dtstart = null;
                yearly = false;
                isBirthday = false;
            } else if (line.equals("END:VEVENT")) {
                if (inEvent && dtstart != null && summary != null) {
                    boolean accept = isBirthday || (yearly && looksLikeBirthdaySummary(summary));

                    if (accept) {
                        LocalDate date = parseIcsDate(dtstart);
                        if (date != null) {
                            boolean yearTrusted = date.isBefore(LocalDate.now()) &&
                                    date.getYear() > 1900 &&
                                    date.getYear() < LocalDate.now().getYear();
                            result.add(new IcsEntry(summary.trim(), date, yearTrusted));
                        }
                    }
                }
                inEvent = false;
            } else if (inEvent) {
                if (isSummaryLine(line)) {
                    String value = valueAfterColon(line);
                    if (value != null) {
                        summary = value;
                    }
                } else if (line.startsWith("DTSTART")) {
                    int colon = line.indexOf(':');
                    if (colon >= 0) {
                        dtstart = line.substring(colon + 1).trim();
                    }
                } else if (line.startsWith("RRULE:") && line.contains("FREQ=YEARLY")) {
                    yearly = true;
                } else if (line.startsWith("X-GOOGLE-CALENDAR-CONTENT-TYPE:")
                        && line.toLowerCase(Locale.ROOT).contains("birthday")) {
                    isBirthday = true;
                } else if (line.startsWith("CATEGORIES:")
                        && line.toLowerCase(Locale.ROOT).contains("birthday")) {
                    isBirthday = true;
                } else if (line.startsWith("BDAY:")) {
                    dtstart = line.substring(5).trim();
                    isBirthday = true;
                }
            }
        }
        return result;
    }

    public static List<String> extractYearlyCsvLines(List<String> raw) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.ROOT);
        return extractYearlyEntries(raw).stream()
                .map(entry -> entry.summary() + "," + entry.date().format(fmt))
                .toList();
    }

    private String resolveName(String summary) {
        if (icsNameExtractorService == null) {
            return summary == null ? null : summary.trim();
        }

        try {
            String extracted = icsNameExtractorService.extractName(summary);
            if (extracted == null || extracted.isBlank()) {
                return summary == null ? null : summary.trim();
            }
            return extracted.trim();
        } catch (Exception e) {
            log.atWarn()
                    .setMessage("ics_ai_name_extraction_failed")
                    .setCause(e)
                    .log();
            return summary == null ? null : summary.trim();
        }
    }

    private Friend convertToFriend(IcsEntry entry) {
        String name = resolveName(entry.summary());
        LocalDate date = entry.date();

        if (!entry.yearTrusted()) {
            return new Friend(name, null, date.getMonthValue(), date.getDayOfMonth());
        }

        return new Friend(name, date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }

    private static List<String> unfold(List<String> lines) {
        List<String> out = new ArrayList<>();
        for (String line : lines) {
            if (!line.isEmpty() && (line.charAt(0) == ' ' || line.charAt(0) == '\t')) {
                if (!out.isEmpty()) {
                    out.set(out.size() - 1, out.getLast() + line.substring(1));
                }
            } else {
                out.add(line.replace("\r", ""));
            }
        }
        return out;
    }

    private static LocalDate parseIcsDate(String value) {
        String datePart = value.length() >= 8 ? value.substring(0, 8) : value;
        try {
            return LocalDate.parse(datePart, ICS_DATE_FMT);
        } catch (Exception e) {
            return null;
        }
    }

    private static String buildPreviewLines(List<Friend> valid, List<String> errors) {
        StringBuilder sb = new StringBuilder();
        for (Friend friend : valid) {
            String dateStr = friend.hasYear()
                    ? friend.getBirthDate().format(CSV_DATE_FMT)
                    : String.format("%02d.%02d.", friend.getBirthMonthDay().getDayOfMonth(), friend.getBirthMonthDay().getMonthValue());
            sb.append("✅ ").append(friend.getName())
                    .append(" — ").append(dateStr).append("\n");
        }
        for (String error : errors) {
            sb.append("❌ ").append(error).append("\n");
        }
        return sb.toString().stripTrailing();
    }

    private List<String> downloadLines(String fileId) {
        try {
            org.telegram.telegrambots.meta.api.objects.File tgFile =
                    telegramClient.execute(GetFile.builder().fileId(fileId).build());

            String url = "https://api.telegram.org/file/bot" + botToken + "/" + tgFile.getFilePath();
            URLConnection connection = URI.create(url).toURL().openConnection();
            connection.setConnectTimeout(FILE_CONNECT_TIMEOUT_MILLIS);
            connection.setReadTimeout(FILE_READ_TIMEOUT_MILLIS);
            connection.setUseCaches(false);

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                return reader.lines().toList();
            }
        } catch (TelegramApiException | IOException e) {
            log.atWarn()
                    .setMessage("ics_import_file_download_failed")
                    .addKeyValue("fileId", fileId)
                    .setCause(e)
                    .log();
            return null;
        }
    }
}
