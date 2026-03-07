package com.festiva.command;

import com.festiva.command.handler.StatsCommandHandler;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.i18n.Lang;
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

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("StatsCommandHandler")
@ExtendWith(MockitoExtension.class)
class StatsCommandHandlerTest extends MessagesTestSupport {

    @Mock FriendService friendService;
    @Mock UserStateService userStateService;
    @InjectMocks StatsCommandHandler handler;

    @BeforeEach
    void defaultLang() {
        lenient().when(userStateService.getLanguage(anyLong())).thenReturn(Lang.EN);
    }

    @Test
    @DisplayName("with friends → response contains friend count and next birthday name")
    void withFriends_containsCountAndNextName() {
        LocalDate today = LocalDate.now();
        when(friendService.getFriends(1L)).thenReturn(List.of(
                new Friend("Alice", today.plusDays(1).minusYears(30)),
                new Friend("Bob", today.plusDays(10).minusYears(25))));

        String text = handler.handle(update()).getText();
        assertThat(text).contains("2");
        assertThat(text).contains("Alice");
    }

    @Test
    @DisplayName("no friends → response contains 0 and dash for next birthday")
    void noFriends_containsZeroAndDash() {
        when(friendService.getFriends(1L)).thenReturn(List.of());

        String text = handler.handle(update()).getText();
        assertThat(text).contains("0");
        assertThat(text).contains("—");
    }

    @Test
    @DisplayName("friend with birthday today → shown with cake emoji")
    void birthdayToday_shownWithCakeEmoji() {
        when(friendService.getFriends(1L)).thenReturn(List.of(
                new Friend("Carol", LocalDate.now().minusYears(20))));

        String text = handler.handle(update()).getText();
        assertThat(text).contains("Carol");
        assertThat(text).contains("🎂");
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
