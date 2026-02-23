package com.festiva.bot;

import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.MaybeInaccessibleMessage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CallbackQueryHandler {

    private static final String MONTH_PREFIX = "MONTH_";
    private static final String CURRENT_MONTH = "CURRENT";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final FriendService friendService;

    public EditMessageText handle(CallbackQuery callbackQuery) {
        if (callbackQuery == null) {
            log.warn("Received null CallbackQuery");
            return null;
        }

        String data = callbackQuery.getData();
        if (data == null || !data.startsWith(MONTH_PREFIX)) {
            log.debug("Invalid callback data: {}", data);
            return null;
        }

        MaybeInaccessibleMessage message = callbackQuery.getMessage();
        if (message == null) {
            log.warn("CallbackQuery message is null");
            return null;
        }

        int month = parseMonth(data);
        String text = month < 0 ? "Ошибка при выборе месяца." : buildMonthText(message.getChatId(), month);

        return EditMessageText.builder()
                .chatId(message.getChatId())
                .messageId(message.getMessageId())
                .text(text)
                .build();
    }

    private int parseMonth(String data) {
        String value = data.substring(MONTH_PREFIX.length());
        if (CURRENT_MONTH.equalsIgnoreCase(value)) return 0;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.error("Failed to parse month from callback data: {}", data, e);
            return -1;
        }
    }

    private String buildMonthText(long telegramUserId, int month) {
        int monthToShow = (month == 0) ? LocalDate.now().getMonthValue() : month;
        List<Friend> filtered = friendService.getFriendsSortedByDayMonth(telegramUserId).stream()
                .filter(f -> f.getBirthDate().getMonthValue() == monthToShow)
                .toList();

        if (filtered.isEmpty()) {
            return "В выбранном месяце нет дней рождения.";
        }

        StringBuilder sb = new StringBuilder("Дни рождения в " + monthToShow + "-м месяце:\n");
        filtered.forEach(f -> sb.append("* ").append(f.getBirthDate().format(DATE_FORMATTER))
                .append(" ").append(f.getName())
                .append(" (сейчас пользователю ").append(f.getAge()).append(")\n"));
        return sb.toString();
    }
}
