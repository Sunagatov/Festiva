package com.festiva.friend.handler;

import com.festiva.bot.CallbackResult;
import com.festiva.command.MessageBuilder;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.friend.entity.Relationship;
import com.festiva.friend.workflow.FriendWorkflowSessionService;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.state.BotState;
import com.festiva.state.UserStateService;
import com.festiva.user.api.UserDateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class DatePickerCallbackHandler {

    public static final String RELATIONSHIP_PREFIX = "RELATIONSHIP_";
    public static final String EDIT_REL_PREFIX = "EDIT_REL_";
    private static final String LIST_SORT_DATE = "LIST_SORT_DATE";

    private final FriendService friendService;
    private final UserStateService userStateService;
    private final FriendWorkflowSessionService friendWorkflowSessionService;
    private final UserDateService userDateService;

    public CallbackResult handleYearPage(String data, long userId, Lang lang) {
        Integer offset = parseInteger(data.substring(DatePickerKeyboard.DATE_YEAR_PAGE_PREFIX.length()),
                "callback.date.year.page.parse.failed", data);
        if (offset == null || offset < 0) {
            return sessionExpired(lang);
        }

        String name = friendWorkflowSessionService.getPendingName(userId);
        if (name == null) {
            return sessionExpired(lang);
        }

        friendWorkflowSessionService.setYearPageOffset(userId, offset);
        return new CallbackResult(Messages.get(lang, Messages.DATE_PICK_YEAR, name),
                DatePickerKeyboard.yearKeyboard(offset, lang));
    }

    public CallbackResult handleYearPick(String data, long userId, Lang lang) {
        String name = friendWorkflowSessionService.getPendingName(userId);
        if (name == null) {
            return sessionExpired(lang);
        }

        Integer year = parseInteger(data.substring(DatePickerKeyboard.DATE_YEAR_PREFIX.length()),
                "callback.date.year.parse.failed", data);
        if (year == null || year <= 0) {
            return sessionExpired(lang);
        }

        friendWorkflowSessionService.setPendingYear(userId, year);
        return new CallbackResult(datePrompt(lang, Messages.DATE_PICK_MONTH, name, year, null),
                DatePickerKeyboard.monthKeyboard(lang, friendWorkflowSessionService.getYearPageOffset(userId)));
    }

    public CallbackResult handleSkipYear(long userId, Lang lang) {
        String name = friendWorkflowSessionService.getPendingName(userId);
        if (name == null) {
            return sessionExpired(lang);
        }

        friendWorkflowSessionService.setPendingYear(userId, null);
        return new CallbackResult(datePrompt(lang, Messages.DATE_PICK_MONTH, name, null, null),
                DatePickerKeyboard.monthKeyboard(lang, friendWorkflowSessionService.getYearPageOffset(userId)));
    }

    public CallbackResult handleMonthPick(String data, long userId, Lang lang) {
        Integer month = parseInteger(data.substring(DatePickerKeyboard.DATE_MONTH_PREFIX.length()),
                "callback.date.month.parse.failed", data);
        if (month == null || month < 1 || month > 12) {
            return sessionExpired(lang);
        }

        Integer year = friendWorkflowSessionService.getPendingYear(userId);
        String name = friendWorkflowSessionService.getPendingName(userId);
        if (name == null) {
            return sessionExpired(lang);
        }

        friendWorkflowSessionService.setPendingMonth(userId, month);

        int yearForDayPicker = year != null ? year : 2000;
        return new CallbackResult(datePrompt(lang, Messages.DATE_PICK_DAY, name, year, month),
                DatePickerKeyboard.dayKeyboard(yearForDayPicker, month, lang));
    }

    public CallbackResult handleDayPick(String data, long userId, Lang lang) {
        Integer day = parseInteger(data.substring(DatePickerKeyboard.DATE_DAY_PREFIX.length()),
                "callback.date.day.parse.failed", data);
        Integer year = friendWorkflowSessionService.getPendingYear(userId);
        Integer month = friendWorkflowSessionService.getPendingMonth(userId);
        String name = friendWorkflowSessionService.getPendingName(userId);
        String id = friendWorkflowSessionService.getPendingId(userId);

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
            return new CallbackResult(Messages.get(lang, Messages.EDIT_DATE_DONE, name), MessageBuilder.editAndListMarkup(lang));
        }

        friendWorkflowSessionService.setPendingYear(userId, year);
        friendWorkflowSessionService.setPendingMonth(userId, month);
        friendWorkflowSessionService.setPendingDay(userId, day);
        userStateService.setState(userId, BotState.WAITING_FOR_ADD_FRIEND_RELATIONSHIP);
        return new CallbackResult(Messages.get(lang, Messages.RELATIONSHIP_PICK, name), relationshipKeyboard(lang));
    }

    public CallbackResult handleBackToYear(String data, long userId, Lang lang) {
        String name = friendWorkflowSessionService.getPendingName(userId);
        if (name == null) {
            return sessionExpired(lang);
        }

        Integer offset = parseInteger(data.substring(DatePickerKeyboard.DATE_BACK_TO_YEAR.length() + 1),
                "callback.date.back.year.parse.failed", data);
        if (offset == null || offset < 0) {
            return sessionExpired(lang);
        }

        friendWorkflowSessionService.setYearPageOffset(userId, offset);
        friendWorkflowSessionService.setPendingYear(userId, null);
        return new CallbackResult(Messages.get(lang, Messages.DATE_PICK_YEAR, name),
                DatePickerKeyboard.yearKeyboard(offset, lang));
    }

    public CallbackResult handleBackToMonth(long userId, Lang lang) {
        String name = friendWorkflowSessionService.getPendingName(userId);
        if (name == null) {
            return sessionExpired(lang);
        }

        friendWorkflowSessionService.setPendingMonth(userId, null);
        return new CallbackResult(datePrompt(lang, Messages.DATE_PICK_MONTH, name, friendWorkflowSessionService.getPendingYear(userId), null),
                DatePickerKeyboard.monthKeyboard(lang, friendWorkflowSessionService.getYearPageOffset(userId)));
    }

    private String datePrompt(Lang lang, String key, String name, Integer year, Integer month) {
        String base = Messages.get(lang, key, name);
        String breadcrumb = dateBreadcrumb(lang, year, month);
        if (breadcrumb == null) {
            return base;
        }
        return base + "\n📅 " + breadcrumb + " → __";
    }

    private String dateBreadcrumb(Lang lang, Integer year, Integer month) {
        if (month != null) {
            String monthLabel = Month.of(month).getDisplayName(TextStyle.FULL, lang.locale());
            monthLabel = monthLabel.substring(0, 1).toUpperCase(lang.locale()) + monthLabel.substring(1);
            return year != null ? monthLabel + " " + year : monthLabel;
        }
        if (year != null) {
            return String.valueOf(year);
        }
        return null;
    }

    public CallbackResult handleRelationship(String data, long userId, Lang lang) {
        String name = friendWorkflowSessionService.getPendingName(userId);
        Integer year = friendWorkflowSessionService.getPendingYear(userId);
        Integer month = friendWorkflowSessionService.getPendingMonth(userId);
        Integer day = friendWorkflowSessionService.getPendingDay(userId);
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
        return new CallbackResult(buildAddedPreview(lang, name, year, month, day, rel), MessageBuilder.addAndListMarkup(lang));
    }

    public CallbackResult handleEditFieldRel(String data, long userId, Lang lang) {
        String id = data.substring(EditCallbackHandler.EDIT_FIELD_REL.length());
        Friend friend = friendService.findOwnedFriend(id, userId).orElse(null);
        if (friend == null) {
            return sessionExpired(lang);
        }

        friendWorkflowSessionService.setPendingName(userId, friend.getName());
        friendWorkflowSessionService.setPendingId(userId, id);
        userStateService.setState(userId, BotState.WAITING_FOR_EDIT_RELATIONSHIP);
        return new CallbackResult(Messages.get(lang, Messages.RELATIONSHIP_PICK, friend.getName()), editRelKeyboard(lang));
    }

    public CallbackResult handleEditRelationship(String data, long userId, Lang lang) {
        String id = friendWorkflowSessionService.getPendingId(userId);
        String name = friendWorkflowSessionService.getPendingName(userId);
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
        return new CallbackResult(Messages.get(lang, Messages.EDIT_REL_DONE, name), MessageBuilder.editAndListMarkup(lang));
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

    private String buildAddedPreview(Lang lang, String name, Integer year, int month, int day, Relationship relationship) {
        StringJoiner text = new StringJoiner("\n");
        text.add(Messages.get(lang, Messages.FRIEND_ADDED_CARD));
        text.add("👤 " + name);
        text.add("📅 " + formatBirthDate(lang, year, month, day));
        if (relationship != null) {
            text.add("💞 " + relationshipTitle(lang, relationship));
        }
        return text.toString();
    }

    private String formatBirthDate(Lang lang, Integer year, int month, int day) {
        String monthLabel = Month.of(month).getDisplayName(TextStyle.FULL, lang.locale());
        if (lang == Lang.EN) {
            monthLabel = monthLabel.substring(0, 1).toUpperCase(lang.locale()) + monthLabel.substring(1);
        }
        if (lang == Lang.EN) {
            return year != null ? monthLabel + " " + day + ", " + year : monthLabel + " " + day;
        }
        return year != null ? day + " " + monthLabel + " " + year : day + " " + monthLabel;
    }

    private String relationshipTitle(Lang lang, Relationship relationship) {
        String label = relationship.label(lang);
        int firstSpace = label.indexOf(' ');
        if (firstSpace < 0 || firstSpace == label.length() - 1) {
            return label;
        }
        return label.substring(firstSpace + 1);
    }
}
