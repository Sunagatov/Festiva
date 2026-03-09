package com.festiva.command;

import com.festiva.command.handler.JubileeCommandHandler;
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
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("JubileeCommandHandler")
@ExtendWith(MockitoExtension.class)
class JubileeCommandHandlerTest extends com.festiva.i18n.MessagesTestSupport {

    @Mock FriendService friendService;
    @Mock UserStateService userStateService;
    @InjectMocks JubileeCommandHandler handler;

    @Test
    @DisplayName("friend turning a multiple of 5 → appears in jubilee list")
    void jubileeFriend_appearsInList() {
        LocalDate today = LocalDate.now();
        // getAge()=29, getNextAge()=30 (multiple of 5)
        Friend friend = new Friend("Alice", today.plusDays(1).minusYears(30));
        when(userStateService.getLanguage(1L)).thenReturn(Lang.EN);
        when(friendService.getFriends(1L)).thenReturn(List.of(friend));

        assertThat(handler.handle(update()).getText()).contains("Alice");
    }

    @Test
    @DisplayName("friend turning a non-multiple of 5 → excluded from jubilee list")
    void nonJubileeFriend_excludedFromList() {
        LocalDate today = LocalDate.now();
        // turns 31 on next birthday (not a multiple of 5)
        Friend friend = new Friend("Bob", today.minusDays(1).minusYears(30));
        when(userStateService.getLanguage(1L)).thenReturn(Lang.EN);
        when(friendService.getFriends(1L)).thenReturn(List.of(friend));

        String text = handler.handle(update()).getText();

        assertThat(text).doesNotContain("Bob");
        assertThat(text).contains(Messages.get(Lang.EN, Messages.JUBILEE_NONE));
    }

    @Test
    @DisplayName("no friends → returns friends-empty message")
    void noFriends_returnsFriendsEmpty() {
        when(userStateService.getLanguage(1L)).thenReturn(Lang.EN);
        when(friendService.getFriends(1L)).thenReturn(List.of());

        assertThat(handler.handle(update()).getText())
                .contains(Messages.get(Lang.EN, Messages.FRIENDS_EMPTY));
    }

    @Test
    @DisplayName("no jubilees → jubilee-none contains /upcomingbirthdays hint")
    void noJubilees_containsUpcomingHint() {
        LocalDate today = LocalDate.now();
        Friend friend = new Friend("Bob", today.minusDays(1).minusYears(30));
        when(userStateService.getLanguage(1L)).thenReturn(Lang.EN);
        when(friendService.getFriends(1L)).thenReturn(List.of(friend));
        assertThat(handler.handle(update()).getText()).contains("/upcomingbirthdays");
    }

    @Test
    @DisplayName("jubilee friend RU → returns RU message")
    void jubileeFriend_ru_returnsRuMessage() {
        LocalDate today = LocalDate.now();
        Friend friend = new Friend("Alice", today.plusDays(1).minusYears(30));
        when(userStateService.getLanguage(1L)).thenReturn(Lang.RU);
        when(friendService.getFriends(1L)).thenReturn(List.of(friend));
        assertThat(handler.handle(update()).getText()).contains("Alice");
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
