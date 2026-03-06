package com.festiva.friend;

import com.festiva.IntegrationTestBase;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.friend.repository.FriendMongoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FriendService (integration)")
class FriendServiceIntegrationTest extends IntegrationTestBase {

    @Autowired FriendService friendService;
    @Autowired FriendMongoRepository repo;

    @BeforeEach
    void clean() { repo.deleteAll(); }

    @Test
    @DisplayName("addFriend() persists and getFriends() retrieves it")
    void addAndRetrieve() {
        friendService.addFriend(1L, new Friend("Alice", LocalDate.of(1990, 6, 15)));
        assertThat(friendService.getFriends(1L))
                .hasSize(1)
                .first().extracting(Friend::getName).isEqualTo("Alice");
    }

    @Test
    @DisplayName("friendExists() returns true only for the saved friend")
    void friendExists() {
        friendService.addFriend(1L, new Friend("Alice", LocalDate.of(1990, 1, 1)));
        assertThat(friendService.friendExists(1L, "Alice")).isTrue();
        assertThat(friendService.friendExists(1L, "Bob")).isFalse();
    }

    @Test
    @DisplayName("deleteFriend() removes the friend")
    void deleteFriend() {
        friendService.addFriend(1L, new Friend("Alice", LocalDate.of(1990, 1, 1)));
        friendService.deleteFriend(1L, "Alice");
        assertThat(friendService.getFriends(1L)).isEmpty();
    }

    @Test
    @DisplayName("friends are isolated per telegramUserId")
    void userIsolation() {
        friendService.addFriend(1L, new Friend("Alice", LocalDate.of(1990, 1, 1)));
        friendService.addFriend(2L, new Friend("Bob",   LocalDate.of(1990, 1, 1)));
        assertThat(friendService.getFriends(1L)).extracting(Friend::getName).containsExactly("Alice");
        assertThat(friendService.getFriends(2L)).extracting(Friend::getName).containsExactly("Bob");
    }

    @Test
    @DisplayName("getAllUserIds() returns distinct user IDs that have friends")
    void getAllUserIds() {
        friendService.addFriend(10L, new Friend("A", LocalDate.of(1990, 1, 1)));
        friendService.addFriend(20L, new Friend("B", LocalDate.of(1990, 1, 1)));
        assertThat(friendService.getAllUserIds()).containsExactlyInAnyOrder(10L, 20L);
    }
}
