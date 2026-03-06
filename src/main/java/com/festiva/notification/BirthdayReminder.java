package com.festiva.notification;

import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.i18n.Messages;
import com.festiva.state.UserStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BirthdayReminder {

    private static final Map<Long, String> TEMPLATE_KEYS = Map.of(
            0L, Messages.NOTIFY_TODAY,
            1L, Messages.NOTIFY_TOMORROW,
            7L, Messages.NOTIFY_WEEK
    );

    private final FriendService friendService;
    private final NotificationSender notificationSender;
    private final UserStateService userStateService;

    @Scheduled(cron = "0 0 9 * * *")
    public void checkBirthdays() {
        LocalDate today = LocalDate.now();
        log.info("reminder.check.start: date={}", today);
        List<Long> userIds = friendService.getAllUserIds();
        int[] notifiedCount = {0};
        userIds.forEach(userId ->
                friendService.getFriends(userId).forEach(f -> {
                    if (checkAndNotify(userId, f, today)) notifiedCount[0]++;
                })
        );
        log.info("reminder.check.done: userCount={}, notifiedCount={}", userIds.size(), notifiedCount[0]);
    }

    private boolean checkAndNotify(long userId, Friend friend, LocalDate today) {
        long daysUntil = ChronoUnit.DAYS.between(today, friend.nextBirthday(today));
        String key = TEMPLATE_KEYS.get(daysUntil);
        if (key == null) return false;
        try {
            notificationSender.send(userId,
                    Messages.get(userStateService.getLanguage(userId), key, friend.getName()));
            log.debug("reminder.notify.sent: userId={}, friend={}, daysUntil={}", userId, friend.getName(), daysUntil);
            return true;
        } catch (RuntimeException e) {
            log.error("reminder.notify.failed: userId={}, friend={}, message={}", userId, friend.getName(), e.getMessage(), e);
            return false;
        }
    }
}
