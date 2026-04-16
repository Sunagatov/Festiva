package com.festiva.state;

import com.festiva.i18n.Lang;
import com.festiva.user.UserPreference;
import com.festiva.user.UserPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("UserStateService")
class UserStateServiceTest {

    UserPreferenceRepository repo;
    UserStateService service;

    @BeforeEach
    void setUp() {
        repo = mock(UserPreferenceRepository.class);
        when(repo.findById(anyLong())).thenReturn(Optional.empty());
        service = new UserStateService(
            mock(UserSessionRepository.class),
            repo,
            mock(PendingImportRepository.class)
        );
    }

    @Test
    @DisplayName("new user defaults to IDLE state and EN language")
    void newUser_defaultsToIdleAndRu() {
        assertThat(service.getState(1L)).isEqualTo(BotState.IDLE);
        assertThat(service.getLanguage(1L)).isEqualTo(Lang.EN);
    }

    @Test
    @DisplayName("setState() / getState() round-trips correctly")
    void setState_roundTrips() {
        service.setState(1L, BotState.WAITING_FOR_ADD_FRIEND_NAME);
        assertThat(service.getState(1L)).isEqualTo(BotState.WAITING_FOR_ADD_FRIEND_NAME);
    }

    @Test
    @DisplayName("clearState() resets to IDLE and clears pending name")
    void clearState_resetsToIdle() {
        service.setState(1L, BotState.WAITING_FOR_ADD_FRIEND_DATE);
        service.setPendingName(1L, "Alice");
        service.clearState(1L);
        assertThat(service.getState(1L)).isEqualTo(BotState.IDLE);
        assertThat(service.getPendingName(1L)).isNull();
    }

    @Test
    @DisplayName("setLanguage() persists to repo and updates in-memory cache")
    void setLanguage_persistsAndCaches() {
        when(repo.findById(1L)).thenReturn(Optional.of(new UserPreference(1L, Lang.RU, 9, "Europe/Moscow", null)));
        service.setLanguage(1L, Lang.EN);
        assertThat(service.getLanguage(1L)).isEqualTo(Lang.EN);
        verify(repo).save(any(UserPreference.class));
    }

    @Test
    @DisplayName("setNotifyHour() persists to repo")
    void setNotifyHour_persistsToRepo() {
        service.setNotifyHour(1L, 10);
        verify(repo).save(any(UserPreference.class));
    }

    @Test
    @DisplayName("setTimezone() persists to repo")
    void setTimezone_persistsToRepo() {
        service.setTimezone(1L, "Europe/London");
        verify(repo).save(any(UserPreference.class));
    }

    @Test
    @DisplayName("getNotifyHour() returns 9 when no prefs saved")
    void getNotifyHour_defaultsToNine() {
        assertThat(service.getNotifyHour(1L)).isEqualTo(9);
    }

    @Test
    @DisplayName("getTimezone() returns UTC when no prefs saved")
    void getTimezone_defaultsMoscow() {
        assertThat(service.getTimezone(1L)).isEqualTo("UTC");
    }

    @Test
    @DisplayName("sessions are isolated per userId")
    void sessions_areIsolatedPerUser() {
        service.setState(1L, BotState.WAITING_FOR_ADD_FRIEND_NAME);
        service.setLanguage(1L, Lang.RU);
        assertThat(service.getState(2L)).isEqualTo(BotState.IDLE);
        assertThat(service.getLanguage(2L)).isEqualTo(Lang.EN);
    }
}
