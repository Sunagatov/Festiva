package com.festiva.bot;

import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.i18n.MessagesTestSupport;
import com.festiva.state.BotState;
import com.festiva.state.UserStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("EditCallbackHandler")
@ExtendWith(MockitoExtension.class)
class EditCallbackHandlerTest extends MessagesTestSupport {

    @Mock FriendService friendService;
    @Mock UserStateService userStateService;
    @InjectMocks EditCallbackHandler handler;

    private Friend alice;

    @BeforeEach
    void setup() {
        alice = new Friend("Alice", LocalDate.of(1990, 3, 15));
        alice.setId("id-alice");
        lenient().when(friendService.findFriendById("id-alice")).thenReturn(Optional.of(alice));
    }

    @Test
    @DisplayName("handleEditSelect → response contains friend name and field buttons use ID")
    void handleEditSelect_containsNameAndIdButtons() {
        CallbackResult result = handler.handleEditSelect(EditCallbackHandler.EDIT_PREFIX + "id-alice", 1L, Lang.EN);

        assertThat(result.text).contains("Alice");
        result.markup.getKeyboard().stream().flatMap(row -> row.stream())
                .forEach(btn -> assertThat(btn.getCallbackData()).contains("id-alice"));
    }

    @Test
    @DisplayName("handleEditSelect → friend not found returns unknown command")
    void handleEditSelect_notFound_returnsUnknown() {
        when(friendService.findFriendById("ghost")).thenReturn(Optional.empty());
        CallbackResult result = handler.handleEditSelect(EditCallbackHandler.EDIT_PREFIX + "ghost", 1L, Lang.EN);
        assertThat(result.text).contains("?");
    }

    @Test
    @DisplayName("handleEditFieldName → stores pendingName and pendingId, sets state")
    void handleEditFieldName_storesPendingAndSetsState() {
        handler.handleEditFieldName(EditCallbackHandler.EDIT_FIELD_NAME + "id-alice", 1L, Lang.EN);

        verify(userStateService).setPendingName(1L, "Alice");
        verify(userStateService).setPendingId(1L, "id-alice");
        verify(userStateService).setState(1L, BotState.WAITING_FOR_EDIT_NAME);
    }

    @Test
    @DisplayName("handleEditFieldName → friend not found returns unknown command")
    void handleEditFieldName_notFound_returnsUnknown() {
        when(friendService.findFriendById("ghost")).thenReturn(Optional.empty());
        CallbackResult result = handler.handleEditFieldName(EditCallbackHandler.EDIT_FIELD_NAME + "ghost", 1L, Lang.EN);
        assertThat(result.text).contains(Messages.get(Lang.EN, Messages.UNKNOWN_COMMAND));
    }

    @Test
    @DisplayName("handleEditFieldDate → stores pendingName and pendingId, sets WAITING_FOR_EDIT_DATE")
    void handleEditFieldDate_storesPendingAndSetsState() {
        handler.handleEditFieldDate(EditCallbackHandler.EDIT_FIELD_DATE + "id-alice", 1L, Lang.EN);

        verify(userStateService).setPendingName(1L, "Alice");
        verify(userStateService).setPendingId(1L, "id-alice");
        verify(userStateService).setState(1L, BotState.WAITING_FOR_EDIT_DATE);
    }

    @Test
    @DisplayName("handleEditNotify → toggles notification and returns toggled message")
    void handleEditNotify_togglesAndReturnsMessage() {
        when(friendService.toggleFriendNotify(1L, "Alice")).thenReturn(true);

        CallbackResult result = handler.handleEditNotify(EditCallbackHandler.EDIT_FIELD_NOTIFY + "id-alice", 1L, Lang.EN);

        verify(friendService).toggleFriendNotify(1L, "Alice");
        assertThat(result.text).contains("Alice");
    }

    @Test
    @DisplayName("handleEditNotify → friend not found returns unknown command")
    void handleEditNotify_notFound_returnsUnknown() {
        when(friendService.findFriendById("ghost")).thenReturn(Optional.empty());
        CallbackResult result = handler.handleEditNotify(EditCallbackHandler.EDIT_FIELD_NOTIFY + "ghost", 1L, Lang.EN);
        assertThat(result.text).contains(Messages.get(Lang.EN, Messages.UNKNOWN_COMMAND));
    }
}
