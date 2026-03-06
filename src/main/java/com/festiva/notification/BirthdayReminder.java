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
        log.info("Starting birthday check");
        List<Long> userIds = friendService.getAllUserIds();
        userIds.forEach(userId ->
                friendService.getFriendsSortedByDayMonth(userId).forEach(f -> checkAndNotify(userId, f))
        );
        log.info("Birthday check completed for {} users", userIds.size());
    }

    private void checkAndNotify(long userId, Friend friend) {
        LocalDate today = LocalDate.now();
        long daysUntil = ChronoUnit.DAYS.between(today, friend.nextBirthday(today));
        String key = TEMPLATE_KEYS.get(daysUntil);
        if (key == null) return;
        try {
            notificationSender.send(userId,
                    Messages.get(userStateService.getLanguage(userId), key, friend.getName()));
        } catch (RuntimeException e) {
            log.error("Failed to send notification to userId={} for friend={}", userId, friend.getName(), e);
        }
    }
}
