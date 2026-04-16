package com.festiva.notification;

import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.user.UserPreference;
import com.festiva.user.UserPreferenceRepository;
import com.festiva.util.HtmlEscaper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
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
    private final UserPreferenceRepository userPreferenceRepository;

    @PostConstruct
    public void checkBirthdaysOnStartup() {
        if (!startupCheckEnabled) {
            return;
        }

        try {
            checkBirthdaysForHour(ZonedDateTime.now(ZoneId.of("UTC")));
        } catch (Exception e) {
            log.warn("reminder.startup.check.failed", e);
        }
    }

    @Scheduled(cron = "0 0 * * * *", zone = "UTC")
    public void checkBirthdays() {
        if (!scheduleEnabled) {
            return;
        }

        checkBirthdaysForHour(ZonedDateTime.now(ZoneId.of("UTC")));
    }

    void checkBirthdaysForHour(ZonedDateTime utcNow) {
        List<Long> userIds = friendService.getAllUserIds();

        Map<Long, UserPreference> prefByUser = userPreferenceRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserPreference::getTelegramUserId, p -> p));
        Map<Long, List<Friend>> friendsByUser = friendService.getFriendsByUserIds(userIds);

        userIds.forEach(userId -> {
            MDC.put("userId", String.valueOf(userId));
            try {
                processUser(userId, prefByUser.get(userId), friendsByUser.getOrDefault(userId, List.of()), utcNow);
            } finally {
                MDC.remove("userId");
            }
        });
    }

    private void processUser(long userId, UserPreference pref, List<Friend> friends, ZonedDateTime utcNow) {
        ZoneId zone = resolveZone(pref);
        if (zone == null) {
            return;
        }

        ZonedDateTime userNow = utcNow.withZoneSameInstant(zone);
        if (!shouldNotify(pref, userNow)) {
            return;
        }

        LocalDate today = userNow.toLocalDate();
        Lang lang = pref != null && pref.getLang() != null ? pref.getLang() : UserPreference.DEFAULT_LANG;

        int count = (int) friends.stream().filter(f -> checkAndNotify(userId, f, today, lang)).count();
        if (count > 0 || !friends.isEmpty()) {
            UserPreference p = pref != null ? pref : new UserPreference();
            p.setTelegramUserId(userId);
            p.setLastNotifiedDate(today);
            userPreferenceRepository.save(p);
        }
    }

    private ZoneId resolveZone(UserPreference pref) {
        String tz = pref != null && pref.getTimezone() != null ? pref.getTimezone() : UserPreference.DEFAULT_TIMEZONE;
        try {
            return ZoneId.of(tz);
        } catch (java.time.zone.ZoneRulesException e) {
            log.warn("reminder.timezone.invalid: tz={}", tz, e);
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

    private boolean checkAndNotify(long userId, Friend friend, LocalDate today, Lang lang) {
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

            notificationSender.send(userId, message);
            return true;
        } catch (RuntimeException e) {
            log.error("reminder.notify.failed: userId={}, friendId={}, daysUntil={}",
                    userId, friend.getId(), daysUntil, e);
            return false;
        }
    }
}