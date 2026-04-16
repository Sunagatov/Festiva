package com.festiva.command;

import com.festiva.command.handler.TodayCommandHandler;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.i18n.MessagesTestSupport;
import com.festiva.state.UserStateService;
import com.festiva.util.UserDateService;
import org.junit.jupiter.api.BeforeEach;
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

@DisplayName("TodayCommandHandler")
@ExtendWith(MockitoExtension.class)
class TodayCommandHandlerTest extends MessagesTestSupport {

    @Mock FriendService friendService;
    @Mock UserStateService userStateService;
    @Mock UserDateService userDateService;
    @InjectMocks TodayCommandHandler handler;

    @BeforeEach
    void defaultLang() {
        lenient().when(userStateService.getLanguage(anyLong())).thenReturn(Lang.EN);
        lenient().when(userDateService.todayFor(anyLong())).thenReturn(LocalDate.now());
    }

    @Test
    @DisplayName("no birthdays today → returns today-none message")
    void noBirthdaysToday_returnsNoneMessage() {
        when(friendService.getFriends(1L)).thenReturn(List.of(
                new Friend("Alice", LocalDate.now().plusDays(1).minusYears(30))));

        SendMessage result = handler.handle(update());

        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.TODAY_NONE));
    }

    @Test
    @DisplayName("birthday today → returns friend's name in message")
    void birthdayToday_returnsFriendName() {
        when(friendService.getFriends(1L)).thenReturn(List.of(
                new Friend("Bob", LocalDate.now().minusYears(25))));

        SendMessage result = handler.handle(update());

        assertThat(result.getText()).contains("Bob");
    }

    @Test
    @DisplayName("birthday today → result contains next-step hint")
    void birthdayToday_containsNextStepHint() {
        when(friendService.getFriends(1L)).thenReturn(List.of(
                new Friend("Bob", LocalDate.now().minusYears(25))));
        assertThat(handler.handle(update()).getText()).contains("/list");
    }

    @Test
    @DisplayName("no birthdays today RU → returns RU today-none message")
    void noBirthdaysToday_ru_returnsRuNone() {
        when(userStateService.getLanguage(anyLong())).thenReturn(Lang.RU);
        when(friendService.getFriends(1L)).thenReturn(List.of());
        assertThat(handler.handle(update()).getText())
                .contains(Messages.get(Lang.RU, Messages.TODAY_NONE));
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
