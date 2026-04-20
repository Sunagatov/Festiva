package com.festiva.friend.api;

import com.festiva.friend.entity.Friend;
import com.festiva.friend.repository.FriendMongoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("FriendService")
@ExtendWith(MockitoExtension.class)
class FriendServiceTest {

    @Mock
    FriendMongoRepository friendRepository;

    @InjectMocks
    FriendService friendService;

    @Test
    @DisplayName("addFriend when cap reached → throws and does not save")
    void addFriend_whenCapReached_throws() {
        when(friendRepository.existsByTelegramUserIdAndNormalizedName(1L, "alice")).thenReturn(false);
        when(friendRepository.countByTelegramUserId(1L)).thenReturn((long) FriendService.FRIEND_CAP);

        assertThatThrownBy(() -> friendService.addFriend(1L, new Friend("Alice", LocalDate.of(1990, 3, 15))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Friend cap");

        verify(friendRepository, never()).save(any());
    }
}
