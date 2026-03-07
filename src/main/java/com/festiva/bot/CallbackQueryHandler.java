package com.festiva.bot;

import com.festiva.command.DatePickerKeyboard;
import com.festiva.command.MessageBuilder;
import com.festiva.command.handler.BulkAddCommandHandler;
import com.festiva.command.handler.DeleteAccountCommandHandler;
import com.festiva.command.handler.EditFriendCommandHandler;
import com.festiva.command.handler.ListCommandHandler;
import com.festiva.command.handler.RemoveCommandHandler;
import com.festiva.command.handler.SettingsCommandHandler;
import com.festiva.command.handler.UpcomingBirthdaysCommandHandler;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.state.UserStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.MaybeInaccessibleMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CallbackQueryHandler {

    private static final String MONTH_PREFIX   = "MONTH_";
    private static final String REMOVE_PREFIX  = "REMOVE_";
    private static final String CONFIRM_PREFIX = "CONFIRM_REMOVE_";
    private static final String CANCEL_REMOVE  = "CANCEL_REMOVE";
    private static final String LANG_PREFIX    = "LANG_";
    private static final String CURRENT_MONTH  = "CURRENT";
    private static final String LIST_SORT_DATE = "LIST_SORT_DATE";
    private static final String LIST_SORT_NAME = "LIST_SORT_NAME";

    private final FriendService friendService;
    private final UserStateService userStateService;
    private final UpcomingBirthdaysCommandHandler upcomingHandler;
    private final ListCommandHandler listHandler;
    private final BulkAddCommandHandler bulkAddHandler;
    private final DatePickerCallbackHandler datePickerHandler;
    private final EditCallbackHandler editHandler;
    private final RemoveCommandHandler removeCommandHandler;
    private final EditFriendCommandHandler editFriendCommandHandler;
    private final DeleteAccountCommandHandler deleteAccountHandler;

    public EditMessageText handle(CallbackQuery callbackQuery) {
        if (callbackQuery == null) return null;
        String data = callbackQuery.getData();
        MaybeInaccessibleMessage message = callbackQuery.getMessage();
        if (data == null || message == null) return null;

        long chatId = message.getChatId();
        int messageId = message.getMessageId();
        long userId = callbackQuery.getFrom().getId();
        Lang lang = userStateService.getLanguage(userId);

        CallbackResult result = dispatch(data, chatId, userId, lang);
        if (result == null) return null;
        if (result.sendMessage != null) return toEdit(result.sendMessage, messageId);

        EditMessageText.EditMessageTextBuilder<?, ?> builder = EditMessageText.builder()
                .chatId(chatId).messageId(messageId).parseMode("HTML").text(result.text);
        if (result.markup != null) builder.replyMarkup(result.markup);
        return builder.build();
    }

    private CallbackResult dispatch(String data, long chatId, long userId, Lang lang) {
        if (data.startsWith(DatePickerKeyboard.DATE_YEAR_PAGE_PREFIX))  return datePickerHandler.handleYearPage(data, userId, lang);
        if (data.startsWith(DatePickerKeyboard.DATE_YEAR_PREFIX))       return datePickerHandler.handleYearPick(data, userId, lang);
        if (data.startsWith(DatePickerKeyboard.DATE_MONTH_PREFIX))      return datePickerHandler.handleMonthPick(data, userId, lang);
        if (data.startsWith(DatePickerKeyboard.DATE_DAY_PREFIX))        return datePickerHandler.handleDayPick(data, userId, lang);
        if (data.startsWith(DatePickerKeyboard.DATE_BACK_TO_YEAR))      return datePickerHandler.handleBackToYear(data, userId, lang);
        if (DatePickerKeyboard.DATE_BACK_TO_MONTH.equals(data))         return datePickerHandler.handleBackToMonth(userId, lang);
        if (data.startsWith(DatePickerCallbackHandler.RELATIONSHIP_PREFIX)) return datePickerHandler.handleRelationship(data, userId, lang);
        if (data.startsWith(DatePickerCallbackHandler.EDIT_REL_PREFIX)) return datePickerHandler.handleEditRelationship(data, userId, lang);
        if (data.startsWith(SettingsCommandHandler.SETTINGS_HOUR_PREFIX)) return handleSettingsHour(data, userId, lang);
        if (data.startsWith(SettingsCommandHandler.SETTINGS_TZ_PREFIX))   return handleSettingsTz(data, userId, lang);
        if (data.startsWith(UpcomingBirthdaysCommandHandler.UPCOMING_DAYS_PREFIX)) return handleUpcoming(data, userId, lang);
        if (data.startsWith(ListCommandHandler.LIST_PAGE_PREFIX))       return handleListPage(data, userId, lang);
        if (data.startsWith(LIST_SORT_DATE) || data.startsWith(LIST_SORT_NAME)) return handleListSort(data, userId, lang);
        if ("ACTION_ADD".equals(data))                                  return handleActionAdd(userId, lang);
        if (BulkAddCommandHandler.CALLBACK_PASTE.equals(data))         return new CallbackResult(bulkAddHandler.promptPaste(chatId, userId, lang));
        if (BulkAddCommandHandler.CALLBACK_CSV.equals(data))           { bulkAddHandler.sendCsvTemplate(chatId, lang); return null; }
        if (data.startsWith(LANG_PREFIX))                               return handleLanguage(userId, data.substring(LANG_PREFIX.length()));
        if (data.startsWith(RemoveCommandHandler.REMOVE_PAGE_PREFIX))  return handleRemovePage(data, userId, lang);
        if (data.startsWith(EditFriendCommandHandler.EDIT_PAGE_PREFIX)) return handleEditPage(data, userId, lang);
        if (data.startsWith(EditCallbackHandler.EDIT_FIELD_NOTIFY))    return editHandler.handleEditNotify(data, userId, lang);
        if (data.startsWith(EditCallbackHandler.EDIT_FIELD_NAME))      return editHandler.handleEditFieldName(data, userId, lang);
        if (data.startsWith(EditCallbackHandler.EDIT_FIELD_DATE))      return editHandler.handleEditFieldDate(data, userId, lang);
        if (data.startsWith(EditCallbackHandler.EDIT_FIELD_REL))       return datePickerHandler.handleEditFieldRel(data, userId, lang, EditCallbackHandler.EDIT_FIELD_REL);
        if (data.startsWith(EditCallbackHandler.EDIT_PREFIX))          return editHandler.handleEditSelect(data, userId, lang);
        if (data.startsWith(REMOVE_PREFIX))                            return handleRemove(data, userId, lang);
        if (data.startsWith(CONFIRM_PREFIX))                           return handleConfirmRemove(userId, data.substring(CONFIRM_PREFIX.length()), lang);
        if (CANCEL_REMOVE.equals(data))                                return handleCancelRemove(userId, lang);
        if (DeleteAccountCommandHandler.CONFIRM_DELETE.equals(data))    return handleConfirmDeleteAccount(userId, lang);
        if (DeleteAccountCommandHandler.CANCEL_DELETE.equals(data))     return new CallbackResult(Messages.get(lang, Messages.DELETE_ACCOUNT_CANCEL), null);
        if (data.startsWith(MONTH_PREFIX))                             return handleMonth(userId, data, lang);
        log.debug("callback.unknown: data={}", data);
        return null;
    }

    // ── Settings ─────────────────────────────────────────────────────────────

    private CallbackResult handleSettingsHour(String data, long userId, Lang lang) {
        int hour = Integer.parseInt(data.substring(SettingsCommandHandler.SETTINGS_HOUR_PREFIX.length()));
        userStateService.setNotifyHour(userId, hour);
        return new CallbackResult(Messages.get(lang, Messages.SETTINGS_HOUR_SET, hour),
                SettingsCommandHandler.combined(hour, userStateService.getTimezone(userId)));
    }

    private CallbackResult handleSettingsTz(String data, long userId, Lang lang) {
        String tz = data.substring(SettingsCommandHandler.SETTINGS_TZ_PREFIX.length());
        userStateService.setTimezone(userId, tz);
        return new CallbackResult(Messages.get(lang, Messages.SETTINGS_TZ_SET, tz),
                SettingsCommandHandler.combined(userStateService.getNotifyHour(userId), tz));
    }

    // ── List ─────────────────────────────────────────────────────────────────

    private CallbackResult handleListSort(String data, long userId, Lang lang) {
        boolean byDate = data.startsWith(LIST_SORT_DATE);
        int page = parsePageSuffix(data);
        var friends = friendService.getFriendsSortedByDayMonth(userId);
        return new CallbackResult(listHandler.buildText(friends, lang, byDate, page),
                listHandler.keyboard(lang, byDate, page, friends.size()));
    }

    private CallbackResult handleListPage(String data, long userId, Lang lang) {
        String suffix = data.substring(ListCommandHandler.LIST_PAGE_PREFIX.length());
        boolean byDate = suffix.startsWith("DATE");
        int page = Integer.parseInt(suffix.substring(suffix.lastIndexOf('_') + 1));
        var friends = friendService.getFriendsSortedByDayMonth(userId);
        return new CallbackResult(listHandler.buildText(friends, lang, byDate, page),
                listHandler.keyboard(lang, byDate, page, friends.size()));
    }

    private int parsePageSuffix(String data) {
        int idx = data.lastIndexOf('_');
        if (idx < 0) return 0;
        try { return Integer.parseInt(data.substring(idx + 1)); } catch (NumberFormatException e) { return 0; }
    }

    // ── Upcoming ─────────────────────────────────────────────────────────────

    private CallbackResult handleUpcoming(String data, long userId, Lang lang) {
        int days = Integer.parseInt(data.substring(UpcomingBirthdaysCommandHandler.UPCOMING_DAYS_PREFIX.length()));
        var friends = friendService.getFriends(userId);
        return new CallbackResult(upcomingHandler.buildText(friends, lang, days), upcomingHandler.filterKeyboard(lang, days));
    }

    // ── Add / Language ────────────────────────────────────────────────────────

    private CallbackResult handleActionAdd(long userId, Lang lang) {
        userStateService.setState(userId, com.festiva.state.BotState.WAITING_FOR_ADD_FRIEND_NAME);
        return new CallbackResult(Messages.get(lang, Messages.ENTER_NAME), null);
    }

    private CallbackResult handleLanguage(long userId, String code) {
        try {
            Lang lang = Lang.valueOf(code);
            userStateService.setLanguage(userId, lang);
            log.debug("callback.language.changed: userId={}, lang={}", userId, lang);
            return new CallbackResult(Messages.get(lang, Messages.LANGUAGE_SET), null);
        } catch (IllegalArgumentException e) {
            log.warn("callback.language.unknown: code={}, reason={}", code, e.getMessage());
            return new CallbackResult(Messages.get(userStateService.getLanguage(userId), Messages.UNKNOWN_COMMAND), null);
        }
    }

    // ── Remove ────────────────────────────────────────────────────────────────

    private CallbackResult handleRemovePage(String data, long userId, Lang lang) {
        int page = Integer.parseInt(data.substring(RemoveCommandHandler.REMOVE_PAGE_PREFIX.length()));
        var friends = friendService.getFriendsSortedByDayMonth(userId);
        return new CallbackResult(Messages.get(lang, Messages.SELECT_REMOVE),
                removeCommandHandler.keyboard(friends, lang, page));
    }

    private CallbackResult handleEditPage(String data, long userId, Lang lang) {
        int page = Integer.parseInt(data.substring(EditFriendCommandHandler.EDIT_PAGE_PREFIX.length()));
        var friends = friendService.getFriendsSortedByDayMonth(userId);
        return new CallbackResult(Messages.get(lang, Messages.EDIT_SELECT),
                editFriendCommandHandler.keyboard(friends, lang, page));
    }

    private CallbackResult handleRemove(String data, long userId, Lang lang) {
        String id = data.substring(REMOVE_PREFIX.length());
        Friend friend = friendService.findFriendById(id).orElse(null);
        if (friend == null) return new CallbackResult(Messages.get(lang, Messages.UNKNOWN_COMMAND), null);
        String name = friend.getName();
        userStateService.setPendingName(userId, name);
        userStateService.setState(userId, com.festiva.state.BotState.WAITING_FOR_REMOVE_CONFIRM);
        return new CallbackResult(Messages.get(lang, Messages.CONFIRM_REMOVE_ASK, name), confirmKeyboard(id, name, lang));
    }

    private CallbackResult handleConfirmRemove(long userId, String id, Lang lang) {
        Friend friend = friendService.findFriendById(id).orElse(null);
        if (friend == null) return new CallbackResult(Messages.get(lang, Messages.FRIEND_NOT_FOUND, "?"), null);
        String name = friend.getName();
        friendService.deleteFriend(userId, name);
        userStateService.clearState(userId);
        log.debug("callback.friend.removed: userId={}, name={}", userId, name);
        return new CallbackResult(Messages.get(lang, Messages.FRIEND_REMOVED, name), null);
    }

    private CallbackResult handleCancelRemove(long userId, Lang lang) {
        userStateService.clearState(userId);
        return new CallbackResult(Messages.get(lang, Messages.CONFIRM_REMOVE_CANCEL), null);
    }

    private CallbackResult handleConfirmDeleteAccount(long userId, Lang lang) {
        deleteAccountHandler.deleteAccount(userId);
        return new CallbackResult(Messages.get(lang, Messages.DELETE_ACCOUNT_DONE), null);
    }

    // ── Month ─────────────────────────────────────────────────────────────────

    private CallbackResult handleMonth(long userId, String data, Lang lang) {
        String value = data.substring(MONTH_PREFIX.length());
        int month;
        if (CURRENT_MONTH.equalsIgnoreCase(value)) {
            month = LocalDate.now().getMonthValue();
        } else {
            try { month = Integer.parseInt(value); }
            catch (NumberFormatException e) {
                log.warn("callback.month.parse.failed: data={}, reason={}", data, e.getMessage());
                return new CallbackResult(Messages.get(lang, Messages.MONTH_PARSE_ERROR), null);
            }
        }
        var filtered = friendService.getFriendsSortedByDayMonth(userId).stream()
                .filter(f -> f.getBirthDate().getMonthValue() == month).toList();
        String raw = Month.of(month).getDisplayName(TextStyle.FULL_STANDALONE, lang.locale());
        String monthName = Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
        if (filtered.isEmpty()) return new CallbackResult(Messages.get(lang, Messages.BIRTHDAYS_NONE, monthName), null);

        LocalDate today = LocalDate.now();
        StringBuilder sb = new StringBuilder(Messages.get(lang, Messages.BIRTHDAYS_HEADER, monthName));
        filtered.forEach(f -> {
            boolean alreadyCelebrated = f.nextBirthday(today).getYear() > today.getYear();
            String ageLabel = alreadyCelebrated ? Messages.get(lang, Messages.YEARS_OLD, f.getAge())
                    : Messages.get(lang, Messages.YEARS_TURNS, f.getNextAge());
            sb.append("– <b>").append(f.getBirthDate().format(MessageBuilder.DATE_FORMATTER))
                    .append("</b> ").append(f.getName())
                    .append(" (<i>").append(ageLabel).append("</i>)\n");
        });
        return new CallbackResult(sb.toString(), null);
    }

    // ── Keyboards ─────────────────────────────────────────────────────────────

    private InlineKeyboardMarkup confirmKeyboard(String id, String name, Lang lang) {
        return InlineKeyboardMarkup.builder().keyboard(List.of(new InlineKeyboardRow(
                InlineKeyboardButton.builder().text(Messages.get(lang, Messages.CONFIRM_YES)).callbackData(CONFIRM_PREFIX + id).build(),
                InlineKeyboardButton.builder().text(Messages.get(lang, Messages.CONFIRM_NO)).callbackData(CANCEL_REMOVE).build()
        ))).build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private EditMessageText toEdit(SendMessage msg, int messageId) {
        return EditMessageText.builder()
                .chatId(msg.getChatId()).messageId(messageId).parseMode("HTML").text(msg.getText()).build();
    }
}
