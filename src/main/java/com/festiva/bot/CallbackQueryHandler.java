package com.festiva.bot;

import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.state.UserStateService;
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
    private static final String REMOVE_PREFIX = "REMOVE_";
    private static final String CURRENT_MONTH = "CURRENT";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private static final String[] MONTH_NAMES = {
        "", "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    };

    private final FriendService friendService;
    private final UserStateService userStateService;

    public EditMessageText handle(CallbackQuery callbackQuery) {
        if (callbackQuery == null) return null;

        String data = callbackQuery.getData();
        MaybeInaccessibleMessage message = callbackQuery.getMessage();
        if (data == null || message == null) return null;

        long chatId = message.getChatId();
        long userId = callbackQuery.getFrom().getId();

        String text;
        if (data.startsWith(REMOVE_PREFIX)) {
            text = handleRemove(userId, data.substring(REMOVE_PREFIX.length()));
        } else if (data.startsWith(MONTH_PREFIX)) {
            text = handleMonth(chatId, data);
        } else {
            log.debug("Unknown callback data: {}", data);
            return null;
        }

        return EditMessageText.builder()
                .chatId(chatId)
                .messageId(message.getMessageId())
                .parseMode("HTML")
                .text(text)
                .build();
    }

    private String handleRemove(long userId, String name) {
        if (!friendService.friendExists(userId, name)) {
            return "Друг \"" + name + "\" не найден.";
        }
        friendService.deleteFriend(userId, name);
        userStateService.clearState(userId);
        return "✅ <b>" + name + "</b> удалён!";
    }

    private String handleMonth(long chatId, String data) {
        String value = data.substring(MONTH_PREFIX.length());
        int month;
        if (CURRENT_MONTH.equalsIgnoreCase(value)) {
            month = LocalDate.now().getMonthValue();
        } else {
            try {
                month = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                log.error("Failed to parse month from callback data: {}", data);
                return "Ошибка при выборе месяца.";
            }
        }

        List<Friend> filtered = friendService.getFriendsSortedByDayMonth(chatId).stream()
                .filter(f -> f.getBirthDate().getMonthValue() == month)
                .toList();

        if (filtered.isEmpty()) {
            return "В <b>" + MONTH_NAMES[month].toLowerCase() + "</b> нет дней рождения.";
        }

        StringBuilder sb = new StringBuilder("🎂 <b>Дни рождения — " + MONTH_NAMES[month] + "</b>\n\n");
        filtered.forEach(f -> sb.append("– <b>").append(f.getBirthDate().format(DATE_FORMATTER))
                .append("</b> ").append(f.getName())
                .append(" (<i>").append(f.getAge()).append(" лет</i>)\n"));
        return sb.toString();
    }
}
