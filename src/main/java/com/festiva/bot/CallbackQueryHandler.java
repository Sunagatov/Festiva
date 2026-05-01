package com.festiva.bot;

import com.festiva.command.MessageBuilder;
import com.festiva.command.handler.BulkAddCommandHandler;
import com.festiva.command.handler.ImportIcsCommandHandler;
import com.festiva.command.handler.SettingsCommandHandler;
import com.festiva.command.handler.UpcomingBirthdaysCommandHandler;
import com.festiva.friend.api.FriendAction;
import com.festiva.friend.api.FriendCallbackService;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.importing.PendingIcsImportService;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.state.BotState;
import com.festiva.state.UserStateService;
import com.festiva.user.api.AccountDeletionAction;
import com.festiva.user.api.AccountDeletionService;
import com.festiva.user.api.UserLanguageCallbackService;
import com.festiva.user.api.UserPreferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.MaybeInaccessibleMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class CallbackQueryHandler {

    public static final String ACTION_ABOUT = "ACTION_ABOUT";

    private final FriendService friendService;
    private final UserStateService userStateService;
    private final UpcomingBirthdaysCommandHandler upcomingHandler;
    private final BulkAddCommandHandler bulkAddHandler;
    private final FriendCallbackService friendCallbackService;
    private final MoreCallbackHandler moreCallbackHandler;
    private final AccountDeletionService accountDeletionService;
    private final UserLanguageCallbackService userLanguageCallbackService;
    private final UserPreferenceService userPreferenceService;
    private final PendingIcsImportService pendingIcsImportService;

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
        Lang lang = languageOf(userId);

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
        if ((r = friendCallbackService.handle(data, userId, lang)) != null) {
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

    private CallbackResult dispatchMisc(String data, long chatId, long userId, Lang lang) {
        CallbackResult r;
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
        if ((r = moreCallbackHandler.handle(data, chatId, userId, lang)) != null) {
            return r;
        }
        if ((r = userLanguageCallbackService.handle(data, userId)) != null) {
            return r;
        }

        switch (data) {
            case FriendAction.ACTION_ADD -> {
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
                return new CallbackResult(Messages.get(lang, Messages.ICS_PROMPT), MessageBuilder.backToMoreMarkup(lang));
            }
            case AccountDeletionAction.CONFIRM_DELETE -> {
                return handleConfirmDeleteAccount(userId, lang);
            }
            case AccountDeletionAction.CANCEL_DELETE -> {
                return new CallbackResult(Messages.get(lang, Messages.DELETE_ACCOUNT_CANCEL), MessageBuilder.settingsButtonMarkup(lang));
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
            userPreferenceService.setNotifyHour(userId, hour);
            return new CallbackResult(Messages.get(lang, Messages.SETTINGS_HOUR_SET, hour),
                    SettingsCommandHandler.combined(hour, timezoneOf(userId), lang,
                            SettingsCommandHandler.regionForTimezone(timezoneOf(userId))));
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
        if (SettingsCommandHandler.tzKeyboard(region, timezoneOf(userId)).getKeyboard().isEmpty()) {
            log.atDebug()
                    .setMessage("callback_settings_timezone_region_invalid")
                    .addKeyValue("userId", userId)
                    .addKeyValue("region", region)
                    .log();
            return sessionExpired(lang);
        }
        return new CallbackResult(Messages.get(lang, Messages.SETTINGS_HEADER) + "\n\n" +
                Messages.get(lang, Messages.SETTINGS_TZ_HEADER),
                SettingsCommandHandler.combined(notifyHourOf(userId),
                        timezoneOf(userId), lang, region));
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
        userPreferenceService.setTimezone(userId, tz);
        return new CallbackResult(Messages.get(lang, Messages.SETTINGS_TZ_SET, tz),
                SettingsCommandHandler.combined(notifyHourOf(userId), tz, lang,
                        SettingsCommandHandler.regionForTimezone(tz)));
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

    private CallbackResult handleConfirmDeleteAccount(long userId, Lang lang) {
        accountDeletionService.deleteAccount(userId);
        return new CallbackResult(Messages.get(lang, Messages.DELETE_ACCOUNT_DONE), null);
    }

    private CallbackResult handleIcsConfirm(long userId, Lang lang) {
        List<com.festiva.friend.entity.Friend> pending = pendingIcsImportService != null
                ? pendingIcsImportService.get(userId)
                : userStateService.getPendingIcsImport(userId);
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

    private Lang languageOf(long userId) {
        return userPreferenceService.getLanguage(userId);
    }

    private int notifyHourOf(long userId) {
        return userPreferenceService.getNotifyHour(userId);
    }

    private String timezoneOf(long userId) {
        return userPreferenceService.getTimezone(userId);
    }
}
