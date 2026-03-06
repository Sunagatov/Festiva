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

    private static final String MONTH_PREFIX      = "MONTH_";
    private static final String REMOVE_PREFIX     = "REMOVE_";
    private static final String CONFIRM_PREFIX    = "CONFIRM_REMOVE_";
    private static final String CANCEL_REMOVE     = "CANCEL_REMOVE";
    private static final String ACTION_ADD        = "ACTION_ADD";
    private static final String EDIT_PREFIX       = "EDIT_";
    private static final String EDIT_FIELD_NAME   = "EDIT_FIELD_NAME_";
    private static final String EDIT_FIELD_DATE   = "EDIT_FIELD_DATE_";
    private static final String EDIT_FIELD_NOTIFY = "EDIT_FIELD_NOTIFY_";
    private static final String LANG_PREFIX       = "LANG_";
    private static final String CURRENT_MONTH     = "CURRENT";
    private static final String RELATIONSHIP_PREFIX = "RELATIONSHIP_";

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

        String text;
        InlineKeyboardMarkup markup = null;

        if (data.startsWith(DatePickerKeyboard.DATE_YEAR_PAGE_PREFIX)) {
            int offset = Integer.parseInt(data.substring(DatePickerKeyboard.DATE_YEAR_PAGE_PREFIX.length()));
            userStateService.setYearPageOffset(userId, offset);
            String name = userStateService.getPendingName(userId);
            text = Messages.get(lang, Messages.DATE_PICK_YEAR, name);
            markup = DatePickerKeyboard.yearKeyboard(offset, lang);
        } else if (data.startsWith(DatePickerKeyboard.DATE_YEAR_PREFIX)) {
            int year = Integer.parseInt(data.substring(DatePickerKeyboard.DATE_YEAR_PREFIX.length()));
            userStateService.setPendingYear(userId, year);
            String name = userStateService.getPendingName(userId);
            text = Messages.get(lang, Messages.DATE_PICK_MONTH, name);
            markup = DatePickerKeyboard.monthKeyboard(lang, userStateService.getYearPageOffset(userId));
        } else if (data.startsWith(DatePickerKeyboard.DATE_MONTH_PREFIX)) {
            int month = Integer.parseInt(data.substring(DatePickerKeyboard.DATE_MONTH_PREFIX.length()));
            userStateService.setPendingMonth(userId, month);
            Integer year = userStateService.getPendingYear(userId);
            String name = userStateService.getPendingName(userId);
            text = Messages.get(lang, Messages.DATE_PICK_DAY, name);
            markup = DatePickerKeyboard.dayKeyboard(year, month, lang);
        } else if (data.startsWith(DatePickerKeyboard.DATE_DAY_PREFIX)) {
            int day = Integer.parseInt(data.substring(DatePickerKeyboard.DATE_DAY_PREFIX.length()));
            Integer year = userStateService.getPendingYear(userId);
            Integer month = userStateService.getPendingMonth(userId);
            String name = userStateService.getPendingName(userId);
            LocalDate birthDate = LocalDate.of(year, month, day);
            if (userStateService.getState(userId) == BotState.WAITING_FOR_EDIT_DATE) {
                friendService.updateFriendDate(userId, name, birthDate);
                userStateService.clearState(userId);
                log.debug("friend.date.updated: userId={}, name={}", userId, name);
                text = Messages.get(lang, Messages.EDIT_DATE_DONE, name);
            } else {
                // store date, ask for relationship
                userStateService.setPendingYear(userId, year);
                userStateService.setPendingMonth(userId, month);
                userStateService.setPendingDay(userId, day);
                userStateService.setState(userId, BotState.WAITING_FOR_ADD_FRIEND_RELATIONSHIP);
                text = Messages.get(lang, Messages.RELATIONSHIP_PICK, name);
                markup = relationshipKeyboard(lang);
            }
        } else if (data.startsWith(RELATIONSHIP_PREFIX)) {
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
            text = Messages.get(lang, Messages.FRIEND_ADDED, name);
            markup = InlineKeyboardMarkup.builder().keyboard(List.of(new InlineKeyboardRow(
                    InlineKeyboardButton.builder().text(Messages.get(lang, Messages.QUICK_ADD_ANOTHER)).callbackData("ACTION_ADD").build(),
                    InlineKeyboardButton.builder().text(Messages.get(lang, Messages.QUICK_LIST)).callbackData("LIST_SORT_DATE").build()
            ))).build();
        } else if (data.startsWith(DatePickerKeyboard.DATE_BACK_TO_YEAR)) {
            int offset = Integer.parseInt(data.substring(DatePickerKeyboard.DATE_BACK_TO_YEAR.length() + 1));
            userStateService.setYearPageOffset(userId, offset);
            userStateService.setPendingYear(userId, null);
            String name = userStateService.getPendingName(userId);
            text = Messages.get(lang, Messages.DATE_PICK_YEAR, name);
            markup = DatePickerKeyboard.yearKeyboard(offset, lang);
        } else if (DatePickerKeyboard.DATE_BACK_TO_MONTH.equals(data)) {
            Integer year = userStateService.getPendingYear(userId);
            userStateService.setPendingMonth(userId, null);
            String name = userStateService.getPendingName(userId);
            text = Messages.get(lang, Messages.DATE_PICK_MONTH, name);
            markup = DatePickerKeyboard.monthKeyboard(lang, userStateService.getYearPageOffset(userId));
        } else if (data.startsWith(SettingsCommandHandler.SETTINGS_HOUR_PREFIX)) {
            int hour = Integer.parseInt(data.substring(SettingsCommandHandler.SETTINGS_HOUR_PREFIX.length()));
            userStateService.setNotifyHour(userId, hour);
            String tz = userStateService.getTimezone(userId);
            text = Messages.get(lang, Messages.SETTINGS_HOUR_SET, hour);
            markup = SettingsCommandHandler.combined(hour, tz);
        } else if (data.startsWith(SettingsCommandHandler.SETTINGS_TZ_PREFIX)) {
            String tz = data.substring(SettingsCommandHandler.SETTINGS_TZ_PREFIX.length());
            userStateService.setTimezone(userId, tz);
            int hour = userStateService.getNotifyHour(userId);
            text = Messages.get(lang, Messages.SETTINGS_TZ_SET, tz);
            markup = SettingsCommandHandler.combined(hour, tz);
        } else if (data.startsWith(UpcomingBirthdaysCommandHandler.UPCOMING_DAYS_PREFIX)) {
            int days = Integer.parseInt(data.substring(UpcomingBirthdaysCommandHandler.UPCOMING_DAYS_PREFIX.length()));
            List<com.festiva.friend.entity.Friend> friends = friendService.getFriends(userId);
            text = upcomingHandler.buildText(friends, lang, days);
            markup = upcomingHandler.filterKeyboard(lang, days);
        } else if ("LIST_SORT_DATE".equals(data) || "LIST_SORT_NAME".equals(data)) {
            boolean byDate = "LIST_SORT_DATE".equals(data);
            List<com.festiva.friend.entity.Friend> friends = friendService.getFriendsSortedByDayMonth(userId);
            text = listHandler.buildText(friends, lang, byDate);
            markup = listHandler.sortKeyboard(lang, byDate);
        } else if (ACTION_ADD.equals(data)) {
            userStateService.setState(userId, BotState.WAITING_FOR_ADD_FRIEND_NAME);
            text = Messages.get(lang, Messages.ENTER_NAME);
        } else if (BulkAddCommandHandler.CALLBACK_PASTE.equals(data)) {
            return toEdit(bulkAddHandler.promptPaste(chatId, userId, lang), messageId);
        } else if (BulkAddCommandHandler.CALLBACK_CSV.equals(data)) {
            bulkAddHandler.sendCsvTemplate(chatId, lang);
            return null;
        } else if (data.startsWith(LANG_PREFIX)) {
            text = handleLanguage(userId, data.substring(LANG_PREFIX.length()));
        } else if (data.startsWith(EDIT_FIELD_NOTIFY)) {
            String name = data.substring(EDIT_FIELD_NOTIFY.length());
            friendService.toggleFriendNotify(userId, name);
            boolean enabled = friendService.getFriends(userId).stream()
                    .filter(f -> f.getName().equalsIgnoreCase(name))
                    .findFirst().map(com.festiva.friend.entity.Friend::isNotifyEnabled).orElse(true);
            text = Messages.get(lang, Messages.EDIT_NOTIFY_TOGGLED, name,
                    Messages.get(lang, enabled ? Messages.NOTIFY_STATUS_ON : Messages.NOTIFY_STATUS_OFF));
        } else if (data.startsWith(EDIT_FIELD_NAME)) {
            String name = data.substring(EDIT_FIELD_NAME.length());
            userStateService.setPendingName(userId, name);
            userStateService.setState(userId, BotState.WAITING_FOR_EDIT_NAME);
            text = Messages.get(lang, Messages.EDIT_ENTER_NAME, name);
        } else if (data.startsWith(EDIT_FIELD_DATE)) {
            String name = data.substring(EDIT_FIELD_DATE.length());
            userStateService.setPendingName(userId, name);
            userStateService.setYearPageOffset(userId, 0);
            userStateService.setState(userId, BotState.WAITING_FOR_EDIT_DATE);
            text = Messages.get(lang, Messages.DATE_PICK_YEAR, name);
            markup = DatePickerKeyboard.yearKeyboard(0, lang);
        } else if (data.startsWith(EDIT_PREFIX) && !data.startsWith(EDIT_FIELD_NAME) && !data.startsWith(EDIT_FIELD_DATE) && !data.startsWith(EDIT_FIELD_NOTIFY)) {
            String name = data.substring(EDIT_PREFIX.length());
            String currentDate = friendService.getFriends(userId).stream()
                    .filter(f -> f.getName().equalsIgnoreCase(name))
                    .findFirst()
                    .map(f -> f.getBirthDate().format(MessageBuilder.DATE_FORMATTER))
                    .orElse("?");
            text = Messages.get(lang, Messages.EDIT_CHOOSE_FIELD, name, currentDate);
            boolean notifyOn = friendService.getFriends(userId).stream()
                    .filter(f -> f.getName().equalsIgnoreCase(name))
                    .findFirst().map(com.festiva.friend.entity.Friend::isNotifyEnabled).orElse(true);
            markup = InlineKeyboardMarkup.builder()
                    .keyboard(List.of(
                            new InlineKeyboardRow(
                                    InlineKeyboardButton.builder().text(Messages.get(lang, Messages.EDIT_FIELD_NAME_BTN)).callbackData(EDIT_FIELD_NAME + name).build(),
                                    InlineKeyboardButton.builder().text(Messages.get(lang, Messages.EDIT_FIELD_DATE_BTN)).callbackData(EDIT_FIELD_DATE + name).build()),
                            new InlineKeyboardRow(
                                    InlineKeyboardButton.builder().text(notifyOn ? Messages.get(lang, Messages.EDIT_NOTIFS_ON) : Messages.get(lang, Messages.EDIT_NOTIFS_OFF)).callbackData(EDIT_FIELD_NOTIFY + name).build())
                    ))
                    .build();
        } else if (data.startsWith(REMOVE_PREFIX)) {
            String name = data.substring(REMOVE_PREFIX.length());
            text = Messages.get(lang, Messages.CONFIRM_REMOVE_ASK, name);
            markup = confirmKeyboard(name, lang);
            userStateService.setPendingName(userId, name);
            userStateService.setState(userId, BotState.WAITING_FOR_REMOVE_CONFIRM);
        } else if (data.startsWith(CONFIRM_PREFIX)) {
            text = handleConfirmRemove(userId, data.substring(CONFIRM_PREFIX.length()), lang);
        } else if (CANCEL_REMOVE.equals(data)) {
            userStateService.clearState(userId);
            text = Messages.get(lang, Messages.CONFIRM_REMOVE_CANCEL);
        } else if (data.startsWith(MONTH_PREFIX)) {
            text = handleMonth(userId, data, lang);
        } else {
            log.debug("callback.unknown: data={}", data);
            return null;
        }

        EditMessageText.EditMessageTextBuilder builder = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .parseMode("HTML")
                .text(text);
        if (markup != null) builder.replyMarkup(markup);
        return builder.build();
    }

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

    private InlineKeyboardMarkup confirmKeyboard(String name, Lang lang) {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(new InlineKeyboardRow(
                        InlineKeyboardButton.builder().text(Messages.get(lang, Messages.CONFIRM_YES)).callbackData(CONFIRM_PREFIX + name).build(),
                        InlineKeyboardButton.builder().text(Messages.get(lang, Messages.CONFIRM_NO)).callbackData(CANCEL_REMOVE).build()
                )))
                .build();
    }

    private String handleConfirmRemove(long userId, String name, Lang lang) {
        if (!friendService.friendExists(userId, name)) {
            return Messages.get(lang, Messages.FRIEND_NOT_FOUND, name);
        }
        friendService.deleteFriend(userId, name);
        userStateService.clearState(userId);
        log.debug("callback.friend.removed: userId={}, name={}", userId, name);
        return Messages.get(lang, Messages.FRIEND_REMOVED, name);
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
        LocalDate today = LocalDate.now();
        filtered.forEach(f -> {
            LocalDate next = f.nextBirthday(today);
            boolean isFuture = !next.isBefore(today);
            String ageLabel = isFuture
                    ? Messages.get(lang, Messages.YEARS_TURNS, f.getNextAge())
                    : Messages.get(lang, Messages.YEARS_OLD, f.getAge());
            sb.append("– <b>").append(f.getBirthDate().format(MessageBuilder.DATE_FORMATTER))
                    .append("</b> ").append(f.getName())
                    .append(" (<i>").append(ageLabel).append("</i>)\n");
        });
        return sb.toString();
    }

    private EditMessageText toEdit(SendMessage msg, int messageId) {
        return EditMessageText.builder()
                .chatId(msg.getChatId())
                .messageId(messageId)
                .parseMode("HTML")
                .text(msg.getText())
                .build();
    }
}
