package com.festiva.friend;

import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.friend.repository.FriendMongoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DisplayName("FriendService — sort logic")
@ExtendWith(MockitoExtension.class)
class FriendServiceTest {

    @Mock FriendMongoRepository repo;
    @InjectMocks FriendService service;

    @Test
    @DisplayName("getFriendsSortedByDayMonth() — sorts by day/month regardless of birth year")
    void sortsByDayMonth() {
        Friend dec = new Friend("Dec", LocalDate.of(1990, 12, 1));
        Friend jan = new Friend("Jan", LocalDate.of(1995, 1, 15));
        Friend mar = new Friend("Mar", LocalDate.of(1985, 3, 10));
        when(repo.findByTelegramUserId(1L)).thenReturn(List.of(dec, mar, jan));

        assertThat(service.getFriendsSortedByDayMonth(1L))
                .extracting(Friend::getName)
                .containsExactly("Jan", "Mar", "Dec");
    }

    @Test
    @DisplayName("getFriendsSortedByDayMonth() — Feb 29 sorts before Mar 1")
    void leapDaySortsBeforeMarch() {
        Friend leap = new Friend("Leap", LocalDate.of(2000, 2, 29));
        Friend mar  = new Friend("Mar",  LocalDate.of(1990, 3, 1));
        when(repo.findByTelegramUserId(1L)).thenReturn(List.of(mar, leap));

        assertThat(service.getFriendsSortedByDayMonth(1L))
                .extracting(Friend::getName)
                .containsExactly("Leap", "Mar");
    }
}
