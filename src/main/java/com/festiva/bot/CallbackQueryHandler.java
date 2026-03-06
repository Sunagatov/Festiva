package com.festiva.bot;

import com.festiva.command.DatePickerKeyboard;
import com.festiva.command.MessageBuilder;
import com.festiva.command.handler.BulkAddCommandHandler;
import com.festiva.command.handler.ListCommandHandler;
import com.festiva.command.handler.SettingsCommandHandler;
import com.festiva.command.handler.UpcomingBirthdaysCommandHandler;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.friend.entity.Relationship;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.state.BotState;
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
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CallbackQueryHandler {

    private static final String MONTH_PREFIX        = "MONTH_";
    private static final String REMOVE_PREFIX       = "REMOVE_";
    private static final String CONFIRM_PREFIX      = "CONFIRM_REMOVE_";
    private static final String CANCEL_REMOVE       = "CANCEL_REMOVE";
    private static final String ACTION_ADD          = "ACTION_ADD";
    private static final String EDIT_PREFIX         = "EDIT_";
    private static final String EDIT_FIELD_NAME     = "EDIT_FIELD_NAME_";
    private static final String EDIT_FIELD_DATE     = "EDIT_FIELD_DATE_";
    private static final String EDIT_FIELD_NOTIFY   = "EDIT_FIELD_NOTIFY_";
    private static final String EDIT_FIELD_REL      = "EDIT_FIELD_REL_";
    private static final String EDIT_REL_PREFIX     = "EDIT_REL_";
    private static final String LANG_PREFIX         = "LANG_";
    private static final String CURRENT_MONTH       = "CURRENT";
    private static final String RELATIONSHIP_PREFIX = "RELATIONSHIP_";
    private static final String LIST_SORT_DATE      = "LIST_SORT_DATE";
    private static final String LIST_SORT_NAME      = "LIST_SORT_NAME";

    private final FriendService friendService;
    private final UserStateService userStateService;
    private final UpcomingBirthdaysCommandHandler upcomingHandler;
    private final ListCommandHandler listHandler;
    private final BulkAddCommandHandler bulkAddHandler;

    public EditMessageText handle(CallbackQuery callbackQuery) {
        if (callbackQuery == null) return null;
        String data = callbackQuery.getData();
        MaybeInaccessibleMessage message = callbackQuery.getMessage();
        if (data == null || message == null) return null;

        long chatId = message.getChatId();
        int messageId = message.getMessageId();
        long userId = callbackQuery.getFrom().getId();
        Lang lang = userStateService.getLanguage(userId);

        Result result = dispatch(data, chatId, userId, lang);
        if (result == null) return null;
        if (result.sendMessage != null) return toEdit(result.sendMessage, messageId);

        EditMessageText.EditMessageTextBuilder<?, ?> builder = EditMessageText.builder()
                .chatId(chatId).messageId(messageId).parseMode("HTML").text(result.text);
        if (result.markup != null) builder.replyMarkup(result.markup);
        return builder.build();
    }

    private Result dispatch(String data, long chatId, long userId, Lang lang) {
        if (data.startsWith(DatePickerKeyboard.DATE_YEAR_PAGE_PREFIX))  return handleYearPage(data, userId, lang);
        if (data.startsWith(DatePickerKeyboard.DATE_YEAR_PREFIX))       return handleYearPick(data, userId, lang);
        if (data.startsWith(DatePickerKeyboard.DATE_MONTH_PREFIX))      return handleMonthPick(data, userId, lang);
        if (data.startsWith(DatePickerKeyboard.DATE_DAY_PREFIX))        return handleDayPick(data, userId, lang);
        if (data.startsWith(DatePickerKeyboard.DATE_BACK_TO_YEAR))      return handleBackToYear(data, userId, lang);
        if (DatePickerKeyboard.DATE_BACK_TO_MONTH.equals(data))         return handleBackToMonth(userId, lang);
        if (data.startsWith(RELATIONSHIP_PREFIX))                       return handleRelationship(data, userId, lang);
        if (data.startsWith(EDIT_REL_PREFIX))                           return handleEditRelationship(data, userId, lang);
        if (data.startsWith(SettingsCommandHandler.SETTINGS_HOUR_PREFIX)) return handleSettingsHour(data, userId, lang);
        if (data.startsWith(SettingsCommandHandler.SETTINGS_TZ_PREFIX))   return handleSettingsTz(data, userId, lang);
        if (data.startsWith(UpcomingBirthdaysCommandHandler.UPCOMING_DAYS_PREFIX)) return handleUpcoming(data, userId, lang);
        if (data.startsWith(ListCommandHandler.LIST_PAGE_PREFIX))       return handleListPage(data, userId, lang);
        if (data.startsWith(LIST_SORT_DATE) || data.startsWith(LIST_SORT_NAME)) return handleListSort(data, userId, lang);
        if (ACTION_ADD.equals(data))                                    return handleActionAdd(userId, lang);
        if (BulkAddCommandHandler.CALLBACK_PASTE.equals(data))         return new Result(bulkAddHandler.promptPaste(chatId, userId, lang));
        if (BulkAddCommandHandler.CALLBACK_CSV.equals(data))           { bulkAddHandler.sendCsvTemplate(chatId, lang); return null; }
        if (data.startsWith(LANG_PREFIX))                               return handleLanguage(userId, data.substring(LANG_PREFIX.length()));
        if (data.startsWith(EDIT_FIELD_NOTIFY))                        return handleEditNotify(data, userId, lang);
        if (data.startsWith(EDIT_FIELD_NAME))                          return handleEditFieldName(data, userId, lang);
        if (data.startsWith(EDIT_FIELD_DATE))                          return handleEditFieldDate(data, userId, lang);
        if (data.startsWith(EDIT_FIELD_REL))                           return handleEditFieldRel(data, userId, lang);
        if (data.startsWith(EDIT_PREFIX))                              return handleEditSelect(data, userId, lang);
        if (data.startsWith(REMOVE_PREFIX))                            return handleRemove(data, userId, lang);
        if (data.startsWith(CONFIRM_PREFIX))                           return handleConfirmRemove(userId, data.substring(CONFIRM_PREFIX.length()), lang);
        if (CANCEL_REMOVE.equals(data))                                return handleCancelRemove(userId, lang);
        if (data.startsWith(MONTH_PREFIX))                             return handleMonth(userId, data, lang);
        log.debug("callback.unknown: data={}", data);
        return null;
    }

    // ── Date picker ──────────────────────────────────────────────────────────

    private Result handleYearPage(String data, long userId, Lang lang) {
        int offset = Integer.parseInt(data.substring(DatePickerKeyboard.DATE_YEAR_PAGE_PREFIX.length()));
        userStateService.setYearPageOffset(userId, offset);
        return new Result(Messages.get(lang, Messages.DATE_PICK_YEAR, userStateService.getPendingName(userId)),
                DatePickerKeyboard.yearKeyboard(offset, lang));
    }

    private Result handleYearPick(String data, long userId, Lang lang) {
        int year = Integer.parseInt(data.substring(DatePickerKeyboard.DATE_YEAR_PREFIX.length()));
        userStateService.setPendingYear(userId, year);
        return new Result(Messages.get(lang, Messages.DATE_PICK_MONTH, userStateService.getPendingName(userId)),
                DatePickerKeyboard.monthKeyboard(lang, userStateService.getYearPageOffset(userId)));
    }

    private Result handleMonthPick(String data, long userId, Lang lang) {
        int month = Integer.parseInt(data.substring(DatePickerKeyboard.DATE_MONTH_PREFIX.length()));
        userStateService.setPendingMonth(userId, month);
        return new Result(Messages.get(lang, Messages.DATE_PICK_DAY, userStateService.getPendingName(userId)),
                DatePickerKeyboard.dayKeyboard(userStateService.getPendingYear(userId), month, lang));
    }

    private Result handleDayPick(String data, long userId, Lang lang) {
        int day = Integer.parseInt(data.substring(DatePickerKeyboard.DATE_DAY_PREFIX.length()));
        Integer year = userStateService.getPendingYear(userId);
        Integer month = userStateService.getPendingMonth(userId);
        String name = userStateService.getPendingName(userId);
        LocalDate birthDate = LocalDate.of(year, month, day);
        if (userStateService.getState(userId) == BotState.WAITING_FOR_EDIT_DATE) {
            friendService.updateFriendDate(userId, name, birthDate);
            userStateService.clearState(userId);
            log.debug("friend.date.updated: userId={}, name={}", userId, name);
            return new Result(Messages.get(lang, Messages.EDIT_DATE_DONE, name), null);
        }
        userStateService.setPendingYear(userId, year);
        userStateService.setPendingMonth(userId, month);
        userStateService.setPendingDay(userId, day);
        userStateService.setState(userId, BotState.WAITING_FOR_ADD_FRIEND_RELATIONSHIP);
        return new Result(Messages.get(lang, Messages.RELATIONSHIP_PICK, name), relationshipKeyboard(lang));
    }

    private Result handleBackToYear(String data, long userId, Lang lang) {
        int offset = Integer.parseInt(data.substring(DatePickerKeyboard.DATE_BACK_TO_YEAR.length() + 1));
        userStateService.setYearPageOffset(userId, offset);
        userStateService.setPendingYear(userId, null);
        return new Result(Messages.get(lang, Messages.DATE_PICK_YEAR, userStateService.getPendingName(userId)),
                DatePickerKeyboard.yearKeyboard(offset, lang));
    }

    private Result handleBackToMonth(long userId, Lang lang) {
        userStateService.setPendingMonth(userId, null);
        return new Result(Messages.get(lang, Messages.DATE_PICK_MONTH, userStateService.getPendingName(userId)),
                DatePickerKeyboard.monthKeyboard(lang, userStateService.getYearPageOffset(userId)));
    }

    // ── Relationship (add flow) ───────────────────────────────────────────────

    private Result handleRelationship(String data, long userId, Lang lang) {
        String name = userStateService.getPendingName(userId);
        Integer year = userStateService.getPendingYear(userId);
        Integer month = userStateService.getPendingMonth(userId);
        Integer day = userStateService.getPendingDay(userId);
        LocalDate birthDate = LocalDate.of(year, month, day);
        Relationship rel = "SKIP".equals(data.substring(RELATIONSHIP_PREFIX.length())) ? null
                : Relationship.valueOf(data.substring(RELATIONSHIP_PREFIX.length()));
        friendService.addFriend(userId, new Friend(name, birthDate, rel));
        userStateService.clearState(userId);
        log.debug("friend.added: userId={}, name={}, relationship={}", userId, name, rel);
        return new Result(Messages.get(lang, Messages.FRIEND_ADDED, name),
                InlineKeyboardMarkup.builder().keyboard(List.of(new InlineKeyboardRow(
                        InlineKeyboardButton.builder().text(Messages.get(lang, Messages.QUICK_LIST)).callbackData(LIST_SORT_DATE + "_0").build(),
                        InlineKeyboardButton.builder().text(Messages.get(lang, Messages.QUICK_ADD_ANOTHER)).callbackData(ACTION_ADD).build()
                ))).build());
    }

    // ── Relationship (edit flow) ──────────────────────────────────────────────

    private Result handleEditFieldRel(String data, long userId, Lang lang) {
        String name = data.substring(EDIT_FIELD_REL.length());
        userStateService.setPendingName(userId, name);
        userStateService.setState(userId, BotState.WAITING_FOR_EDIT_RELATIONSHIP);
        return new Result(Messages.get(lang, Messages.RELATIONSHIP_PICK, name), editRelKeyboard(lang));
    }

    private Result handleEditRelationship(String data, long userId, Lang lang) {
        String name = userStateService.getPendingName(userId);
        String value = data.substring(EDIT_REL_PREFIX.length());
        Relationship rel = "SKIP".equals(value) ? null : Relationship.valueOf(value);
        friendService.updateFriendRelationship(userId, name, rel);
        userStateService.clearState(userId);
        log.debug("friend.relationship.updated: userId={}, name={}, rel={}", userId, name, rel);
        return new Result(Messages.get(lang, Messages.EDIT_REL_DONE, name), null);
    }

    // ── Settings ─────────────────────────────────────────────────────────────

    private Result handleSettingsHour(String data, long userId, Lang lang) {
        int hour = Integer.parseInt(data.substring(SettingsCommandHandler.SETTINGS_HOUR_PREFIX.length()));
        userStateService.setNotifyHour(userId, hour);
        return new Result(Messages.get(lang, Messages.SETTINGS_HOUR_SET, hour),
                SettingsCommandHandler.combined(hour, userStateService.getTimezone(userId)));
    }

    private Result handleSettingsTz(String data, long userId, Lang lang) {
        String tz = data.substring(SettingsCommandHandler.SETTINGS_TZ_PREFIX.length());
        userStateService.setTimezone(userId, tz);
        return new Result(Messages.get(lang, Messages.SETTINGS_TZ_SET, tz),
                SettingsCommandHandler.combined(userStateService.getNotifyHour(userId), tz));
    }

    // ── List ─────────────────────────────────────────────────────────────────

    private Result handleListSort(String data, long userId, Lang lang) {
        // format: LIST_SORT_DATE_<page> or LIST_SORT_NAME_<page>
        boolean byDate = data.startsWith(LIST_SORT_DATE);
        int page = parsePageSuffix(data);
        List<Friend> friends = friendService.getFriendsSortedByDayMonth(userId);
        return new Result(listHandler.buildText(friends, lang, byDate, page),
                listHandler.keyboard(lang, byDate, page, friends.size()));
    }

    private Result handleListPage(String data, long userId, Lang lang) {
        // format: LIST_PAGE_DATE_<page> or LIST_PAGE_NAME_<page>
        String suffix = data.substring(ListCommandHandler.LIST_PAGE_PREFIX.length());
        boolean byDate = suffix.startsWith("DATE");
        int page = Integer.parseInt(suffix.substring(suffix.lastIndexOf('_') + 1));
        List<Friend> friends = friendService.getFriendsSortedByDayMonth(userId);
        return new Result(listHandler.buildText(friends, lang, byDate, page),
                listHandler.keyboard(lang, byDate, page, friends.size()));
    }

    private int parsePageSuffix(String data) {
        int idx = data.lastIndexOf('_');
        if (idx < 0) return 0;
        try { return Integer.parseInt(data.substring(idx + 1)); } catch (NumberFormatException e) { return 0; }
    }

    // ── Upcoming ─────────────────────────────────────────────────────────────

    private Result handleUpcoming(String data, long userId, Lang lang) {
        int days = Integer.parseInt(data.substring(UpcomingBirthdaysCommandHandler.UPCOMING_DAYS_PREFIX.length()));
        List<Friend> friends = friendService.getFriends(userId);
        return new Result(upcomingHandler.buildText(friends, lang, days), upcomingHandler.filterKeyboard(lang, days));
    }

    // ── Add / Language ────────────────────────────────────────────────────────

    private Result handleActionAdd(long userId, Lang lang) {
        userStateService.setState(userId, BotState.WAITING_FOR_ADD_FRIEND_NAME);
        return new Result(Messages.get(lang, Messages.ENTER_NAME), null);
    }

    private Result handleLanguage(long userId, String code) {
        try {
            Lang lang = Lang.valueOf(code);
            userStateService.setLanguage(userId, lang);
            log.debug("callback.language.changed: userId={}, lang={}", userId, lang);
            return new Result(Messages.get(lang, Messages.LANGUAGE_SET), null);
        } catch (IllegalArgumentException e) {
            log.warn("callback.language.unknown: code={}, reason={}", code, e.getMessage());
            return new Result(Messages.get(userStateService.getLanguage(userId), Messages.UNKNOWN_COMMAND), null);
        }
    }

    // ── Edit ─────────────────────────────────────────────────────────────────

    private Result handleEditNotify(String data, long userId, Lang lang) {
        String name = data.substring(EDIT_FIELD_NOTIFY.length());
        friendService.toggleFriendNotify(userId, name);
        boolean enabled = friendService.getFriends(userId).stream()
                .filter(f -> f.getName().equalsIgnoreCase(name))
                .findFirst().map(Friend::isNotifyEnabled).orElse(true);
        return new Result(Messages.get(lang, Messages.EDIT_NOTIFY_TOGGLED, name,
                Messages.get(lang, enabled ? Messages.NOTIFY_STATUS_ON : Messages.NOTIFY_STATUS_OFF)), null);
    }

    private Result handleEditFieldName(String data, long userId, Lang lang) {
        String name = data.substring(EDIT_FIELD_NAME.length());
        userStateService.setPendingName(userId, name);
        userStateService.setState(userId, BotState.WAITING_FOR_EDIT_NAME);
        return new Result(Messages.get(lang, Messages.EDIT_ENTER_NAME, name), null);
    }

    private Result handleEditFieldDate(String data, long userId, Lang lang) {
        String name = data.substring(EDIT_FIELD_DATE.length());
        userStateService.setPendingName(userId, name);
        userStateService.setYearPageOffset(userId, 0);
        userStateService.setState(userId, BotState.WAITING_FOR_EDIT_DATE);
        return new Result(Messages.get(lang, Messages.DATE_PICK_YEAR, name), DatePickerKeyboard.yearKeyboard(0, lang));
    }

    private Result handleEditSelect(String data, long userId, Lang lang) {
        if (data.startsWith(EDIT_FIELD_NAME) || data.startsWith(EDIT_FIELD_DATE)
                || data.startsWith(EDIT_FIELD_NOTIFY) || data.startsWith(EDIT_FIELD_REL)) return null;
        String name = data.substring(EDIT_PREFIX.length());
        Friend found = friendService.getFriends(userId).stream()
                .filter(f -> f.getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
        String currentDate = found != null ? found.getBirthDate().format(MessageBuilder.DATE_FORMATTER) : "?";
        boolean notifyOn = found == null || found.isNotifyEnabled();
        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder().keyboard(List.of(
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder().text(Messages.get(lang, Messages.EDIT_FIELD_NAME_BTN)).callbackData(EDIT_FIELD_NAME + name).build(),
                        InlineKeyboardButton.builder().text(Messages.get(lang, Messages.EDIT_FIELD_DATE_BTN)).callbackData(EDIT_FIELD_DATE + name).build()),
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder().text(Messages.get(lang, Messages.EDIT_FIELD_REL_BTN)).callbackData(EDIT_FIELD_REL + name).build(),
                        InlineKeyboardButton.builder().text(notifyOn ? Messages.get(lang, Messages.EDIT_NOTIFS_ON) : Messages.get(lang, Messages.EDIT_NOTIFS_OFF)).callbackData(EDIT_FIELD_NOTIFY + name).build())
        )).build();
        return new Result(Messages.get(lang, Messages.EDIT_CHOOSE_FIELD, name, currentDate), markup);
    }

    // ── Remove ────────────────────────────────────────────────────────────────

    private Result handleRemove(String data, long userId, Lang lang) {
        String name = data.substring(REMOVE_PREFIX.length());
        userStateService.setPendingName(userId, name);
        userStateService.setState(userId, BotState.WAITING_FOR_REMOVE_CONFIRM);
        return new Result(Messages.get(lang, Messages.CONFIRM_REMOVE_ASK, name), confirmKeyboard(name, lang));
    }

    private Result handleConfirmRemove(long userId, String name, Lang lang) {
        if (!friendService.friendExists(userId, name))
            return new Result(Messages.get(lang, Messages.FRIEND_NOT_FOUND, name), null);
        friendService.deleteFriend(userId, name);
        userStateService.clearState(userId);
        log.debug("callback.friend.removed: userId={}, name={}", userId, name);
        return new Result(Messages.get(lang, Messages.FRIEND_REMOVED, name), null);
    }

    private Result handleCancelRemove(long userId, Lang lang) {
        userStateService.clearState(userId);
        return new Result(Messages.get(lang, Messages.CONFIRM_REMOVE_CANCEL), null);
    }

    // ── Month ─────────────────────────────────────────────────────────────────

    private Result handleMonth(long userId, String data, Lang lang) {
        String value = data.substring(MONTH_PREFIX.length());
        int month;
        if (CURRENT_MONTH.equalsIgnoreCase(value)) {
            month = LocalDate.now().getMonthValue();
        } else {
            try { month = Integer.parseInt(value); }
            catch (NumberFormatException e) {
                log.warn("callback.month.parse.failed: data={}, reason={}", data, e.getMessage());
                return new Result(Messages.get(lang, Messages.MONTH_PARSE_ERROR), null);
            }
        }
        List<Friend> filtered = friendService.getFriendsSortedByDayMonth(userId).stream()
                .filter(f -> f.getBirthDate().getMonthValue() == month).toList();
        String raw = Month.of(month).getDisplayName(TextStyle.FULL_STANDALONE, lang.locale());
        String monthName = Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
        if (filtered.isEmpty()) return new Result(Messages.get(lang, Messages.BIRTHDAYS_NONE, monthName), null);

        StringBuilder sb = new StringBuilder(Messages.get(lang, Messages.BIRTHDAYS_HEADER, monthName));
        LocalDate today = LocalDate.now();
        filtered.forEach(f -> {
            boolean isFuture = !f.nextBirthday(today).isBefore(today);
            String ageLabel = isFuture ? Messages.get(lang, Messages.YEARS_TURNS, f.getNextAge())
                    : Messages.get(lang, Messages.YEARS_OLD, f.getAge());
            sb.append("– <b>").append(f.getBirthDate().format(MessageBuilder.DATE_FORMATTER))
                    .append("</b> ").append(f.getName())
                    .append(" (<i>").append(ageLabel).append("</i>)\n");
        });
        return new Result(sb.toString(), null);
    }

    // ── Keyboards ─────────────────────────────────────────────────────────────

    private InlineKeyboardMarkup relationshipKeyboard(Lang lang) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        InlineKeyboardRow row = new InlineKeyboardRow();
        for (Relationship r : Relationship.values()) {
            row.add(InlineKeyboardButton.builder().text(r.label(lang)).callbackData(RELATIONSHIP_PREFIX + r.name()).build());
            if (row.size() == 3) { rows.add(row); row = new InlineKeyboardRow(); }
        }
        if (!row.isEmpty()) rows.add(row);
        rows.add(new InlineKeyboardRow(
                InlineKeyboardButton.builder().text(Messages.get(lang, Messages.RELATIONSHIP_SKIP)).callbackData(RELATIONSHIP_PREFIX + "SKIP").build()));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private InlineKeyboardMarkup editRelKeyboard(Lang lang) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        InlineKeyboardRow row = new InlineKeyboardRow();
        for (Relationship r : Relationship.values()) {
            row.add(InlineKeyboardButton.builder().text(r.label(lang)).callbackData(EDIT_REL_PREFIX + r.name()).build());
            if (row.size() == 3) { rows.add(row); row = new InlineKeyboardRow(); }
        }
        if (!row.isEmpty()) rows.add(row);
        rows.add(new InlineKeyboardRow(
                InlineKeyboardButton.builder().text(Messages.get(lang, Messages.RELATIONSHIP_SKIP)).callbackData(EDIT_REL_PREFIX + "SKIP").build()));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private InlineKeyboardMarkup confirmKeyboard(String name, Lang lang) {
        return InlineKeyboardMarkup.builder().keyboard(List.of(new InlineKeyboardRow(
                InlineKeyboardButton.builder().text(Messages.get(lang, Messages.CONFIRM_YES)).callbackData(CONFIRM_PREFIX + name).build(),
                InlineKeyboardButton.builder().text(Messages.get(lang, Messages.CONFIRM_NO)).callbackData(CANCEL_REMOVE).build()
        ))).build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private EditMessageText toEdit(SendMessage msg, int messageId) {
        return EditMessageText.builder()
                .chatId(msg.getChatId()).messageId(messageId).parseMode("HTML").text(msg.getText()).build();
    }

    private static final class Result {
        final String text;
        final InlineKeyboardMarkup markup;
        final SendMessage sendMessage;
        Result(String text, InlineKeyboardMarkup markup) { this.text = text; this.markup = markup; this.sendMessage = null; }
        Result(SendMessage sendMessage) { this.text = null; this.markup = null; this.sendMessage = sendMessage; }
    }
}
