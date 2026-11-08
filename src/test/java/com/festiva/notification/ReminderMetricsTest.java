package com.festiva.notification;

import com.festiva.friend.api.FriendService;
import com.festiva.user.api.UserPreferenceService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReminderMetricsTest {
    @Test void successfulEmptyScanIsStillACompletedJob() {
        var registry = new SimpleMeterRegistry();
        var metrics = new ReminderMetrics(registry);
        var friends = mock(FriendService.class);
        var preferences = mock(UserPreferenceService.class);
        when(friends.getAllUserIds()).thenReturn(List.of());
        when(preferences.findByUserIds(List.of())).thenReturn(Map.of());
        when(friends.getFriendsByUserIds(List.of())).thenReturn(Map.of());
        new BirthdayReminder(friends, mock(NotificationSender.class), preferences, metrics)
                .checkBirthdaysForHour(ZonedDateTime.now());
        assertThat(registry.get("festiva.reminders.runs").tag("outcome", "success").counter().count()).isEqualTo(1);
        assertThat(registry.get("festiva.reminders.last.success.seconds").gauge().value()).isPositive();
    }

    @Test void failedDatabaseReadDoesNotAdvanceLastSuccess() {
        var registry = new SimpleMeterRegistry();
        var metrics = new ReminderMetrics(registry);
        var friends = mock(FriendService.class);
        when(friends.getAllUserIds()).thenThrow(new IllegalStateException("unavailable"));
        var reminder = new BirthdayReminder(friends, mock(NotificationSender.class), mock(UserPreferenceService.class), metrics);
        assertThatThrownBy(() -> reminder.checkBirthdaysForHour(ZonedDateTime.now())).isInstanceOf(IllegalStateException.class);
        assertThat(registry.get("festiva.reminders.runs").tag("outcome", "failure").counter().count()).isEqualTo(1);
        assertThat(registry.get("festiva.reminders.last.success.seconds").gauge().value()).isZero();
    }

    @Test void failedDeliveryIsNotReportedAsASuccessfulRun() {
        var registry = new SimpleMeterRegistry();
        var metrics = new ReminderMetrics(registry);
        metrics.complete(metrics.start(), false, 2, 1);
        assertThat(registry.get("festiva.reminders.deliveries").tag("outcome", "success").counter().count()).isEqualTo(2);
        assertThat(registry.get("festiva.reminders.deliveries").tag("outcome", "failure").counter().count()).isEqualTo(1);
        assertThat(registry.get("festiva.reminders.last.success.seconds").gauge().value()).isZero();
    }
}
