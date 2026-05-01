package com.festiva.friend.api;

import com.festiva.bot.CallbackResult;
import com.festiva.friend.entity.Friend;
import com.festiva.friend.handler.DatePickerCallbackHandler;
import com.festiva.friend.handler.EditCallbackHandler;
import com.festiva.friend.handler.EditFriendCommandHandler;
import com.festiva.friend.handler.ListCommandHandler;
import com.festiva.friend.handler.RemoveCommandHandler;
import com.festiva.friend.workflow.FriendWorkflowSessionService;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.i18n.MessagesTestSupport;
import com.festiva.state.BotState;
import com.festiva.state.UserStateService;
import com.festiva.user.api.UserDateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("FriendCallbackService")
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unused")
class FriendCallbackServiceTest extends MessagesTestSupport {

    @Mock private FriendService friendService;
    @Mock private UserStateService userStateService;
    @Mock private DatePickerCallbackHandler datePickerHandler;
    @Mock private EditCallbackHandler editHandler;
    @Mock private RemoveCommandHandler removeCommandHandler;
    @Mock private EditFriendCommandHandler editFriendCommandHandler;
    @Mock private FriendWorkflowSessionService friendWorkflowSessionService;
    @Mock private UserDateService userDateService;
    @Mock private ListCommandHandler listHandler;
    @InjectMocks private FriendCallbackService service;

    @BeforeEach
    void defaults() {
        lenient().when(userDateService.todayFor(anyLong())).thenReturn(LocalDate.now());
    }

    @Test
    @DisplayName("REMOVE_ callback stores pending friend and returns confirmation")
    void removeCallback_storesPendingAndReturnsConfirmation() {
        Friend alice = new Friend("Alice", LocalDate.of(1990, 1, 1));
        alice.setId("id-alice");
        when(friendService.findOwnedFriend("id-alice", 1L)).thenReturn(Optional.of(alice));

        CallbackResult result = service.handle("REMOVE_id-alice", 1L, Lang.EN);

        verify(friendWorkflowSessionService).setPendingName(1L, "Alice");
        verify(friendWorkflowSessionService).setPendingId(1L, "id-alice");
        verify(userStateService).setState(1L, BotState.WAITING_FOR_REMOVE_CONFIRM);
        assertThat(result.text).contains(Messages.get(Lang.EN, Messages.CONFIRM_REMOVE_ASK, "Alice"));
        assertThat(result.markup).isNotNull();
    }

    @Test
    @DisplayName("CONFIRM_REMOVE_ callback deletes friend and clears state")
    void confirmRemoveCallback_deletesFriendAndClearsState() {
        Friend alice = new Friend("Alice", LocalDate.of(1990, 1, 1));
        alice.setId("id-alice");
        when(friendService.findOwnedFriend("id-alice", 1L)).thenReturn(Optional.of(alice));

        CallbackResult result = service.handle("CONFIRM_REMOVE_id-alice", 1L, Lang.EN);

        verify(friendService).deleteFriendById("id-alice", 1L);
        verify(userStateService).clearState(1L);
        assertThat(result.text).contains(Messages.get(Lang.EN, Messages.FRIEND_REMOVED, "Alice"));
    }

    @Test
    @DisplayName("MONTH_ callback filters friends by month")
    void monthCallback_filtersFriendsByMonth() {
        Friend june = new Friend("Alice", LocalDate.of(1990, 6, 15));
        Friend december = new Friend("Bob", LocalDate.of(1990, 12, 1));
        when(friendService.getFriendsSortedByDayMonth(1L)).thenReturn(List.of(june, december));
        when(userDateService.todayFor(1L)).thenReturn(LocalDate.of(2024, 6, 1));

        CallbackResult result = service.handle("MONTH_6", 1L, Lang.EN);

        assertThat(result.text).contains("Alice").doesNotContain("Bob");
        assertThat(result.markup).isNotNull();
        assertThat(result.markup.getKeyboard().getFirst().getFirst().getCallbackData())
                .isEqualTo(com.festiva.command.handler.MoreCommandHandler.CALLBACK_BROWSE);
    }

    @Test
    @DisplayName("MONTH_CURRENT callback resolves current month and returns no-birthdays message")
    void monthCurrentCallback_returnsNoBirthdaysMessage() {
        when(friendService.getFriendsSortedByDayMonth(1L)).thenReturn(List.of());
        when(userDateService.todayFor(1L)).thenReturn(LocalDate.of(2024, 3, 15));

        CallbackResult result = service.handle("MONTH_CURRENT", 1L, Lang.EN);

        assertThat(result.text).contains(Messages.get(Lang.EN, Messages.BIRTHDAYS_NONE,
                Month.MARCH.getDisplayName(TextStyle.FULL_STANDALONE, Lang.EN.locale())));
        assertThat(result.markup).isNotNull();
    }
}
