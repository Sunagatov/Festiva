package com.festiva.friend.api;

import com.festiva.bot.CallbackResult;
import com.festiva.command.MessageBuilder;
import com.festiva.friend.entity.Friend;
import com.festiva.friend.handler.DatePickerCallbackHandler;
import com.festiva.friend.handler.DatePickerKeyboard;
import com.festiva.friend.handler.EditCallbackHandler;
import com.festiva.friend.handler.EditFriendCommandHandler;
import com.festiva.friend.handler.ListCommandHandler;
import com.festiva.friend.handler.RemoveCommandHandler;
import com.festiva.friend.workflow.FriendWorkflowSessionService;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.state.BotState;
import com.festiva.state.UserStateService;
import com.festiva.user.api.UserDateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class FriendCallbackService {

    private static final String MONTH_PREFIX = "MONTH_";
    private static final String REMOVE_PREFIX = "REMOVE_";
    private static final String CONFIRM_PREFIX = "CONFIRM_REMOVE_";
    private static final String CANCEL_REMOVE = "CANCEL_REMOVE";
    private static final String CURRENT_MONTH = "CURRENT";
    private static final String LIST_SORT_DATE = "LIST_SORT_DATE";
    private static final String LIST_SORT_NAME = "LIST_SORT_NAME";

    private final FriendService friendService;
    private final UserStateService userStateService;
    private final DatePickerCallbackHandler datePickerHandler;
    private final EditCallbackHandler editHandler;
    private final RemoveCommandHandler removeCommandHandler;
    private final EditFriendCommandHandler editFriendCommandHandler;
    private final FriendWorkflowSessionService friendWorkflowSessionService;
    private final UserDateService userDateService;
    private final ListCommandHandler listHandler;

    public CallbackResult handle(String data, long userId, Lang lang) {
        CallbackResult result;
        if ((result = dispatchDatePicker(data, userId, lang)) != null) {
            return result;
        }
        if ((result = dispatchEdit(data, userId, lang)) != null) {
            return result;
        }
        if ((result = dispatchRemove(data, userId, lang)) != null) {
            return result;
        }
        if ((result = dispatchList(data, userId, lang)) != null) {
            return result;
        }
        if (data.startsWith(MONTH_PREFIX)) {
            return handleMonth(userId, data, lang);
        }
        return null;
    }

    private CallbackResult dispatchDatePicker(String data, long userId, Lang lang) {
        if (data.startsWith(DatePickerKeyboard.DATE_YEAR_PAGE_PREFIX)) {
            return datePickerHandler.handleYearPage(data, userId, lang);
        }
        if (data.startsWith(DatePickerKeyboard.DATE_YEAR_PREFIX)) {
            return datePickerHandler.handleYearPick(data, userId, lang);
        }
        if (DatePickerKeyboard.DATE_SKIP_YEAR.equals(data)) {
            return datePickerHandler.handleSkipYear(userId, lang);
        }
        if (data.startsWith(DatePickerKeyboard.DATE_MONTH_PREFIX)) {
            return datePickerHandler.handleMonthPick(data, userId, lang);
        }
        if (data.startsWith(DatePickerKeyboard.DATE_DAY_PREFIX)) {
            return datePickerHandler.handleDayPick(data, userId, lang);
        }
        if (data.startsWith(DatePickerKeyboard.DATE_BACK_TO_YEAR)) {
            return datePickerHandler.handleBackToYear(data, userId, lang);
        }
        if (DatePickerKeyboard.DATE_BACK_TO_MONTH.equals(data)) {
            return datePickerHandler.handleBackToMonth(userId, lang);
        }
        if (data.startsWith(DatePickerCallbackHandler.RELATIONSHIP_PREFIX)) {
            return datePickerHandler.handleRelationship(data, userId, lang);
        }
        if (data.startsWith(DatePickerCallbackHandler.EDIT_REL_PREFIX)) {
            return datePickerHandler.handleEditRelationship(data, userId, lang);
        }
        return null;
    }

    private CallbackResult dispatchEdit(String data, long userId, Lang lang) {
        if (data.startsWith(EditFriendCommandHandler.EDIT_PAGE_PREFIX)) {
            return handleEditPage(data, userId, lang);
        }
        if (data.startsWith(EditCallbackHandler.EDIT_FIELD_NOTIFY)) {
            return editHandler.handleEditNotify(data, userId, lang);
        }
        if (data.startsWith(EditCallbackHandler.EDIT_FIELD_NAME)) {
            return editHandler.handleEditFieldName(data, userId, lang);
        }
        if (data.startsWith(EditCallbackHandler.EDIT_FIELD_DATE)) {
            return editHandler.handleEditFieldDate(data, userId, lang);
        }
        if (data.startsWith(EditCallbackHandler.EDIT_FIELD_REL)) {
            return datePickerHandler.handleEditFieldRel(data, userId, lang);
        }
        if (data.startsWith(EditCallbackHandler.EDIT_PREFIX)) {
            return editHandler.handleEditSelect(data, userId, lang);
        }
        return null;
    }

    private CallbackResult dispatchRemove(String data, long userId, Lang lang) {
        if (data.startsWith(RemoveCommandHandler.REMOVE_PAGE_PREFIX)) {
            return handleRemovePage(data, userId, lang);
        }
        if (data.startsWith(CONFIRM_PREFIX)) {
            return handleConfirmRemove(userId, data.substring(CONFIRM_PREFIX.length()), lang);
        }
        if (data.startsWith(REMOVE_PREFIX)) {
            return handleRemove(data, userId, lang);
        }
        if (CANCEL_REMOVE.equals(data)) {
            return handleCancelRemove(userId, lang);
        }
        return null;
    }

    private CallbackResult dispatchList(String data, long userId, Lang lang) {
        if (data.startsWith(ListCommandHandler.LIST_PAGE_PREFIX)) {
            return handleListPage(data, userId, lang);
        }
        if (data.startsWith(LIST_SORT_DATE) || data.startsWith(LIST_SORT_NAME)) {
            return handleListSort(data, userId, lang);
        }
        return null;
    }

    private CallbackResult handleListSort(String data, long userId, Lang lang) {
        boolean byDate = data.startsWith(LIST_SORT_DATE);
        Integer page = parsePageSuffix(data);
        if (page == null) {
            return sessionExpired(lang);
        }

        var friends = friendService.getFriendsSortedByDayMonth(userId);
        return new CallbackResult(listHandler.buildText(friends, lang, byDate, page, userId),
                listHandler.keyboard(lang, byDate, page, friends.size()));
    }

    private CallbackResult handleListPage(String data, long userId, Lang lang) {
        String suffix = data.substring(ListCommandHandler.LIST_PAGE_PREFIX.length());
        boolean byDate;
        if (suffix.startsWith("DATE_")) {
            byDate = true;
        } else if (suffix.startsWith("NAME_")) {
            byDate = false;
        } else {
            log.atDebug()
                    .setMessage("callback_list_page_mode_invalid")
                    .addKeyValue("data", data)
                    .log();
            return sessionExpired(lang);
        }

        Integer page = parsePageSuffix(data);
        if (page == null) {
            return sessionExpired(lang);
        }

        var friends = friendService.getFriendsSortedByDayMonth(userId);
        return new CallbackResult(listHandler.buildText(friends, lang, byDate, page, userId),
                listHandler.keyboard(lang, byDate, page, friends.size()));
    }

    private Integer parsePageSuffix(String data) {
        int idx = data.lastIndexOf('_');
        if (idx < 0) {
            log.atDebug()
                    .setMessage("callback_page_parse_failed")
                    .addKeyValue("reason", "missing_suffix")
                    .addKeyValue("data", data)
                    .log();
            return null;
        }

        try {
            int page = Integer.parseInt(data.substring(idx + 1));
            if (page < 0) {
                log.atDebug()
                        .setMessage("callback_page_parse_failed")
                        .addKeyValue("reason", "negative_page")
                        .addKeyValue("data", data)
                        .log();
                return null;
            }
            return page;
        } catch (NumberFormatException e) {
            log.atDebug()
                    .setMessage("callback_page_parse_failed")
                    .addKeyValue("reason", "number_format")
                    .addKeyValue("data", data)
                    .log();
            return null;
        }
    }

    private CallbackResult handleRemovePage(String data, long userId, Lang lang) {
        String rawPage = data.substring(RemoveCommandHandler.REMOVE_PAGE_PREFIX.length());
        try {
            int page = Integer.parseInt(rawPage);
            if (page < 0) {
                return sessionExpired(lang);
            }

            var friends = friendService.getFriendsSortedByDayMonth(userId);
            if (friends.isEmpty()) {
                return new CallbackResult(Messages.get(lang, Messages.FRIENDS_EMPTY), null);
            }
            return new CallbackResult(Messages.get(lang, Messages.SELECT_REMOVE),
                    removeCommandHandler.keyboard(friends, page));
        } catch (NumberFormatException e) {
            log.atDebug()
                    .setMessage("callback_remove_page_parse_failed")
                    .addKeyValue("data", data)
                    .log();
            return sessionExpired(lang);
        }
    }

    private CallbackResult handleEditPage(String data, long userId, Lang lang) {
        String rawPage = data.substring(EditFriendCommandHandler.EDIT_PAGE_PREFIX.length());
        try {
            int page = Integer.parseInt(rawPage);
            if (page < 0) {
                return sessionExpired(lang);
            }

            var friends = friendService.getFriendsSortedByDayMonth(userId);
            if (friends.isEmpty()) {
                return new CallbackResult(Messages.get(lang, Messages.FRIENDS_EMPTY), null);
            }
            return new CallbackResult(Messages.get(lang, Messages.EDIT_SELECT),
                    editFriendCommandHandler.keyboard(friends, page));
        } catch (NumberFormatException e) {
            log.atDebug()
                    .setMessage("callback_edit_page_parse_failed")
                    .addKeyValue("data", data)
                    .log();
            return sessionExpired(lang);
        }
    }

    private CallbackResult handleRemove(String data, long userId, Lang lang) {
        String id = data.substring(REMOVE_PREFIX.length());
        Friend friend = friendService.findOwnedFriend(id, userId).orElse(null);
        if (friend == null) {
            return sessionExpired(lang);
        }

        String name = friend.getName();
        friendWorkflowSessionService.setPendingName(userId, name);
        friendWorkflowSessionService.setPendingId(userId, id);
        userStateService.setState(userId, BotState.WAITING_FOR_REMOVE_CONFIRM);
        return new CallbackResult(Messages.get(lang, Messages.CONFIRM_REMOVE_ASK, name), confirmKeyboard(id, lang));
    }

    private CallbackResult handleConfirmRemove(long userId, String id, Lang lang) {
        Friend friend = friendService.findOwnedFriend(id, userId).orElse(null);
        if (friend == null) {
            return sessionExpired(lang);
        }

        String name = friend.getName();
        friendService.deleteFriendById(id, userId);
        userStateService.clearState(userId);
        return new CallbackResult(Messages.get(lang, Messages.FRIEND_REMOVED, name), null);
    }

    private CallbackResult handleCancelRemove(long userId, Lang lang) {
        userStateService.clearState(userId);
        return new CallbackResult(Messages.get(lang, Messages.CONFIRM_REMOVE_CANCEL), null);
    }

    private CallbackResult handleMonth(long userId, String data, Lang lang) {
        String value = data.substring(MONTH_PREFIX.length());
        int month;
        if (CURRENT_MONTH.equalsIgnoreCase(value)) {
            month = userDateService.todayFor(userId).getMonthValue();
        } else {
            try {
                month = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                log.atDebug()
                        .setMessage("callback_month_parse_failed")
                        .addKeyValue("data", data)
                        .log();
                return new CallbackResult(Messages.get(lang, Messages.MONTH_PARSE_ERROR), null);
            }
        }

        if (month < 1 || month > 12) {
            log.atDebug()
                    .setMessage("callback_month_invalid")
                    .addKeyValue("data", data)
                    .addKeyValue("month", month)
                    .log();
            return new CallbackResult(Messages.get(lang, Messages.MONTH_PARSE_ERROR), null);
        }

        var filtered = friendService.getFriendsSortedByDayMonth(userId).stream()
                .filter(f -> f.getBirthMonthDay().getMonthValue() == month)
                .toList();

        String raw = Month.of(month).getDisplayName(TextStyle.FULL_STANDALONE, lang.locale());
        String monthName = Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
        if (filtered.isEmpty()) {
            return new CallbackResult(Messages.get(lang, Messages.BIRTHDAYS_NONE, monthName), null);
        }

        LocalDate today = userDateService.todayFor(userId);
        StringBuilder sb = new StringBuilder(Messages.get(lang, Messages.BIRTHDAYS_HEADER, monthName) + "\n\n");
        filtered.forEach(f -> {
            String dateStr = f.hasYear()
                    ? f.getBirthDate().format(MessageBuilder.DATE_FORMATTER)
                    : String.format("%02d.%02d", f.getBirthMonthDay().getDayOfMonth(), f.getBirthMonthDay().getMonthValue());

            sb.append("– <b>").append(dateStr)
                    .append("</b> ").append(com.festiva.util.HtmlEscaper.escape(f.getName()));

            if (f.hasYear()) {
                LocalDate next = f.nextBirthday(today);
                boolean alreadyCelebrated = next.equals(today) || next.getYear() > today.getYear();
                String ageLabel = alreadyCelebrated
                        ? Messages.get(lang, Messages.YEARS_OLD, Messages.yearsRu(lang, f.getAge(today)))
                        : Messages.get(lang, Messages.YEARS_TURNS, Messages.yearsRu(lang, f.getNextAge(today)));
                sb.append(" (<i>").append(ageLabel).append("</i>)");
            }

            sb.append("\n");
        });
        return new CallbackResult(sb.toString(), null);
    }

    private InlineKeyboardMarkup confirmKeyboard(String id, Lang lang) {
        return InlineKeyboardMarkup.builder().keyboard(List.of(new InlineKeyboardRow(
                InlineKeyboardButton.builder().text(Messages.get(lang, Messages.CONFIRM_YES)).callbackData(CONFIRM_PREFIX + id).build(),
                InlineKeyboardButton.builder().text(Messages.get(lang, Messages.CONFIRM_NO)).callbackData(CANCEL_REMOVE).build()
        ))).build();
    }

    private CallbackResult sessionExpired(Lang lang) {
        return new CallbackResult(Messages.get(lang, Messages.SESSION_EXPIRED), null);
    }
}
