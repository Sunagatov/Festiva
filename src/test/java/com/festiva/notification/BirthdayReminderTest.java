package com.festiva.notification;

import com.festiva.IntegrationTestBase;
import com.festiva.bot.BirthdayBot;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.friend.repository.FriendMongoRepository;
import com.festiva.i18n.Lang;
import com.festiva.user.UserPreference;
import com.festiva.user.UserPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("BirthdayReminder (integration)")
class BirthdayReminderTest extends IntegrationTestBase {

    private static final ZonedDateTime UTC_9 = ZonedDateTime.now(ZoneId.of("UTC")).withHour(9).withMinute(0).withSecond(0).withNano(0);

    @Autowired BirthdayReminder birthdayReminder;
    @Autowired FriendService friendService;
    @Autowired FriendMongoRepository friendMongoRepository;
    @Autowired UserPreferenceRepository userPreferenceRepository;
    @Autowired BirthdayBot birthdayBot;

    @BeforeEach
    void clean() {
        friendMongoRepository.deleteAll();
        userPreferenceRepository.deleteAll();
    }

    private void savePrefs(long userId) {
        userPreferenceRepository.save(new UserPreference(userId, Lang.EN, 9, "UTC", null));
    }

    @Test
    @DisplayName("birthday today → notification contains friend's name")
    void todayBirthday_sendsNotificationWithName() {
        savePrefs(10L);
        friendService.addFriend(10L, new Friend("Alice", LocalDate.now().minusYears(30)));
        birthdayReminder.checkBirthdaysForHour(UTC_9);
        verify(birthdayBot).send(eq(10L), contains("Alice"));
    }

    @Test
    @DisplayName("birthday tomorrow → notification contains friend's name")
    void tomorrowBirthday_sendsNotificationWithName() {
        savePrefs(11L);
        friendService.addFriend(11L, new Friend("Bob", LocalDate.now().plusDays(1).minusYears(25)));
        birthdayReminder.checkBirthdaysForHour(UTC_9);
        verify(birthdayBot).send(eq(11L), contains("Bob"));
    }

    @Test
    @DisplayName("birthday in 7 days → notification contains friend's name")
    void weekBirthday_sendsNotificationWithName() {
        savePrefs(12L);
        friendService.addFriend(12L, new Friend("Carol", LocalDate.now().plusDays(7).minusYears(20)));
        birthdayReminder.checkBirthdaysForHour(UTC_9);
        verify(birthdayBot).send(eq(12L), contains("Carol"));
    }

    @Test
    @DisplayName("birthday in 10 days → no notification sent")
    void irrelevantDate_noNotification() {
        savePrefs(13L);
        friendService.addFriend(13L, new Friend("Dave", LocalDate.now().plusDays(10).minusYears(20)));
        birthdayReminder.checkBirthdaysForHour(UTC_9);
        verify(birthdayBot, never()).send(anyLong(), anyString());
    }

    @Test
    @DisplayName("notification failure → does not propagate exception")
    void notificationFailure_doesNotPropagateException() {
        savePrefs(14L);
        friendService.addFriend(14L, new Friend("FailFriend", LocalDate.now().minusYears(25)));
        doThrow(new RuntimeException("send failed")).when(birthdayBot).send(eq(14L), anyString());
        assertThatCode(() -> birthdayReminder.checkBirthdaysForHour(UTC_9)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("wrong notify hour → no notification sent")
    void wrongHour_noNotification() {
        userPreferenceRepository.save(new UserPreference(15L, Lang.EN, 10, "UTC", null));
        friendService.addFriend(15L, new Friend("Eve", LocalDate.now().minusYears(30)));
        birthdayReminder.checkBirthdaysForHour(UTC_9);
        verify(birthdayBot, never()).send(eq(15L), anyString());
    }

    @Test
    @DisplayName("invalid timezone in prefs → skips user, no exception")
    void invalidTimezone_skipsUserSilently() {
        userPreferenceRepository.save(new UserPreference(16L, Lang.EN, 9, "Not/AZone", null));
        friendService.addFriend(16L, new Friend("Frank", LocalDate.now().minusYears(30)));
        assertThatCode(() -> birthdayReminder.checkBirthdaysForHour(UTC_9)).doesNotThrowAnyException();
        verify(birthdayBot, never()).send(eq(16L), anyString());
    }
}
