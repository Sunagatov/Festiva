package com.festiva.bot;

import com.festiva.command.DatePickerKeyboard;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.friend.entity.Relationship;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.state.BotState;
import com.festiva.state.UserStateService;
import com.festiva.util.UserDateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings("unused")
class DatePickerCallbackHandler {

    static final String RELATIONSHIP_PREFIX = "RELATIONSHIP_";
    static final String EDIT_REL_PREFIX     = "EDIT_REL_";
    private static final String LIST_SORT_DATE = "LIST_SORT_DATE";

    private final FriendService friendService;
    private final UserStateService userStateService;
    private final UserDateService userDateService;

    CallbackResult handleYearPage(String data, long userId, Lang lang) {
        Integer offset = parseInteger(data.substring(DatePickerKeyboard.DATE_YEAR_PAGE_PREFIX.length()),
                "callback.date.year.page.parse.failed", data);
        if (offset == null || offset < 0) {
            return sessionExpired(lang);
        }

        String name = userStateService.getPendingName(userId);
        if (name == null) {
            return sessionExpired(lang);
        }

        userStateService.setYearPageOffset(userId, offset);
        return new CallbackResult(Messages.get(lang, Messages.DATE_PICK_YEAR, name),
                DatePickerKeyboard.yearKeyboard(offset, lang));
    }

    CallbackResult handleYearPick(String data, long userId, Lang lang) {
        String name = userStateService.getPendingName(userId);
        if (name == null) {
            return sessionExpired(lang);
        }

        Integer year = parseInteger(data.substring(DatePickerKeyboard.DATE_YEAR_PREFIX.length()),
                "callback.date.year.parse.failed", data);
        if (year == null || year <= 0) {
            return sessionExpired(lang);
        }

        userStateService.setPendingYear(userId, year);
        return new CallbackResult(Messages.get(lang, Messages.DATE_PICK_MONTH, name),
                DatePickerKeyboard.monthKeyboard(lang, userStateService.getYearPageOffset(userId)));
    }

    CallbackResult handleSkipYear(long userId, Lang lang) {
        String name = userStateService.getPendingName(userId);
        if (name == null) {
            return sessionExpired(lang);
        }

        userStateService.setPendingYear(userId, null);
        return new CallbackResult(Messages.get(lang, Messages.DATE_PICK_MONTH, name),
                DatePickerKeyboard.monthKeyboard(lang, userStateService.getYearPageOffset(userId)));
    }

    CallbackResult handleMonthPick(String data, long userId, Lang lang) {
        Integer month = parseInteger(data.substring(DatePickerKeyboard.DATE_MONTH_PREFIX.length()),
                "callback.date.month.parse.failed", data);
        if (month == null || month < 1 || month > 12) {
            return sessionExpired(lang);
        }

        Integer year = userStateService.getPendingYear(userId);
        String name = userStateService.getPendingName(userId);
        if (name == null) {
            return sessionExpired(lang);
        }

        userStateService.setPendingMonth(userId, month);

        int yearForDayPicker = year != null ? year : 2000;
        return new CallbackResult(Messages.get(lang, Messages.DATE_PICK_DAY, name),
                DatePickerKeyboard.dayKeyboard(yearForDayPicker, month, lang));
    }

