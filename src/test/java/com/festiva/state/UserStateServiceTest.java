package com.festiva.state;

import com.festiva.i18n.Lang;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserStateService")
class UserStateServiceTest {

    UserStateService service;

    @BeforeEach
    void setUp() { service = new UserStateService(); }

    @Test
    @DisplayName("new user defaults to IDLE state and RU language")
    void newUser_defaultsToIdleAndRu() {
        assertThat(service.getState(1L)).isEqualTo(BotState.IDLE);
        assertThat(service.getLanguage(1L)).isEqualTo(Lang.RU);
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
    @DisplayName("setLanguage() / getLanguage() round-trips correctly")
    void language_roundTrips() {
        service.setLanguage(1L, Lang.EN);
        assertThat(service.getLanguage(1L)).isEqualTo(Lang.EN);
    }

    @Test
    @DisplayName("sessions are isolated per userId")
    void sessions_areIsolatedPerUser() {
        service.setState(1L, BotState.WAITING_FOR_ADD_FRIEND_NAME);
        service.setLanguage(1L, Lang.EN);
        assertThat(service.getState(2L)).isEqualTo(BotState.IDLE);
        assertThat(service.getLanguage(2L)).isEqualTo(Lang.RU);
    }
}
