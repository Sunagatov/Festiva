package com.festiva.bot;

import com.festiva.command.MessageBuilder;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.state.UserStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.MaybeInaccessibleMessage;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CallbackQueryHandler {

    private static final String MONTH_PREFIX  = "MONTH_";
    private static final String REMOVE_PREFIX = "REMOVE_";
    private static final String LANG_PREFIX   = "LANG_";
    private static final String CURRENT_MONTH = "CURRENT";

    private final FriendService friendService;
    private final UserStateService userStateService;

    public EditMessageText handle(CallbackQuery callbackQuery) {
        if (callbackQuery == null) return null;

        String data = callbackQuery.getData();
        MaybeInaccessibleMessage message = callbackQuery.getMessage();
        if (data == null || message == null) return null;

        long chatId = message.getChatId();
        long userId = callbackQuery.getFrom().getId();
        Lang lang = userStateService.getLanguage(userId);

        String text;
        if (data.startsWith(LANG_PREFIX)) {
            text = handleLanguage(userId, data.substring(LANG_PREFIX.length()));
        } else if (data.startsWith(REMOVE_PREFIX)) {
            text = handleRemove(userId, data.substring(REMOVE_PREFIX.length()), lang);
        } else if (data.startsWith(MONTH_PREFIX)) {
            text = handleMonth(userId, data, lang);
        } else {
            log.debug("callback.unknown: data={}", data);
            return null;
        }

        return EditMessageText.builder()
                .chatId(chatId)
                .messageId(message.getMessageId())
                .parseMode("HTML")
                .text(text)
                .build();
    }

    private String handleLanguage(long userId, String code) {
        try {
            Lang lang = Lang.valueOf(code);
            userStateService.setLanguage(userId, lang);
            log.debug("callback.language.changed: userId={}, lang={}", userId, lang);
            return Messages.get(lang, Messages.LANGUAGE_SET);
        } catch (IllegalArgumentException e) {
            log.warn("callback.language.unknown: code={}, reason={}", code, e.getMessage());
            return Messages.get(userStateService.getLanguage(userId), Messages.UNKNOWN_COMMAND);
        }
    }

    private String handleRemove(long userId, String name, Lang lang) {
        if (!friendService.friendExists(userId, name)) {
            return Messages.get(lang, Messages.FRIEND_NOT_FOUND, name);
        }
        friendService.deleteFriend(userId, name);
        userStateService.clearState(userId);
        log.debug("callback.friend.removed: userId={}, name={}", userId, name);
        return Messages.get(lang, Messages.FRIEND_REMOVED, name);
    }

    private String handleMonth(long userId, String data, Lang lang) {
        String value = data.substring(MONTH_PREFIX.length());
        int month;
        if (CURRENT_MONTH.equalsIgnoreCase(value)) {
            month = LocalDate.now().getMonthValue();
        } else {
            try {
                month = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                log.warn("callback.month.parse.failed: data={}, reason={}", data, e.getMessage());
                return Messages.get(lang, Messages.MONTH_PARSE_ERROR);
            }
        }

        List<Friend> filtered = friendService.getFriendsSortedByDayMonth(userId).stream()
                .filter(f -> f.getBirthDate().getMonthValue() == month)
                .toList();

        String raw = Month.of(month).getDisplayName(TextStyle.FULL_STANDALONE, lang.locale());
        String monthName = Character.toUpperCase(raw.charAt(0)) + raw.substring(1);

        if (filtered.isEmpty()) {
            return Messages.get(lang, Messages.BIRTHDAYS_NONE, monthName);
        }

        StringBuilder sb = new StringBuilder(Messages.get(lang, Messages.BIRTHDAYS_HEADER, monthName));
        filtered.forEach(f -> sb.append("– <b>").append(f.getBirthDate().format(MessageBuilder.DATE_FORMATTER))
                .append("</b> ").append(f.getName())
                .append(" (<i>").append(Messages.get(lang, Messages.YEARS_OLD, f.getAge())).append("</i>)\n"));
        return sb.toString();
    }
}
