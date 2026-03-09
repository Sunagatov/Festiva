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
import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("EditCallbackHandler")
@ExtendWith(MockitoExtension.class)
class EditCallbackHandlerTest extends MessagesTestSupport {

    @Mock FriendService friendService;
    @Mock UserStateService userStateService;
    @InjectMocks EditCallbackHandler handler;

    @BeforeEach
    void setup() {
        Friend alice = new Friend("Alice", LocalDate.of(1990, 3, 15));
        alice.setId("id-alice");
        lenient().when(friendService.findFriendById("id-alice")).thenReturn(Optional.of(alice));
    }

    @Test
    @DisplayName("handleEditSelect → response contains friend name and field buttons use ID")
    void handleEditSelect_containsNameAndIdButtons() {
        CallbackResult result = handler.handleEditSelect(EditCallbackHandler.EDIT_PREFIX + "id-alice", Lang.EN);

        assertThat(result.text).contains("Alice");
        assertThat(result.markup).isNotNull();
        result.markup.getKeyboard().stream().flatMap(Collection::stream)
                .forEach(btn -> assertThat(btn.getCallbackData()).contains("id-alice"));
    }

    @Test
    @DisplayName("handleEditSelect → friend not found returns SESSION_EXPIRED")
    void handleEditSelect_notFound_returnsSessionExpired() {
        when(friendService.findFriendById("ghost")).thenReturn(Optional.empty());
        CallbackResult result = handler.handleEditSelect(EditCallbackHandler.EDIT_PREFIX + "ghost", Lang.EN);
        assertThat(result.text).contains(Messages.get(Lang.EN, Messages.SESSION_EXPIRED));
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
    @DisplayName("handleEditFieldName → friend not found returns SESSION_EXPIRED")
    void handleEditFieldName_notFound_returnsSessionExpired() {
        when(friendService.findFriendById("ghost")).thenReturn(Optional.empty());
        CallbackResult result = handler.handleEditFieldName(EditCallbackHandler.EDIT_FIELD_NAME + "ghost", 1L, Lang.EN);
        assertThat(result.text).contains(Messages.get(Lang.EN, Messages.SESSION_EXPIRED));
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
    @DisplayName("handleEditNotify → friend not found returns SESSION_EXPIRED")
    void handleEditNotify_notFound_returnsSessionExpired() {
        when(friendService.findFriendById("ghost")).thenReturn(Optional.empty());
        CallbackResult result = handler.handleEditNotify(EditCallbackHandler.EDIT_FIELD_NOTIFY + "ghost", 1L, Lang.EN);
        assertThat(result.text).contains(Messages.get(Lang.EN, Messages.SESSION_EXPIRED));
    }

    @Test
    @DisplayName("handleEditSelect prompt contains /cancel hint")
    void handleEditSelect_prompt_containsCancelHint() {
        CallbackResult result = handler.handleEditSelect(EditCallbackHandler.EDIT_PREFIX + "id-alice", Lang.EN);
        assertThat(result.text).contains("/cancel");
    }

    @Test
    @DisplayName("handleEditNotify success contains next-step hint")
    void handleEditNotify_success_containsNextStepHint() {
        when(friendService.toggleFriendNotify(1L, "Alice")).thenReturn(true);
        CallbackResult result = handler.handleEditNotify(EditCallbackHandler.EDIT_FIELD_NOTIFY + "id-alice", 1L, Lang.EN);
        assertThat(result.text).contains("/edit");
    }

    @Test
    @DisplayName("handleEditNotify RU → returns RU message")
    void handleEditNotify_ru_returnsRuMessage() {
        when(friendService.toggleFriendNotify(1L, "Alice")).thenReturn(false);
        CallbackResult result = handler.handleEditNotify(EditCallbackHandler.EDIT_FIELD_NOTIFY + "id-alice", 1L, Lang.RU);
        assertThat(result.text).contains(Messages.get(Lang.RU, Messages.EDIT_NOTIFY_TOGGLED, "Alice",
                Messages.get(Lang.RU, Messages.NOTIFY_STATUS_OFF)));
    }
}
