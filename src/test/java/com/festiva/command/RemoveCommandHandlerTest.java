package com.festiva.command;

import com.festiva.command.handler.RemoveCommandHandler;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.state.BotState;
import com.festiva.state.UserStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("RemoveCommandHandler")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RemoveCommandHandlerTest {

    @Mock FriendService friendService;
    @Mock UserStateService userStateService;
    @InjectMocks RemoveCommandHandler handler;

    @BeforeEach
    void defaultLang() {
        lenient().when(userStateService.getLanguage(anyLong())).thenReturn(Lang.EN);
    }

    @Test
    @DisplayName("/remove with friends → sets state to WAITING_FOR_REMOVE and returns keyboard")
    void handle_withFriends_setsStateAndReturnsKeyboard() {
        when(friendService.getFriendsSortedByDayMonth(1L))
                .thenReturn(List.of(new Friend("Alice", LocalDate.of(1990, 1, 1))));

        SendMessage result = handler.handle(update("1", "/remove"));

        verify(userStateService).setState(1L, BotState.WAITING_FOR_REMOVE_FRIEND_INPUT);
        assertThat(result.getReplyMarkup()).isNotNull();
    }

    @Test
    @DisplayName("/remove with no friends → returns friends-empty message, no state change")
    void handle_noFriends_returnsFriendsEmpty() {
        when(friendService.getFriendsSortedByDayMonth(1L)).thenReturn(List.of());

        SendMessage result = handler.handle(update("1", "/remove"));

        verify(userStateService, never()).setState(anyLong(), any());
        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.FRIENDS_EMPTY));
    }

    @Test
    @DisplayName("handleState() with existing friend → removes and confirms")
    void handleState_existingFriend_removesAndConfirms() {
        when(friendService.friendExists(1L, "Alice")).thenReturn(true);

        SendMessage result = handler.handleState(update("1", "Alice"));

        verify(friendService).deleteFriend(1L, "Alice");
        assertThat(result.getText()).contains("Alice");
    }

    @Test
    @DisplayName("handleState() with unknown friend → returns not-found, no deletion")
    void handleState_unknownFriend_returnsNotFound() {
        when(friendService.friendExists(1L, "Ghost")).thenReturn(false);

        SendMessage result = handler.handleState(update("1", "Ghost"));

        verify(friendService, never()).deleteFriend(anyLong(), anyString());
        assertThat(result.getText()).contains("Ghost");
    }

    @Test
    @DisplayName("handleState() with blank input → returns name-empty message")
    void handleState_blankInput_returnsNameEmpty() {
        SendMessage result = handler.handleState(update("1", "   "));
        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.NAME_EMPTY));
    }

    private Update update(String userId, String text) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(Long.parseLong(userId));
        Message message = mock(Message.class);
        when(message.getFrom()).thenReturn(user);
        when(message.getChatId()).thenReturn(Long.parseLong(userId));
        when(message.getText()).thenReturn(text);
        Update update = mock(Update.class);
        when(update.getMessage()).thenReturn(message);
        return update;
    }
}
