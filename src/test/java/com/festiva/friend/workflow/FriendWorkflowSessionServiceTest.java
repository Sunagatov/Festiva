package com.festiva.friend.workflow;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FriendWorkflowSessionServiceTest {

    @Test
    void persistedSessionOlderThanOneHourExpires() {
        FriendWorkflowSessionRepository repository = mock(FriendWorkflowSessionRepository.class);
        FriendWorkflowSession stale = new FriendWorkflowSession();
        stale.setUserId(7L);
        stale.setPendingName("Old name");
        stale.setLastActivity(LocalDateTime.now().minusHours(2));
        when(repository.findById(7L)).thenReturn(Optional.of(stale));

        FriendWorkflowSessionService service = new FriendWorkflowSessionService(repository);

        assertThat(service.getPendingName(7L)).isNull();
    }
}
