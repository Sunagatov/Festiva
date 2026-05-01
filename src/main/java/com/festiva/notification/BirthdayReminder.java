package com.festiva.notification;

import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.user.UserPreference;
import com.festiva.user.api.UserPreferenceService;
import com.festiva.util.HtmlEscaper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class BirthdayReminder {

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${festiva.reminder.startup-check.enabled:true}")
    private boolean startupCheckEnabled;

    @Value("${festiva.reminder.schedule.enabled:true}")
    private boolean scheduleEnabled;

    private static final Map<Long, String> TEMPLATE_KEYS = Map.of(
            0L, Messages.NOTIFY_TODAY,
            1L, Messages.NOTIFY_TOMORROW,
            7L, Messages.NOTIFY_WEEK
    );

    private static final Map<Long, String> TEMPLATE_KEYS_NO_YEAR = Map.of(
            0L, Messages.NOTIFY_TODAY_NO_YEAR,
            1L, Messages.NOTIFY_TOMORROW_NO_YEAR,
            7L, Messages.NOTIFY_WEEK_NO_YEAR
    );

    private final FriendService friendService;
    private final NotificationSender notificationSender;
    private final UserPreferenceService userPreferenceService;

    @PostConstruct
    public void checkBirthdaysOnStartup() {
        if (!startupCheckEnabled) {
            return;
        }

        try {
            checkBirthdaysForHour(ZonedDateTime.now(ZoneId.of("UTC")), "startup");
        } catch (Exception e) {
            log.atWarn()
                    .setMessage("birthday_reminder_startup_check_failed")
                    .setCause(e)
                    .log();
        }
    }

    @Scheduled(cron = "0 0 * * * *", zone = "UTC")
    public void checkBirthdays() {
        if (!scheduleEnabled) {
            return;
        }

        checkBirthdaysForHour(ZonedDateTime.now(ZoneId.of("UTC")), "scheduled");
    }

    void checkBirthdaysForHour(ZonedDateTime utcNow) {
        checkBirthdaysForHour(utcNow, "manual");
    }

    void checkBirthdaysForHour(ZonedDateTime utcNow, String trigger) {
        Instant startedAt = Instant.now();
        List<Long> userIds = friendService.getAllUserIds();
        ReminderRunStats stats = new ReminderRunStats();
        stats.scannedUsers = userIds.size();

        Map<Long, UserPreference> prefByUser = userPreferenceService.findByUserIds(userIds);
        Map<Long, List<Friend>> friendsByUser = friendService.getFriendsByUserIds(userIds);

        userIds.forEach(userId -> {
            MDC.put("userId", String.valueOf(userId));
            try {
                processUser(userId, prefByUser.get(userId), friendsByUser.getOrDefault(userId, List.of()), utcNow, stats);
            } finally {
                MDC.remove("userId");
            }
        });

        log.atInfo()
                .setMessage("birthday_reminder_check_completed")
                .addKeyValue("trigger", trigger)
                .addKeyValue("scannedUsers", stats.scannedUsers)
                .addKeyValue("processedUsers", stats.processedUsers)
                .addKeyValue("notifiedUsers", stats.notifiedUsers)
                .addKeyValue("notificationsSent", stats.notificationsSent)
                .addKeyValue("notificationFailures", stats.notificationFailures)
                .addKeyValue("invalidTimezones", stats.invalidTimezones)
                .addKeyValue("durationMs", ChronoUnit.MILLIS.between(startedAt, Instant.now()))
                .log();
    }

    private void processUser(long userId, UserPreference pref, List<Friend> friends, ZonedDateTime utcNow, ReminderRunStats stats) {
        ZoneId zone = resolveZone(userId, pref, stats);
        if (zone == null) {
            return;
        }

        ZonedDateTime userNow = utcNow.withZoneSameInstant(zone);
        if (!shouldNotify(pref, userNow)) {
            return;
        }
        stats.processedUsers++;

        LocalDate today = userNow.toLocalDate();
        Lang lang = pref != null && pref.getLang() != null ? pref.getLang() : UserPreference.DEFAULT_LANG;

        int count = (int) friends.stream().filter(f -> checkAndNotify(userId, f, today, lang, stats)).count();
        if (count > 0) {
            userPreferenceService.markLastNotifiedDate(userId, today);
            stats.notifiedUsers++;
        }
    }

    private ZoneId resolveZone(long userId, UserPreference pref, ReminderRunStats stats) {
        String tz = pref != null && pref.getTimezone() != null ? pref.getTimezone() : UserPreference.DEFAULT_TIMEZONE;
        try {
            return ZoneId.of(tz);
        } catch (java.time.zone.ZoneRulesException e) {
            stats.invalidTimezones++;
            log.atWarn()
                    .setMessage("birthday_reminder_timezone_invalid")
                    .addKeyValue("userId", userId)
                    .addKeyValue("timezone", tz)
                    .setCause(e)
                    .log();
            return null;
        }
    }

    private boolean shouldNotify(UserPreference pref, ZonedDateTime userNow) {
        int notifyHour = pref != null && pref.getNotifyHour() >= 0 && pref.getNotifyHour() <= 23 ? pref.getNotifyHour() : 9;
        if (notifyHour != userNow.getHour()) {
            return false;
        }
        LocalDate today = userNow.toLocalDate();
        return !today.equals(pref != null ? pref.getLastNotifiedDate() : null);
    }

    private boolean checkAndNotify(long userId, Friend friend, LocalDate today, Lang lang, ReminderRunStats stats) {
        if (!friend.isNotifyEnabled()) {
            return false;
        }

        long daysUntil = ChronoUnit.DAYS.between(today, friend.nextBirthday(today));

        Map<Long, String> templates = friend.hasYear() ? TEMPLATE_KEYS : TEMPLATE_KEYS_NO_YEAR;
        String key = templates.get(daysUntil);
        if (key == null) {
            return false;
        }

        try {
            String message;
            if (friend.hasYear()) {
                message = Messages.get(lang, key,
                        HtmlEscaper.escape(friend.getName()),
                        friend.getRelationship() != null ? " " + friend.getRelationship().label(lang) : "",
                        friend.getZodiac(),
                        Messages.yearsRu(lang, friend.getNextAge(today)),
                        botUsername);
            } else {
                message = Messages.get(lang, key,
                        HtmlEscaper.escape(friend.getName()),
                        friend.getRelationship() != null ? " " + friend.getRelationship().label(lang) : "",
                        friend.getZodiac(),
                        botUsername);
            }

            boolean sent = notificationSender.send(userId, message);
            if (!sent) {
                stats.notificationFailures++;
                log.atError()
                        .setMessage("birthday_reminder_notification_failed")
                        .addKeyValue("userId", userId)
                        .addKeyValue("friendId", friend.getId())
                        .addKeyValue("daysUntil", daysUntil)
                        .log();
            } else {
                stats.notificationsSent++;
            }
            return sent;
        } catch (RuntimeException e) {
            stats.notificationFailures++;
            log.atError()
                    .setMessage("birthday_reminder_notification_failed")
                    .addKeyValue("userId", userId)
                    .addKeyValue("friendId", friend.getId())
                    .addKeyValue("daysUntil", daysUntil)
                    .setCause(e)
                    .log();
            return false;
        }
    }

    private static final class ReminderRunStats {
        private int scannedUsers;
        private int processedUsers;
        private int notifiedUsers;
        private int notificationsSent;
        private int notificationFailures;
        private int invalidTimezones;
    }
}