    CallbackResult handleDayPick(String data, long userId, Lang lang) {
        Integer day = parseInteger(data.substring(DatePickerKeyboard.DATE_DAY_PREFIX.length()),
                "callback.date.day.parse.failed", data);
        Integer year = userStateService.getPendingYear(userId);
        Integer month = userStateService.getPendingMonth(userId);
        String name = userStateService.getPendingName(userId);
        String id = userStateService.getPendingId(userId);

        if (day == null || month == null || name == null) {
            return sessionExpired(lang);
        }

        if (year != null) {
            try {
                LocalDate birthDate = LocalDate.of(year, month, day);
                if (birthDate.isAfter(userDateService.todayFor(userId))) {
                    return new CallbackResult(Messages.get(lang, Messages.DATE_FUTURE_ERROR),
                            DatePickerKeyboard.dayKeyboard(year, month, lang));
                }
            } catch (java.time.DateTimeException e) {
                log.atDebug()
                        .setMessage("callback_date_invalid")
                        .addKeyValue("userId", userId)
                        .addKeyValue("year", year)
                        .addKeyValue("month", month)
                        .addKeyValue("day", day)
                        .log();
                return new CallbackResult(Messages.get(lang, Messages.DATE_FUTURE_ERROR),
                        DatePickerKeyboard.dayKeyboard(year, month, lang));
            }
        } else {
            try {
                java.time.MonthDay.of(month, day);
            } catch (java.time.DateTimeException e) {
                log.atDebug()
                        .setMessage("callback_month_day_invalid")
                        .addKeyValue("userId", userId)
                        .addKeyValue("month", month)
                        .addKeyValue("day", day)
                        .log();
                return new CallbackResult(Messages.get(lang, Messages.DATE_FUTURE_ERROR),
                        DatePickerKeyboard.dayKeyboard(2000, month, lang));
            }
        }

        if (userStateService.getState(userId) == BotState.WAITING_FOR_EDIT_DATE) {
            if (id == null) {
                return sessionExpired(lang);
            }

            try {
                friendService.updateFriendDateById(id, userId, year, month, day);
            } catch (IllegalArgumentException e) {
                log.atDebug()
                        .setMessage("callback_date_edit_rejected")
                        .addKeyValue("userId", userId)
                        .addKeyValue("friendId", id)
                        .addKeyValue("hasYear", year != null)
                        .log();
                return new CallbackResult(Messages.get(lang, Messages.DATE_FUTURE_ERROR),
                        DatePickerKeyboard.dayKeyboard(year != null ? year : 2000, month, lang));
            }

            userStateService.clearState(userId);
            return new CallbackResult(Messages.get(lang, Messages.EDIT_DATE_DONE, name), null);
        }

        userStateService.setPendingYear(userId, year);
        userStateService.setPendingMonth(userId, month);
        userStateService.setPendingDay(userId, day);
        userStateService.setState(userId, BotState.WAITING_FOR_ADD_FRIEND_RELATIONSHIP);
        return new CallbackResult(Messages.get(lang, Messages.RELATIONSHIP_PICK, name), relationshipKeyboard(lang));
    }

    CallbackResult handleBackToYear(String data, long userId, Lang lang) {
        String name = userStateService.getPendingName(userId);
        if (name == null) {
            return sessionExpired(lang);
        }

        Integer offset = parseInteger(data.substring(DatePickerKeyboard.DATE_BACK_TO_YEAR.length() + 1),
                "callback.date.back.year.parse.failed", data);
        if (offset == null || offset < 0) {
            return sessionExpired(lang);
        }

        userStateService.setYearPageOffset(userId, offset);
        userStateService.setPendingYear(userId, null);
        return new CallbackResult(Messages.get(lang, Messages.DATE_PICK_YEAR, name),
                DatePickerKeyboard.yearKeyboard(offset, lang));
    }

    CallbackResult handleBackToMonth(long userId, Lang lang) {
        String name = userStateService.getPendingName(userId);
        if (name == null) {
            return sessionExpired(lang);
        }

        userStateService.setPendingMonth(userId, null);
        return new CallbackResult(Messages.get(lang, Messages.DATE_PICK_MONTH, name),
                DatePickerKeyboard.monthKeyboard(lang, userStateService.getYearPageOffset(userId)));
    }

