package com.festiva.command;

import com.festiva.command.handler.UpcomingBirthdaysCommandHandler;
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

@DisplayName("UpcomingBirthdaysCommandHandler")
@ExtendWith(MockitoExtension.class)
class UpcomingBirthdaysCommandHandlerTest {

    @Mock FriendService friendService;
    @Mock UserStateService userStateService;
    @InjectMocks UpcomingBirthdaysCommandHandler handler;

    @Test
    @DisplayName("friend with birthday in 5 days → appears in upcoming list")
    void friendWithin30Days_appearsInList() {
        LocalDate today = LocalDate.now();
        Friend friend = new Friend("Alice", today.plusDays(5).minusYears(25));
        when(userStateService.getLanguage(1L)).thenReturn(Lang.EN);
        when(friendService.getFriends(1L)).thenReturn(List.of(friend));

        assertThat(handler.handle(update(1L)).getText()).contains("Alice");
    }

    @Test
    @DisplayName("friend with birthday in 31 days → excluded from upcoming list")
    void friendOutside30Days_excludedFromList() {
        LocalDate today = LocalDate.now();
        Friend friend = new Friend("Bob", today.plusDays(31).minusYears(25));
        when(userStateService.getLanguage(1L)).thenReturn(Lang.EN);
        when(friendService.getFriends(1L)).thenReturn(List.of(friend));

        String text = handler.handle(update(1L)).getText();

        assertThat(text).doesNotContain("Bob");
        assertThat(text).contains(Messages.get(Lang.EN, Messages.UPCOMING_NONE, 30));
    }

    @Test
    @DisplayName("no friends → returns upcoming-none message")
    void noFriends_returnsUpcomingNone() {
        when(userStateService.getLanguage(1L)).thenReturn(Lang.EN);
        when(friendService.getFriends(1L)).thenReturn(List.of());

        assertThat(handler.handle(update(1L)).getText())
                .contains(Messages.get(Lang.EN, Messages.UPCOMING_NONE, 30));
    }

    private Update update(long userId) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        Message message = mock(Message.class);
        when(message.getFrom()).thenReturn(user);
        when(message.getChatId()).thenReturn(userId);
        Update update = mock(Update.class);
        when(update.getMessage()).thenReturn(message);
        return update;
    }
}
