package com.festiva.notification;

import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
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

    private static final Map<Long, String> TEMPLATES = Map.of(
            0L, "Сегодня день рождения у вашего друга %s!",
            1L, "Завтра день рождения у вашего друга %s!",
            7L, "Через неделю день рождения у вашего друга %s!"
    );

    private final FriendService friendService;
    private final NotificationSender notificationSender;

    @Scheduled(cron = "0 0 9 * * *")
    public void checkBirthdays() {
        log.info("Starting birthday check");
        List<Long> userIds = friendService.getAllUserIds();
        userIds.forEach(userId -> {
            try {
                friendService.getFriends(userId).forEach(f -> checkAndNotify(userId, f));
            } catch (Exception e) {
                log.error("Error processing notifications for user: {}", userId, e);
            }
        });
        log.info("Birthday check completed for {} users", userIds.size());
    }

    private void checkAndNotify(long userId, Friend friend) {
        LocalDate today = LocalDate.now();
        long daysUntil = ChronoUnit.DAYS.between(today, FriendService.nextBirthday(friend.getBirthDate(), today));
        String template = TEMPLATES.get(daysUntil);
        if (template != null) {
            notificationSender.send(userId, String.format(template, friend.getName()));
        }
    }
}
