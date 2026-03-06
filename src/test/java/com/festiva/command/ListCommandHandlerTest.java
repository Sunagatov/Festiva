package com.festiva.command;

import com.festiva.command.handler.ListCommandHandler;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.state.UserStateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("ListCommandHandler")
@ExtendWith(MockitoExtension.class)
class ListCommandHandlerTest {

    @Mock FriendService friendService;
    @Mock UserStateService userStateService;
    @InjectMocks ListCommandHandler handler;

    @Test
    @DisplayName("empty friend list → returns friends-empty message")
    void emptyList_returnsFriendsEmptyMessage() {
        when(userStateService.getLanguage(1L)).thenReturn(Lang.EN);
        when(friendService.getFriendsSortedByDayMonth(1L)).thenReturn(List.of());

        SendMessage result = handler.handle(update());

        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.FRIENDS_EMPTY));
    }

    @Test
    @DisplayName("friend whose birthday already passed this year → shows 'turned' label")
    void birthdayPassed_showsTurnedLabel() {
        LocalDate today = LocalDate.now();
        Friend friend = new Friend("Alice", today.minusDays(1).minusYears(30));
        when(userStateService.getLanguage(1L)).thenReturn(Lang.EN);
        when(friendService.getFriendsSortedByDayMonth(1L)).thenReturn(List.of(friend));

        String text = handler.handle(update()).getText();

        assertThat(text).contains("Alice");
        assertThat(text).containsPattern("turned.*30|30.*turned");
    }

    @Test
    @DisplayName("friend whose birthday is still ahead this year → shows 'will turn' label")
    void birthdayAhead_showsWillTurnLabel() {
        LocalDate today = LocalDate.now();
        Friend friend = new Friend("Bob", today.plusDays(1).minusYears(30));
        when(userStateService.getLanguage(1L)).thenReturn(Lang.EN);
        when(friendService.getFriendsSortedByDayMonth(1L)).thenReturn(List.of(friend));

        String text = handler.handle(update()).getText();

        assertThat(text).contains("Bob");
        assertThat(text).containsPattern("turns.*30|30.*turns");
    }

    private Update update() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        Message message = mock(Message.class);
        when(message.getFrom()).thenReturn(user);
        when(message.getChatId()).thenReturn(1L);
        Update update = mock(Update.class);
        when(update.getMessage()).thenReturn(message);
        return update;
    }
}
