package com.festiva.command;

import com.festiva.command.handler.EditFriendCommandHandler;
import com.festiva.friend.api.FriendService;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.i18n.MessagesTestSupport;
import com.festiva.state.UserStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("EditFriendCommandHandler")
@ExtendWith(MockitoExtension.class)
class EditFriendCommandHandlerTest extends MessagesTestSupport {

    @Mock FriendService friendService;
    @Mock UserStateService userStateService;
    @InjectMocks EditFriendCommandHandler handler;

    @BeforeEach
    void defaults() {
        lenient().when(userStateService.getLanguage(anyLong())).thenReturn(Lang.EN);
        lenient().when(userStateService.getPendingName(anyLong())).thenReturn("Alice");
    }

    @Test
    @DisplayName("handle with empty list → returns friends_empty")
    void handle_emptyList_returnsFriendsEmpty() {
        when(friendService.getFriendsSortedByDayMonth(1L)).thenReturn(List.of());

        assertThat(handler.handle(update("")).getText())
                .contains(Messages.get(Lang.EN, Messages.FRIENDS_EMPTY));
    }

    @Test
    @DisplayName("handleState with null oldName → SESSION_EXPIRED")
    void handleState_nullOldName_returnsSessionExpired() {
        when(userStateService.getPendingName(1L)).thenReturn(null);

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
        when(friendService.friendExists(1L, "Bob")).thenReturn(false);

        assertThat(handler.handleState(update("Bob")).getText())
                .contains(Messages.get(Lang.EN, Messages.EDIT_NAME_DONE, "Bob"));
        verify(friendService).updateFriendName(1L, "Alice", "Bob");
        verify(userStateService).clearState(1L);
    }

    @Test
    @DisplayName("handleState success → next-step hint contains /edit")
    void handleState_success_containsNextStepHint() {
        when(friendService.friendExists(1L, "Bob")).thenReturn(false);

        assertThat(handler.handleState(update("Bob")).getText()).contains("/edit");
    }

    @Test
    @DisplayName("handleState RU blank name → returns RU error")
    void handleState_ruBlankName_returnsRuError() {
        when(userStateService.getLanguage(anyLong())).thenReturn(Lang.RU);

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
