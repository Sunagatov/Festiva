package com.festiva.friend;

import com.festiva.friend.entity.Friend;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.DateTimeException;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    @DisplayName("nextBirthday() — Feb 29 with year from a non-leap year returns Feb 28")
    void nextBirthday_leapDay_withYear_fromNonLeapYear() {
        Friend leapFriend = new Friend("Leap", LocalDate.of(2000, 2, 29));
        assertThat(leapFriend.nextBirthday(LocalDate.of(2023, 3, 1))).isEqualTo(LocalDate.of(2024, 2, 29));
        assertThat(leapFriend.nextBirthday(LocalDate.of(2024, 3, 1))).isEqualTo(LocalDate.of(2028, 2, 29));
        assertThat(leapFriend.nextBirthday(LocalDate.of(2025, 1, 1))).isEqualTo(LocalDate.of(2028, 2, 29));
    }
    
    @Test
    @DisplayName("nextBirthday() — Feb 29 without year from non-leap year returns Feb 28")
    void nextBirthday_leapDay_noYear_fromNonLeapYear() {
        Friend leapFriend = new Friend("Leap", null, 2, 29);
        assertThat(leapFriend.nextBirthday(LocalDate.of(2023, 3, 1))).isEqualTo(LocalDate.of(2024, 2, 29));
        assertThat(leapFriend.nextBirthday(LocalDate.of(2023, 1, 1))).isEqualTo(LocalDate.of(2023, 2, 28));
        assertThat(leapFriend.nextBirthday(LocalDate.of(2025, 1, 1))).isEqualTo(LocalDate.of(2025, 2, 28));
    }

    @Test
    @DisplayName("getNextAge(LocalDate) — birthday still ahead this year → turns 30 on next birthday")
    void getNextAge_birthdayAhead() {
        LocalDate from = LocalDate.of(2024, 6, 1);
        assertThat(new Friend("Alice", LocalDate.of(1994, 6, 2)).getNextAge(from)).isEqualTo(30);
    }

    @Test
    @DisplayName("getNextAge(LocalDate) — birthday already passed this year → turns 31 on next birthday")
    void getNextAge_birthdayPassed() {
        LocalDate from = LocalDate.of(2024, 6, 1);
        assertThat(new Friend("Alice", LocalDate.of(1994, 5, 31)).getNextAge(from)).isEqualTo(31);
    }
    
    @Test
    @DisplayName("getNextAge(LocalDate) — no year throws IllegalStateException")
    void getNextAge_noYear_throws() {
        Friend noYearFriend = new Friend("Sarah", null, 6, 15);
        assertThatThrownBy(() -> noYearFriend.getNextAge(LocalDate.now()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot calculate age without birth year");
    }

    @Test
    @DisplayName("getZodiac() — returns correct sign for boundary dates")
    void getZodiac_correctSignForBoundaryDates() {
        assertThat(new Friend("x", LocalDate.of(1990,  3, 21)).getZodiac()).isEqualTo("♈");
        assertThat(new Friend("x", LocalDate.of(1990,  4, 19)).getZodiac()).isEqualTo("♈");
        assertThat(new Friend("x", LocalDate.of(1990,  4, 20)).getZodiac()).isEqualTo("♉");
        assertThat(new Friend("x", LocalDate.of(1990, 12, 22)).getZodiac()).isEqualTo("♑");
        assertThat(new Friend("x", LocalDate.of(1990,  2, 19)).getZodiac()).isEqualTo("♓");
        assertThat(new Friend("x", LocalDate.of(1990,  1, 20)).getZodiac()).isEqualTo("♒");
    }
    
    @Test
    @DisplayName("getZodiac() — works without year")
    void getZodiac_noYear() {
        assertThat(new Friend("x", null, 3, 21).getZodiac()).isEqualTo("♈");
        assertThat(new Friend("x", null, 6, 15).getZodiac()).isEqualTo("♊");
    }
    
    @Test
    @DisplayName("hasYear() — returns true when year is set")
    void hasYear_withYear() {
        assertThat(new Friend("Alice", LocalDate.of(1990, 6, 15)).hasYear()).isTrue();
    }
    
    @Test
    @DisplayName("hasYear() — returns false when year is null")
    void hasYear_noYear() {
        assertThat(new Friend("Sarah", null, 6, 15).hasYear()).isFalse();
    }
    
    @Test
    @DisplayName("getBirthDate() — returns LocalDate when year is set")
    void getBirthDate_withYear() {
        Friend friend = new Friend("Alice", 1990, 6, 15);
        assertThat(friend.getBirthDate()).isEqualTo(LocalDate.of(1990, 6, 15));
    }
    
    @Test
    @DisplayName("getBirthDate() — throws when year is null")
    void getBirthDate_noYear_throws() {
        Friend noYearFriend = new Friend("Sarah", null, 6, 15);
        assertThatThrownBy(noYearFriend::getBirthDate)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Birth year is unknown");
    }
    
    @Test
    @DisplayName("getAge() — throws when year is null")
    void getAge_noYear_throws() {
        Friend noYearFriend = new Friend("Sarah", null, 6, 15);
        assertThatThrownBy(() -> noYearFriend.getAge(LocalDate.now()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot calculate age without birth year");
    }
    
    @Test
    @DisplayName("constructor validates invalid date")
    void constructor_invalidDate_throws() {
        assertThatThrownBy(() -> new Friend("Invalid", 2024, 2, 30))
            .isInstanceOf(IllegalArgumentException.class)
            .hasCauseInstanceOf(DateTimeException.class);
        assertThatThrownBy(() -> new Friend("Invalid", 2024, 13, 15))
            .isInstanceOf(IllegalArgumentException.class)
            .hasCauseInstanceOf(DateTimeException.class);
    }
    
    @Test
    @DisplayName("constructor validates invalid month-day without year")
    void constructor_noYear_invalidDate_throws() {
        assertThatThrownBy(() -> new Friend("Invalid", null, 2, 30))
            .isInstanceOf(IllegalArgumentException.class)
            .hasCauseInstanceOf(DateTimeException.class);
        assertThatThrownBy(() -> new Friend("Invalid", null, 13, 15))
            .isInstanceOf(IllegalArgumentException.class)
            .hasCauseInstanceOf(DateTimeException.class);
    }
    
    @Test
    @DisplayName("constructor allows Feb 29 without year")
    void constructor_noYear_feb29_valid() {
        Friend leapFriend = new Friend("Leap", null, 2, 29);
        assertThat(leapFriend.hasYear()).isFalse();
        assertThat(leapFriend.getBirthMonthDay().getMonthValue()).isEqualTo(2);
        assertThat(leapFriend.getBirthMonthDay().getDayOfMonth()).isEqualTo(29);
    }
}
