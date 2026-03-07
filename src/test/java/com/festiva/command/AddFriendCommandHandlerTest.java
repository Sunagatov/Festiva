package com.festiva.command;

import com.festiva.command.handler.AddFriendCommandHandler;
import com.festiva.friend.api.FriendService;
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
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("AddFriendCommandHandler")
@ExtendWith(MockitoExtension.class)
class AddFriendCommandHandlerTest extends MessagesTestSupport {

    @Mock FriendService friendService;
    @Mock UserStateService userStateService;
    @InjectMocks AddFriendCommandHandler handler;

    @BeforeEach
    void defaults() {
        lenient().when(userStateService.getLanguage(anyLong())).thenReturn(Lang.EN);
    }

    @Test
    @DisplayName("handle when at friend cap → returns cap message")
    void handle_atCap_returnsCap() {
        when(friendService.getFriends(1L)).thenReturn(
                java.util.Collections.nCopies(FriendService.FRIEND_CAP, null));

        assertThat(handler.handle(update("")).getText())
                .contains(Messages.get(Lang.EN, Messages.FRIEND_CAP, FriendService.FRIEND_CAP));
    }

    @Test
    @DisplayName("handle under cap → sets WAITING_FOR_ADD_FRIEND_NAME and returns enter-name prompt")
    void handle_underCap_setsStateAndReturnsPrompt() {
        when(friendService.getFriends(1L)).thenReturn(List.of());

        assertThat(handler.handle(update("")).getText())
                .contains(Messages.get(Lang.EN, Messages.ENTER_NAME));
        verify(userStateService).setState(1L, BotState.WAITING_FOR_ADD_FRIEND_NAME);
    }

    @Test
    @DisplayName("handleState with blank name → returns name-empty error")
    void handleState_blankName_returnsError() {
        assertThat(handler.handleState(update("   ")).getText())
                .contains(Messages.get(Lang.EN, Messages.NAME_EMPTY));
    }

    @Test
    @DisplayName("handleState with name > 100 chars → returns name-too-long error")
    void handleState_nameTooLong_returnsError() {
        assertThat(handler.handleState(update("A".repeat(101))).getText())
                .contains(Messages.get(Lang.EN, Messages.NAME_TOO_LONG));
    }

    @Test
    @DisplayName("handleState with duplicate name → returns name-exists error")
    void handleState_duplicateName_returnsError() {
        when(friendService.friendExists(1L, "Alice")).thenReturn(true);

        assertThat(handler.handleState(update("Alice")).getText())
                .contains(Messages.get(Lang.EN, Messages.NAME_EXISTS, "Alice"));
    }

    @Test
    @DisplayName("handleState with valid name → stores pending name and transitions to date picker")
    void handleState_validName_storesPendingNameAndShowsDatePicker() {
        when(friendService.friendExists(1L, "Alice")).thenReturn(false);

        handler.handleState(update("Alice"));

        verify(userStateService).setPendingName(1L, "Alice");
        verify(userStateService).setState(1L, BotState.WAITING_FOR_ADD_FRIEND_DATE);
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
