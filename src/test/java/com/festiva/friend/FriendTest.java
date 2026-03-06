package com.festiva.friend;

import com.festiva.friend.entity.Friend;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Friend entity")
class FriendTest {

    @DisplayName("nextBirthday()")
    @ParameterizedTest(name = "{0}")
    @CsvSource({
        "birthday already passed this year → rolls to next year,  1990-01-01, 2024-06-01, 2025-01-01",
        "birthday still ahead this year   → stays in same year,   1990-12-31, 2024-06-01, 2024-12-31",
        "birthday is today                → returns today,         1990-06-01, 2024-06-01, 2024-06-01",
    })
    void nextBirthday(String label, LocalDate birthDate, LocalDate from, LocalDate expected) {
        assertThat(new Friend("Alice", birthDate).nextBirthday(from)).isEqualTo(expected);
    }

    @Test
    @DisplayName("nextBirthday() — Feb 29 from a non-leap year returns the correct Feb 29 in next leap year")
    void nextBirthday_leapDay_fromNonLeapYear() {
        Friend leapFriend = new Friend("Leap", LocalDate.of(2000, 2, 29));
        assertThat(leapFriend.nextBirthday(LocalDate.of(2023, 3, 1))).isEqualTo(LocalDate.of(2024, 2, 29));
        assertThat(leapFriend.nextBirthday(LocalDate.of(2024, 3, 1))).isEqualTo(LocalDate.of(2028, 2, 29));
        assertThat(leapFriend.nextBirthday(LocalDate.of(2025, 1, 1))).isEqualTo(LocalDate.of(2028, 2, 29));
    }

    @Test
    @DisplayName("getNextAge() — birthday still ahead this year → turns 30 on next birthday")
    void getNextAge_birthdayAhead() {
        LocalDate today = LocalDate.now();
        Friend friend = new Friend("Alice", today.plusDays(1).minusYears(30));
        assertThat(friend.getNextAge()).isEqualTo(30);
    }

    @Test
    @DisplayName("getNextAge() — birthday already passed this year → turns 31 on next birthday")
    void getNextAge_birthdayPassed() {
        LocalDate today = LocalDate.now();
        Friend friend = new Friend("Alice", today.minusDays(1).minusYears(30));
        assertThat(friend.getNextAge()).isEqualTo(31);
    }
}
