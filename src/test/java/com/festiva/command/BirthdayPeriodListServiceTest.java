package com.festiva.command;

import com.festiva.command.handler.BirthdayPeriodListService;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.i18n.Lang;
import com.festiva.i18n.MessagesTestSupport;
import com.festiva.user.api.UserDateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class BirthdayPeriodListServiceTest extends MessagesTestSupport {

    private static final long USER_ID = 42L;
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 25);

    @Mock private FriendService friendService;
    @Mock private UserDateService userDateService;

    private BirthdayPeriodListService service;

    @BeforeEach
    void setUp() {
        service = new BirthdayPeriodListService(friendService, userDateService);
        lenient().when(userDateService.todayFor(USER_ID)).thenReturn(TODAY);
    }

    @Test
    void thisMonthIncludesOnlyCurrentMonthAndSortsByDate() {
        when(friendService.getFriends(USER_ID)).thenReturn(List.of(
                new Friend("Later", 1990, 10, 30),
                new Friend("Other month", 1990, 9, 20),
                new Friend("Earlier", 1990, 10, 5)));

        var view = service.view(USER_ID, Lang.EN,
                BirthdayPeriodListService.Period.THIS_MONTH, true, 0);

        assertThat(view.text()).contains("Birthdays — October", "Earlier", "Later")
                .doesNotContain("Other month");
        assertThat(view.text()).containsSubsequence("Earlier", "Later");
        assertThat(view.markup().getKeyboard().getFirst()).hasSize(2);
    }

    @Test
    void missedIncludesPreviousSixtyDaysButNotTodayOrOlderBirthdays() {
        when(friendService.getFriends(USER_ID)).thenReturn(List.of(
                new Friend("Yesterday", 1990, 10, 24),
                new Friend("Sixty days", 1990, 8, 26),
                new Friend("Sixty one days", 1990, 8, 25),
                new Friend("Today", 1990, 10, 25)));

        var view = service.view(USER_ID, Lang.EN,
                BirthdayPeriodListService.Period.MISSED, true, 0);

        assertThat(view.text()).contains("Yesterday", "Sixty days", "1 day ago", "60 days ago")
                .doesNotContain("Sixty one days", "<b>Today</b>");
        assertThat(view.text()).containsSubsequence("Yesterday", "Sixty days");
    }

    @Test
    void callbackKeepsPeriodAndSupportsNameSorting() {
        when(friendService.getFriends(USER_ID)).thenReturn(List.of(
                new Friend("Zoe", 1990, 10, 5),
                new Friend("Alice", 1990, 10, 20)));

        var result = service.handleCallback("BPL_THIS_MONTH_NAME_0", USER_ID, Lang.EN);

        assertThat(result).isNotNull();
        assertThat(result.text).containsSubsequence("Alice", "Zoe");
        assertThat(result.markup.getKeyboard().getFirst().get(1).getText()).startsWith("✅ ");
    }

    @Test
    void malformedCallbackIsIgnored() {
        assertThat(service.handleCallback("BPL_BROKEN", USER_ID, Lang.EN)).isNull();
        assertThat(service.handleCallback("OTHER", USER_ID, Lang.EN)).isNull();
    }
}
