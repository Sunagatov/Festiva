package com.festiva.friend.handler;

import com.festiva.friend.api.FriendService;
import com.festiva.friend.workflow.FriendWorkflowSessionService;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.i18n.MessagesTestSupport;
import com.festiva.state.UserStateService;
import com.festiva.user.api.UserPreferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("EditFriendCommandHandler")
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unused")
class EditFriendCommandHandlerTest extends MessagesTestSupport {

    @Mock FriendService friendService;
    @Mock UserStateService userStateService;
    @Mock FriendWorkflowSessionService friendWorkflowSessionService;
    @Mock UserPreferenceService userPreferenceService;
    @InjectMocks EditFriendCommandHandler handler;

    @BeforeEach
    void defaults() {
        lenient().when(userPreferenceService.getLanguage(anyLong())).thenReturn(Lang.EN);
        lenient().when(friendWorkflowSessionService.getPendingName(anyLong())).thenReturn("Alice");
        lenient().when(friendWorkflowSessionService.getPendingId(anyLong())).thenReturn("id-alice");

        com.festiva.friend.entity.Friend alice = new com.festiva.friend.entity.Friend("Alice", java.time.LocalDate.of(1990, 1, 1));
        alice.setId("id-alice");
        alice.setTelegramUserId(1L);
        lenient().when(friendService.findOwnedFriend("id-alice", 1L)).thenReturn(java.util.Optional.of(alice));
    }

    @Test
    @DisplayName("handle with empty list → returns friends_empty")
    void handle_emptyList_returnsFriendsEmpty() {
        when(friendService.getFriendsSortedByDayMonth(1L)).thenReturn(List.of());

        var result = handler.handle(update(""));
        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.FRIENDS_EMPTY));
        var markup = (InlineKeyboardMarkup) result.getReplyMarkup();
        assertThat(markup).isNotNull();
        assertThat(markup.getKeyboard().getFirst().getFirst().getText())
                .isEqualTo(Messages.get(Lang.EN, Messages.REMOVE_EMPTY_ADD));
        assertThat(markup.getKeyboard().getLast().getFirst().getCallbackData())
                .isEqualTo(com.festiva.command.handler.MoreCommandHandler.CALLBACK_BACK);
    }

    @Test
    @DisplayName("handle with friends → includes back to more action")
    void handle_withFriends_includesBackToMoreAction() {
        when(friendService.getFriendsSortedByDayMonth(1L)).thenReturn(List.of(
                new com.festiva.friend.entity.Friend("Alice", java.time.LocalDate.of(1990, 1, 1))));

        var result = handler.handle(update(""));
        var markup = (InlineKeyboardMarkup) result.getReplyMarkup();

        assertThat(markup).isNotNull();
        assertThat(markup.getKeyboard().getLast().getFirst().getCallbackData())
                .isEqualTo(com.festiva.command.handler.MoreCommandHandler.CALLBACK_BACK);
    }

    @Test
    @DisplayName("handleState with null oldName → SESSION_EXPIRED")
    void handleState_nullOldName_returnsSessionExpired() {
        lenient().when(friendWorkflowSessionService.getPendingName(1L)).thenReturn(null);

        assertThat(handler.handleState(update("NewName")).getText())
                .contains(Messages.get(Lang.EN, Messages.SESSION_EXPIRED));
    }

    @Test
    @DisplayName("handleState with blank name → name_empty error")
    void handleState_blankName_returnsNameEmpty() {
        assertThat(handler.handleState(update("   ")).getText())
                .contains(Messages.get(Lang.EN, Messages.NAME_EMPTY));
    }

    @Test
    @DisplayName("handleState with name > 100 chars → name_too_long error")
    void handleState_nameTooLong_returnsNameTooLong() {
        assertThat(handler.handleState(update("A".repeat(101))).getText())
                .contains(Messages.get(Lang.EN, Messages.NAME_TOO_LONG));
    }

    @Test
    @DisplayName("handleState with duplicate name (different friend) → name_exists error")
    void handleState_duplicateName_returnsNameExists() {
        when(friendService.friendExists(1L, "Bob")).thenReturn(true);

        assertThat(handler.handleState(update("Bob")).getText())
                .contains(Messages.get(Lang.EN, Messages.NAME_EXISTS, "Bob"));
    }

    @Test
    @DisplayName("handleState same name (case-insensitive) → skips duplicate check and succeeds")
    void handleState_sameNameCaseInsensitive_succeeds() {
        when(friendService.friendExists(1L, "alice")).thenReturn(true);

        assertThat(handler.handleState(update("alice")).getText())
                .contains(Messages.get(Lang.EN, Messages.EDIT_NAME_DONE, "alice"));
    }

    @Test
    @DisplayName("handleState valid new name → updates name, clears state, returns edit_name_done")
    void handleState_validName_updatesAndClearsState() {
        when(friendWorkflowSessionService.getPendingId(1L)).thenReturn("id-alice");
        when(friendService.friendExists(1L, "Bob")).thenReturn(false);

        assertThat(handler.handleState(update("Bob")).getText())
                .contains(Messages.get(Lang.EN, Messages.EDIT_NAME_DONE, "Bob"));
        verify(friendService).updateFriendNameById("id-alice", 1L, "Bob");
        verify(userStateService).clearState(1L);
    }

    @Test
    @DisplayName("handleState success → returns quick action buttons")
    void handleState_success_returnsQuickActions() {
        when(friendService.friendExists(1L, "Bob")).thenReturn(false);

        var result = handler.handleState(update("Bob"));
        assertThat(result.getText()).doesNotContain("/edit");
        assertThat(result.getReplyMarkup()).isNotNull();
    }

    @Test
    @DisplayName("handleState RU blank name → returns RU error")
    void handleState_ruBlankName_returnsRuError() {
        when(userPreferenceService.getLanguage(anyLong())).thenReturn(Lang.RU);

        assertThat(handler.handleState(update("   ")).getText())
                .contains(Messages.get(Lang.RU, Messages.NAME_EMPTY));
    }

    private Update update(String text) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        Message message = mock(Message.class);
        when(message.getFrom()).thenReturn(user);
        when(message.getChatId()).thenReturn(1L);
        lenient().when(message.getText()).thenReturn(text);
        Update update = mock(Update.class);
        when(update.getMessage()).thenReturn(message);
        return update;
    }
}