    CallbackResult handleRelationship(String data, long userId, Lang lang) {
        String name = userStateService.getPendingName(userId);
        Integer year = userStateService.getPendingYear(userId);
        Integer month = userStateService.getPendingMonth(userId);
        Integer day = userStateService.getPendingDay(userId);
        if (month == null || day == null || name == null) {
            return sessionExpired(lang);
        }

        String value = data.substring(RELATIONSHIP_PREFIX.length());
        Relationship rel;
        if ("SKIP".equals(value)) {
            rel = null;
        } else {
            try {
                rel = Relationship.valueOf(value);
            } catch (IllegalArgumentException e) {
                log.atDebug()
                        .setMessage("callback_relationship_invalid")
                        .addKeyValue("userId", userId)
                        .addKeyValue("value", value)
                        .log();
                return sessionExpired(lang);
            }
        }

        if (friendService.getFriends(userId).size() >= FriendService.FRIEND_CAP) {
            userStateService.clearState(userId);
            return new CallbackResult(Messages.get(lang, Messages.FRIEND_CAP, FriendService.FRIEND_CAP), null);
        }

        try {
            friendService.addFriend(userId, new Friend(name, year, month, day, rel));
        } catch (IllegalArgumentException e) {
            userStateService.clearState(userId);
            String text = friendService.friendExists(userId, name)
                    ? Messages.get(lang, Messages.NAME_EXISTS, name)
                    : Messages.get(lang, Messages.SESSION_EXPIRED);
            return new CallbackResult(text, null);
        }

        userStateService.clearState(userId);

        String messageKey = year != null ? Messages.FRIEND_ADDED : Messages.FRIEND_ADDED_NO_YEAR;
        return new CallbackResult(Messages.get(lang, messageKey, name),
                InlineKeyboardMarkup.builder().keyboard(List.of(new InlineKeyboardRow(
                        InlineKeyboardButton.builder().text(Messages.get(lang, Messages.QUICK_LIST)).callbackData(LIST_SORT_DATE + "_0").build(),
                        InlineKeyboardButton.builder().text(Messages.get(lang, Messages.QUICK_ADD_ANOTHER)).callbackData(CallbackQueryHandler.ACTION_ADD).build()
                ))).build());
    }

    CallbackResult handleEditFieldRel(String data, long userId, Lang lang) {
        String id = data.substring(EditCallbackHandler.EDIT_FIELD_REL.length());
        Friend friend = friendService.findOwnedFriend(id, userId).orElse(null);
        if (friend == null) {
            return sessionExpired(lang);
        }

        userStateService.setPendingName(userId, friend.getName());
        userStateService.setPendingId(userId, id);
        userStateService.setState(userId, BotState.WAITING_FOR_EDIT_RELATIONSHIP);
        return new CallbackResult(Messages.get(lang, Messages.RELATIONSHIP_PICK, friend.getName()), editRelKeyboard(lang));
    }

    CallbackResult handleEditRelationship(String data, long userId, Lang lang) {
        String id = userStateService.getPendingId(userId);
        String name = userStateService.getPendingName(userId);
        if (id == null || name == null) {
            return sessionExpired(lang);
        }

        String value = data.substring(EDIT_REL_PREFIX.length());
        Relationship rel;
        if ("SKIP".equals(value)) {
            rel = null;
        } else {
            try {
                rel = Relationship.valueOf(value);
            } catch (IllegalArgumentException e) {
                log.atDebug()
                        .setMessage("callback_edit_relationship_invalid")
                        .addKeyValue("userId", userId)
                        .addKeyValue("value", value)
                        .log();
                return sessionExpired(lang);
            }
        }

        friendService.updateFriendRelationshipById(id, userId, rel);
        userStateService.clearState(userId);
        return new CallbackResult(Messages.get(lang, Messages.EDIT_REL_DONE, name), null);
    }

    private InlineKeyboardMarkup relationshipKeyboard(Lang lang) {
        return relKeyboard(lang, RELATIONSHIP_PREFIX);
    }

    private InlineKeyboardMarkup editRelKeyboard(Lang lang) {
        return relKeyboard(lang, EDIT_REL_PREFIX);
    }

    private InlineKeyboardMarkup relKeyboard(Lang lang, String prefix) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        InlineKeyboardRow row = new InlineKeyboardRow();
        for (Relationship r : Relationship.values()) {
            row.add(InlineKeyboardButton.builder().text(r.label(lang)).callbackData(prefix + r.name()).build());
            if (row.size() == 3) {
                rows.add(row);
                row = new InlineKeyboardRow();
            }
        }
        if (!row.isEmpty()) {
            rows.add(row);
        }
        rows.add(new InlineKeyboardRow(
                InlineKeyboardButton.builder().text(Messages.get(lang, Messages.RELATIONSHIP_SKIP)).callbackData(prefix + "SKIP").build()));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private Integer parseInteger(String raw, String logKey, String data) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            log.atDebug()
                    .setMessage(logKey)
                    .addKeyValue("data", data)
                    .log();
            return null;
        }
    }

    private CallbackResult sessionExpired(Lang lang) {
        return new CallbackResult(Messages.get(lang, Messages.SESSION_EXPIRED), null);
    }
}
