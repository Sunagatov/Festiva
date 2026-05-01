package com.festiva.state;

import com.festiva.friend.workflow.FriendWorkflowSessionRepository;
import com.festiva.friend.workflow.FriendWorkflowSessionService;
import com.festiva.importing.PendingIcsImportRepository;
import com.festiva.importing.PendingIcsImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("UserStateService")
@SuppressWarnings("unused")
class UserStateServiceTest {

    FriendWorkflowSessionRepository friendWorkflowSessionRepository;
    PendingIcsImportRepository pendingIcsImportRepository;
    UserStateService service;

    @BeforeEach
    void setUp() {
        friendWorkflowSessionRepository = mock(FriendWorkflowSessionRepository.class);
        pendingIcsImportRepository = mock(PendingIcsImportRepository.class);
        service = new UserStateService(
            mock(UserSessionRepository.class),
            new FriendWorkflowSessionService(friendWorkflowSessionRepository),
            new PendingIcsImportService(pendingIcsImportRepository)
        );
    }

    @Test
    @DisplayName("new user defaults to IDLE state")
    void newUser_defaultsToIdle() {
        assertThat(service.getState(1L)).isEqualTo(BotState.IDLE);
    }

    @Test
    @DisplayName("setState() / getState() round-trips correctly")
    void setState_roundTrips() {
        service.setState(1L, BotState.WAITING_FOR_ADD_FRIEND_NAME);
        assertThat(service.getState(1L)).isEqualTo(BotState.WAITING_FOR_ADD_FRIEND_NAME);
    }

    @Test
    @DisplayName("clearState() resets to IDLE and clears friend workflow state")
    void clearState_resetsToIdle() {
        service.setState(1L, BotState.WAITING_FOR_ADD_FRIEND_DATE);
        when(friendWorkflowSessionRepository.findById(1L)).thenReturn(Optional.of(new com.festiva.friend.workflow.FriendWorkflowSession()));
        service.getState(1L);
        service.clearState(1L);
        assertThat(service.getState(1L)).isEqualTo(BotState.IDLE);
        verify(friendWorkflowSessionRepository).save(any(com.festiva.friend.workflow.FriendWorkflowSession.class));
        verify(friendWorkflowSessionRepository, never()).deleteById(1L);
        verify(pendingIcsImportRepository).deleteByUserId(1L);
    }

    @Test
    @DisplayName("sessions are isolated per userId")
    void sessions_areIsolatedPerUser() {
        service.setState(1L, BotState.WAITING_FOR_ADD_FRIEND_NAME);
        assertThat(service.getState(2L)).isEqualTo(BotState.IDLE);
    }
}
