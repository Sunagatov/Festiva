package com.festiva.notification;

import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.i18n.Lang;
import com.festiva.i18n.MessagesTestSupport;
import com.festiva.user.UserPreference;
import com.festiva.user.api.UserPreferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("BirthdayReminder unit behaviour")
@ExtendWith(MockitoExtension.class)
class BirthdayReminderUnitTest extends MessagesTestSupport {

    @Mock FriendService friendService;
    @Mock NotificationSender notificationSender;
    @Mock UserPreferenceService userPreferenceService;
    @InjectMocks BirthdayReminder reminder;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(reminder, "botUsername", "festiva_test_bot");
    }

    @Test
    @DisplayName("known-year Feb 29 birthday is reminded on Feb 28 in a non-leap year")
    void leapDayBirthday_nonLeapYear_sendsOnFebruary28() {
        long userId = 1L;
        LocalDate observedBirthday = LocalDate.of(2025, 2, 28);
        ZonedDateTime utcNow = observedBirthday.atTime(9, 0).atZone(ZoneId.of("UTC"));
        UserPreference preference = new UserPreference(userId, Lang.EN, 9, "UTC", null);
        Friend leapDayFriend = new Friend("Leap", LocalDate.of(2000, 2, 29));

        when(friendService.getAllUserIds()).thenReturn(List.of(userId));
        when(userPreferenceService.findByUserIds(List.of(userId))).thenReturn(Map.of(userId, preference));
        when(friendService.getFriendsByUserIds(List.of(userId))).thenReturn(Map.of(userId, List.of(leapDayFriend)));
        when(notificationSender.send(org.mockito.ArgumentMatchers.eq(userId),
                org.mockito.ArgumentMatchers.anyString())).thenReturn(true);

        reminder.checkBirthdaysForHour(utcNow);

        verify(notificationSender).send(org.mockito.ArgumentMatchers.eq(userId), contains("Leap"));
        verify(userPreferenceService).markLastNotifiedDate(userId, observedBirthday);
    }
}
