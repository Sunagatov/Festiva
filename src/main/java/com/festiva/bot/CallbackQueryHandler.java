package com.festiva.bot;

import com.festiva.command.DatePickerKeyboard;
import com.festiva.command.MessageBuilder;
import com.festiva.command.handler.BulkAddCommandHandler;
import com.festiva.command.handler.DeleteAccountCommandHandler;
import com.festiva.command.handler.EditFriendCommandHandler;
import com.festiva.command.handler.ImportIcsCommandHandler;
import com.festiva.command.handler.ListCommandHandler;
import com.festiva.command.handler.RemoveCommandHandler;
import com.festiva.command.handler.SettingsCommandHandler;
import com.festiva.command.handler.UpcomingBirthdaysCommandHandler;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.state.BotState;
import com.festiva.state.UserStateService;
import com.festiva.util.UserDateService;
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
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class CallbackQueryHandler {

    public static final String ACTION_ADD   = "ACTION_ADD";
    public static final String ACTION_ABOUT = "ACTION_ABOUT";

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
    private final BotCommandsService commandsService;
    private final UserDateService userDateService;

    public EditMessageText handle(CallbackQuery callbackQuery) {
        if (callbackQuery == null) {
            return null;
        }

        String data = callbackQuery.getData();
        MaybeInaccessibleMessage message = callbackQuery.getMessage();
        if (data == null || message == null) {
            return null;
        }

        long chatId = message.getChatId();
        int messageId = message.getMessageId();
        long userId = callbackQuery.getFrom().getId();
        Lang lang = userStateService.getLanguage(userId);

        CallbackResult result = dispatch(data, chatId, userId, lang);
        if (result == null) {
            return null;
        }
        if (result.sendMessage != null) {
            return toEdit(result.sendMessage, messageId);
        }

        EditMessageText.EditMessageTextBuilder<?, ?> builder = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .parseMode("HTML")
                .text(result.text != null ? result.text : "");
        if (result.markup != null) {
            builder.replyMarkup(result.markup);
        }
        return builder.build();
    }

    private CallbackResult dispatch(String data, long chatId, long userId, Lang lang) {
        CallbackResult r;
        if ((r = dispatchDatePicker(data, userId, lang)) != null) {
            return r;
        }
        if ((r = dispatchEdit(data, userId, lang)) != null) {
            return r;
        }
        if ((r = dispatchRemove(data, userId, lang)) != null) {
            return r;
        }
        if ((r = dispatchMisc(data, chatId, userId, lang)) != null) {
            return r;
        }

        log.atWarn()
                .setMessage("callback_unknown")
                .addKeyValue("userId", userId)
                .addKeyValue("data", data)
                .log();
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

    private CallbackResult dispatchMisc(String data, long chatId, long userId, Lang lang) {
        if (data.startsWith(SettingsCommandHandler.SETTINGS_HOUR_PREFIX)) {
            return handleSettingsHour(data, userId, lang);
        }
        if (data.startsWith(SettingsCommandHandler.SETTINGS_TZ_REGION_PREFIX)) {
            return handleSettingsTzRegion(data, userId, lang);
        }
        if (data.startsWith(SettingsCommandHandler.SETTINGS_TZ_PREFIX)) {
            return handleSettingsTz(data, userId, lang);
        }
        if (data.startsWith(UpcomingBirthdaysCommandHandler.UPCOMING_DAYS_PREFIX)) {
            return handleUpcoming(data, userId, lang);
        }
        if (data.startsWith(ListCommandHandler.LIST_PAGE_PREFIX)) {
            return handleListPage(data, userId, lang);
        }
        if (data.startsWith(LIST_SORT_DATE) || data.startsWith(LIST_SORT_NAME)) {
            return handleListSort(data, userId, lang);
        }
        if (data.startsWith(LANG_PREFIX)) {
            return handleLanguage(userId, data.substring(LANG_PREFIX.length()));
        }
        if (data.startsWith(MONTH_PREFIX)) {
            return handleMonth(userId, data, lang);
        }

        switch (data) {
            case ACTION_ADD -> {
                return handleActionAdd(userId, lang);
            }
            case ACTION_ABOUT -> {
                return new CallbackResult(Messages.get(lang, Messages.ABOUT), null);
            }
            case BulkAddCommandHandler.CALLBACK_PASTE -> {
                return new CallbackResult(bulkAddHandler.promptPaste(chatId, userId, lang));
            }
            case BulkAddCommandHandler.CALLBACK_CSV -> {
                bulkAddHandler.sendCsvTemplate(chatId, lang);
                return new CallbackResult(bulkAddHandler.promptPaste(chatId, userId, lang));
            }
            case BulkAddCommandHandler.CALLBACK_ICS -> {
                userStateService.setState(userId, BotState.WAITING_FOR_ICS_FILE);
                return new CallbackResult(Messages.get(lang, Messages.ICS_PROMPT), null);
            }
            case DeleteAccountCommandHandler.CONFIRM_DELETE -> {
                return handleConfirmDeleteAccount(userId, lang);
            }
            case DeleteAccountCommandHandler.CANCEL_DELETE -> {
                return new CallbackResult(Messages.get(lang, Messages.DELETE_ACCOUNT_CANCEL), null);
            }
            case ImportIcsCommandHandler.CALLBACK_ICS_CONFIRM -> {
                return handleIcsConfirm(userId, lang);
            }
            case ImportIcsCommandHandler.CALLBACK_ICS_CANCEL -> {
                userStateService.clearState(userId);
                return new CallbackResult(Messages.get(lang, Messages.ICS_CANCELLED), null);
            }
            default -> {
                return null;
            }
        }
    }

    // ── Settings ─────────────────────────────────────────────────────────────

    private CallbackResult handleSettingsHour(String data, long userId, Lang lang) {
        try {
            int hour = Integer.parseInt(data.substring(SettingsCommandHandler.SETTINGS_HOUR_PREFIX.length()));
            if (hour < 0 || hour > 23) {
                log.atDebug()
                        .setMessage("callback_settings_hour_invalid")
                        .addKeyValue("userId", userId)
                        .addKeyValue("hour", hour)
                        .log();
                return sessionExpired(lang);
            }
            userStateService.setNotifyHour(userId, hour);
            return new CallbackResult(Messages.get(lang, Messages.SETTINGS_HOUR_SET, hour),
                    SettingsCommandHandler.combined(hour, userStateService.getTimezone(userId), lang,
                            SettingsCommandHandler.regionForTimezone(userStateService.getTimezone(userId))));
        } catch (NumberFormatException e) {
            log.atDebug()
                    .setMessage("callback_settings_hour_parse_failed")
                    .addKeyValue("data", data)
                    .log();
            return sessionExpired(lang);
        }
    }

    private CallbackResult handleSettingsTzRegion(String data, long userId, Lang lang) {
        String region = data.substring(SettingsCommandHandler.SETTINGS_TZ_REGION_PREFIX.length());
        if (SettingsCommandHandler.tzKeyboard(region, userStateService.getTimezone(userId)).getKeyboard().isEmpty()) {
            log.atDebug()
                    .setMessage("callback_settings_timezone_region_invalid")
                    .addKeyValue("userId", userId)
                    .addKeyValue("region", region)
                    .log();
            return sessionExpired(lang);
        }
        return new CallbackResult(Messages.get(lang, Messages.SETTINGS_HEADER) + "\n\n" +
                Messages.get(lang, Messages.SETTINGS_TZ_HEADER),
                SettingsCommandHandler.combined(userStateService.getNotifyHour(userId),
                        userStateService.getTimezone(userId), lang, region));
    }

    private CallbackResult handleSettingsTz(String data, long userId, Lang lang) {
        String tz = data.substring(SettingsCommandHandler.SETTINGS_TZ_PREFIX.length());
        try {
            @SuppressWarnings("unused")
            java.time.ZoneId validatedZone = java.time.ZoneId.of(tz);
        } catch (java.time.zone.ZoneRulesException e) {
            log.atDebug()
                    .setMessage("callback_settings_timezone_invalid")
                    .addKeyValue("userId", userId)
                    .addKeyValue("timezone", tz)
                    .log();
            return sessionExpired(lang);
        }
        userStateService.setTimezone(userId, tz);
        return new CallbackResult(Messages.get(lang, Messages.SETTINGS_TZ_SET, tz),
                SettingsCommandHandler.combined(userStateService.getNotifyHour(userId), tz, lang,
                        SettingsCommandHandler.regionForTimezone(tz)));
    }

    // ── List ─────────────────────────────────────────────────────────────────

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

    // ── Upcoming ─────────────────────────────────────────────────────────────

    private CallbackResult handleUpcoming(String data, long userId, Lang lang) {
        try {
            int days = Integer.parseInt(data.substring(UpcomingBirthdaysCommandHandler.UPCOMING_DAYS_PREFIX.length()));
            if (!Set.of(7, 14, 30).contains(days)) {
                log.atDebug()
                        .setMessage("callback_upcoming_days_invalid")
                        .addKeyValue("userId", userId)
                        .addKeyValue("days", days)
                        .log();
                return sessionExpired(lang);
            }

            var friends = friendService.getFriends(userId);
            return new CallbackResult(upcomingHandler.buildText(friends, lang, days, userId),
                    upcomingHandler.filterKeyboard(lang, days));
        } catch (NumberFormatException e) {
            log.atDebug()
                    .setMessage("callback_upcoming_days_parse_failed")
                    .addKeyValue("data", data)
                    .log();
            return sessionExpired(lang);
        }
    }

    // ── Add / Language ────────────────────────────────────────────────────────

    private CallbackResult handleActionAdd(long userId, Lang lang) {
        userStateService.setState(userId, com.festiva.state.BotState.WAITING_FOR_ADD_FRIEND_NAME);
        return new CallbackResult(Messages.get(lang, Messages.ENTER_NAME), null);
    }

    private CallbackResult handleLanguage(long userId, String code) {
        try {
            Lang newLang = Lang.valueOf(code);
            userStateService.setLanguage(userId, newLang);
            userStateService.clearState(userId);

            commandsService.updateCommandsForUser(userId, newLang);

            InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                    .keyboard(List.of(new InlineKeyboardRow(
                            InlineKeyboardButton.builder().text((newLang == Lang.EN ? "✅ " : "") + Messages.get(newLang, Messages.LANG_EN_BTN)).callbackData(LANG_PREFIX + Lang.EN.name()).build(),
                            InlineKeyboardButton.builder().text((newLang == Lang.RU ? "✅ " : "") + Messages.get(newLang, Messages.LANG_RU_BTN)).callbackData(LANG_PREFIX + Lang.RU.name()).build()
                    )))
                    .build();
            return new CallbackResult(Messages.get(newLang, Messages.LANGUAGE_SET), keyboard);
        } catch (IllegalArgumentException e) {
            log.atDebug()
                    .setMessage("callback_language_unknown")
                    .addKeyValue("code", code)
                    .log();
            return sessionExpired(userStateService.getLanguage(userId));
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
        userStateService.setPendingName(userId, name);
        userStateService.setPendingId(userId, id);
        userStateService.setState(userId, com.festiva.state.BotState.WAITING_FOR_REMOVE_CONFIRM);
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

    private CallbackResult handleConfirmDeleteAccount(long userId, Lang lang) {
        deleteAccountHandler.deleteAccount(userId);
        return new CallbackResult(Messages.get(lang, Messages.DELETE_ACCOUNT_DONE), null);
    }

    private CallbackResult handleIcsConfirm(long userId, Lang lang) {
        List<com.festiva.friend.entity.Friend> pending = userStateService.getPendingIcsImport(userId);
        if (pending == null || pending.isEmpty()) {
            userStateService.clearState(userId);
            return sessionExpired(lang);
        }

        List<Friend> currentFriends = friendService.getFriends(userId);
        Set<String> existingNames = currentFriends.stream()
                .map(friend -> Friend.normalizeName(friend.getName()))
                .collect(java.util.stream.Collectors.toSet());

        int currentCount = currentFriends.size();
        int saved = 0;
        int duplicateCount = 0;
        int rejectedCount = 0;
        int failureCount = 0;

        for (com.festiva.friend.entity.Friend friend : pending) {
            String normalizedName = Friend.normalizeName(friend.getName());
            if (normalizedName.isBlank() || existingNames.contains(normalizedName)) {
                duplicateCount++;
                continue;
            }

            if (currentCount + saved >= FriendService.FRIEND_CAP) {
                break;
            }

            try {
                friendService.addFriend(userId, friend);
                existingNames.add(normalizedName);
                saved++;
            } catch (IllegalArgumentException e) {
                rejectedCount++;
            } catch (Exception e) {
                failureCount++;
                log.atWarn()
                        .setMessage("ics_import_save_failed")
                        .addKeyValue("userId", userId)
                        .setCause(e)
                        .log();
            }
        }

        userStateService.clearState(userId);

        log.atInfo()
                .setMessage("ics_import_completed")
                .addKeyValue("userId", userId)
                .addKeyValue("pendingCount", pending.size())
                .addKeyValue("savedCount", saved)
                .addKeyValue("duplicateCount", duplicateCount)
                .addKeyValue("rejectedCount", rejectedCount)
                .addKeyValue("failureCount", failureCount)
                .addKeyValue("capReached", currentCount + saved >= FriendService.FRIEND_CAP)
                .log();

        String message = saved > 0
                ? Messages.get(lang, Messages.ICS_DONE, saved)
                : Messages.get(lang, Messages.ICS_NONE_SAVED);
        return new CallbackResult(message, null);
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

    // ── Keyboards ─────────────────────────────────────────────────────────────

    private InlineKeyboardMarkup confirmKeyboard(String id, Lang lang) {
        return InlineKeyboardMarkup.builder().keyboard(List.of(new InlineKeyboardRow(
                InlineKeyboardButton.builder().text(Messages.get(lang, Messages.CONFIRM_YES)).callbackData(CONFIRM_PREFIX + id).build(),
                InlineKeyboardButton.builder().text(Messages.get(lang, Messages.CONFIRM_NO)).callbackData(CANCEL_REMOVE).build()
        ))).build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private EditMessageText toEdit(SendMessage msg, int messageId) {
        EditMessageText.EditMessageTextBuilder<?, ?> builder = EditMessageText.builder()
                .chatId(msg.getChatId())
                .messageId(messageId)
                .parseMode("HTML")
                .text(msg.getText());

        if (msg.getReplyMarkup() instanceof InlineKeyboardMarkup inlineKeyboardMarkup) {
            builder.replyMarkup(inlineKeyboardMarkup);
        }

        return builder.build();
    }

    private CallbackResult sessionExpired(Lang lang) {
        return new CallbackResult(Messages.get(lang, Messages.SESSION_EXPIRED), null);
    }
}
