package com.festiva.notification;

import com.festiva.IntegrationTestBase;
import com.festiva.bot.BirthdayBot;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.friend.repository.FriendMongoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("BirthdayReminder (integration)")
class BirthdayReminderTest extends IntegrationTestBase {

    @Autowired BirthdayReminder birthdayReminder;
    @Autowired FriendService friendService;
    @Autowired FriendMongoRepository friendMongoRepository;
    @Autowired BirthdayBot birthdayBot;

    @BeforeEach
    void clean() { friendMongoRepository.deleteAll(); }

    @Test
    @DisplayName("birthday today → notification contains friend's name")
    void todayBirthday_sendsNotificationWithName() {
        friendService.addFriend(10L, new Friend("Alice", LocalDate.now().minusYears(30)));
        birthdayReminder.checkBirthdays();
        verify(birthdayBot).send(eq(10L), contains("Alice"));
    }

    @Test
    @DisplayName("birthday tomorrow → notification contains friend's name")
    void tomorrowBirthday_sendsNotificationWithName() {
        friendService.addFriend(11L, new Friend("Bob", LocalDate.now().plusDays(1).minusYears(25)));
        birthdayReminder.checkBirthdays();
        verify(birthdayBot).send(eq(11L), contains("Bob"));
    }

    @Test
    @DisplayName("birthday in 7 days → notification contains friend's name")
    void weekBirthday_sendsNotificationWithName() {
        friendService.addFriend(12L, new Friend("Carol", LocalDate.now().plusDays(7).minusYears(20)));
        birthdayReminder.checkBirthdays();
        verify(birthdayBot).send(eq(12L), contains("Carol"));
    }

    @Test
    @DisplayName("birthday in 10 days → no notification sent")
    void irrelevantDate_noNotification() {
        friendService.addFriend(13L, new Friend("Dave", LocalDate.now().plusDays(10).minusYears(20)));
        birthdayReminder.checkBirthdays();
        verify(birthdayBot, never()).send(anyLong(), anyString());
    }
}
