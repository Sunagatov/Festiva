package com.festiva.command;

import com.festiva.command.handler.RemoveCommandHandler;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
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
    @DisplayName("/remove with friends → returns inline keyboard with friend names")
    void handle_withFriends_returnsKeyboard() {
        when(friendService.getFriendsSortedByDayMonth(1L))
                .thenReturn(List.of(new Friend("Alice", LocalDate.of(1990, 1, 1))));

        SendMessage result = handler.handle(update("/remove"));

        assertThat(result.getReplyMarkup()).isNotNull();
    }

    @Test
    @DisplayName("/remove with no friends → returns friends-empty message")
    void handle_noFriends_returnsFriendsEmpty() {
        when(friendService.getFriendsSortedByDayMonth(1L)).thenReturn(List.of());

        SendMessage result = handler.handle(update("/remove"));

        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.FRIENDS_EMPTY));
    }

    private Update update(String text) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        Message message = mock(Message.class);
        when(message.getFrom()).thenReturn(user);
        when(message.getChatId()).thenReturn(1L);
        when(message.getText()).thenReturn(text);
        Update update = mock(Update.class);
        when(update.getMessage()).thenReturn(message);
        return update;
    }
}
