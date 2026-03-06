package com.festiva.notification;

import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.user.UserPreference;
import com.festiva.user.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
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

    private static final Map<Long, String> TEMPLATE_KEYS = Map.of(
            0L, Messages.NOTIFY_TODAY,
            1L, Messages.NOTIFY_TOMORROW,
            7L, Messages.NOTIFY_WEEK
    );

    private final FriendService friendService;
    private final NotificationSender notificationSender;
    private final UserPreferenceRepository userPreferenceRepository;

    @Scheduled(cron = "0 0 * * * *")
    public void checkBirthdays() {
        checkBirthdaysForHour(java.time.LocalTime.now().getHour());
    }

    void checkBirthdaysForHour(int currentHour) {
        LocalDate today = LocalDate.now();
        log.info("reminder.check.start: date={}, hour={}", today, currentHour);
        List<Long> userIds = friendService.getAllUserIds();

        Map<Long, UserPreference> prefByUser = userPreferenceRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserPreference::getTelegramUserId, p -> p));

        int[] notifiedCount = {0};
        userIds.forEach(userId -> {
            UserPreference pref = prefByUser.get(userId);
            int notifyHour = pref != null ? pref.getNotifyHour() : 9;
            if (notifyHour != currentHour) return;
            Lang lang = pref != null ? pref.getLang() : Lang.RU;
            friendService.getFriends(userId).forEach(f -> {
                if (checkAndNotify(userId, f, today, lang)) notifiedCount[0]++;
            });
        });
        log.info("reminder.check.done: userCount={}, notifiedCount={}", userIds.size(), notifiedCount[0]);
    }

    private boolean checkAndNotify(long userId, Friend friend, LocalDate today, Lang lang) {
        long daysUntil = ChronoUnit.DAYS.between(today, friend.nextBirthday(today));
        String key = TEMPLATE_KEYS.get(daysUntil);
        if (key == null) return false;
        try {
            notificationSender.send(userId, Messages.get(lang, key, friend.getName(), friend.getZodiac(), friend.getNextAge(), botUsername));
            log.debug("reminder.notify.sent: userId={}, friend={}, daysUntil={}", userId, friend.getName(), daysUntil);
            return true;
        } catch (RuntimeException e) {
            log.error("reminder.notify.failed: userId={}, friend={}, message={}", userId, friend.getName(), e.getMessage(), e);
            return false;
        }
    }
}
