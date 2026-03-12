package com.festiva.notification;

import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.user.UserPreference;
import com.festiva.user.UserPreferenceRepository;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class BirthdayReminder {

    @Value("${telegram.bot.username}")
    private String botUsername;

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
        try {
            checkBirthdaysForHour(ZonedDateTime.now(ZoneId.of("UTC")));
        } catch (Exception e) {
            log.warn("reminder.startup.check.failed", e);
        }
    }

    @Scheduled(cron = "0 0 * * * *", zone = "UTC")
    public void checkBirthdays() {
        checkBirthdaysForHour(ZonedDateTime.now(ZoneId.of("UTC")));
    }

    void checkBirthdaysForHour(ZonedDateTime utcNow) {
        log.info("reminder.check.start: utcHour={}", utcNow.getHour());
        List<Long> userIds = friendService.getAllUserIds();

        Map<Long, UserPreference> prefByUser = userPreferenceRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserPreference::getTelegramUserId, p -> p));
        Map<Long, List<Friend>> friendsByUser = friendService.getFriendsByUserIds(userIds);

        AtomicInteger notifiedCount = new AtomicInteger();
        userIds.forEach(userId -> {
            MDC.put("userId", String.valueOf(userId));
            try {
                notifiedCount.addAndGet(processUser(userId, prefByUser.get(userId),
                        friendsByUser.getOrDefault(userId, List.of()), utcNow));
            } finally {
                MDC.remove("userId");
            }
        });
        log.info("reminder.check.done: userCount={}, notifiedCount={}", userIds.size(), notifiedCount.get());
    }

    private int processUser(long userId, UserPreference pref, List<Friend> friends, ZonedDateTime utcNow) {
        ZoneId zone = resolveZone(pref);
        if (zone == null) return 0;
        ZonedDateTime userNow = utcNow.withZoneSameInstant(zone);
        if (!shouldNotify(pref, userNow)) return 0;
        LocalDate today = userNow.toLocalDate();
        Lang lang = pref != null && pref.getLang() != null ? pref.getLang() : Lang.RU;

        int count = (int) friends.stream().filter(f -> checkAndNotify(userId, f, today, lang)).count();
        if (count > 0 || !friends.isEmpty()) {
            UserPreference p = pref != null ? pref : new UserPreference();
            p.setTelegramUserId(userId);
            p.setLastNotifiedDate(today);
            userPreferenceRepository.save(p);
        }
        return count;
    }

    private ZoneId resolveZone(UserPreference pref) {
        String tz = pref != null && pref.getTimezone() != null ? pref.getTimezone() : "UTC";
        try {
            return ZoneId.of(tz);
        } catch (java.time.zone.ZoneRulesException e) {
            log.warn("reminder.timezone.invalid: tz={}", tz, e);
            return null;
        }
    }

    private boolean shouldNotify(UserPreference pref, ZonedDateTime userNow) {
        int notifyHour = pref != null && pref.getNotifyHour() >= 0 ? pref.getNotifyHour() : 9;
        if (notifyHour != userNow.getHour()) return false;
        LocalDate today = userNow.toLocalDate();
        return !today.equals(pref != null ? pref.getLastNotifiedDate() : null);
    }

    private boolean checkAndNotify(long userId, Friend friend, LocalDate today, Lang lang) {
        if (!friend.isNotifyEnabled()) return false;
        long daysUntil = ChronoUnit.DAYS.between(today, friend.nextBirthday(today));
        
        // Select appropriate template based on whether year is known
        Map<Long, String> templates = friend.hasYear() ? TEMPLATE_KEYS : TEMPLATE_KEYS_NO_YEAR;
        String key = templates.get(daysUntil);
        if (key == null) return false;
        
        try {
            String message;
            if (friend.hasYear()) {
                // With year: 5 parameters (name, relationship, zodiac, age, botUsername)
                message = Messages.get(lang, key,
                        friend.getName(),
                        friend.getRelationship() != null ? " " + friend.getRelationship().label(lang) : "",
                        friend.getZodiac(),
                        Messages.yearsRu(lang, friend.getNextAge(today)),
                        botUsername);
            } else {
                // Without year: 4 parameters (name, relationship, zodiac, botUsername)
                message = Messages.get(lang, key,
                        friend.getName(),
                        friend.getRelationship() != null ? " " + friend.getRelationship().label(lang) : "",
                        friend.getZodiac(),
                        botUsername);
            }
            
            notificationSender.send(userId, message);
            log.debug("reminder.notify.sent: userId={}, friend={}, daysUntil={}, hasYear={}", 
                    userId, friend.getName(), daysUntil, friend.hasYear());
            return true;
        } catch (RuntimeException e) {
            log.error("reminder.notify.failed: userId={}, friend={}, message={}", 
                    userId, friend.getName(), e.getMessage(), e);
            return false;
        }
    }
}
