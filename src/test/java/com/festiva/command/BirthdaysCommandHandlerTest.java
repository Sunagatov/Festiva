package com.festiva.command;

import com.festiva.command.handler.BirthdaysCommandHandler;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("BirthdaysCommandHandler")
@ExtendWith(MockitoExtension.class)
class BirthdaysCommandHandlerTest extends MessagesTestSupport {

    @Mock FriendService friendService;
    @Mock UserStateService userStateService;
    @InjectMocks BirthdaysCommandHandler handler;

    @BeforeEach
    void defaults() {
        lenient().when(userStateService.getLanguage(anyLong())).thenReturn(Lang.EN);
    }

    @Test
    @DisplayName("handle → response contains birthdays-pick prompt")
    void handle_containsPickPrompt() {
        when(friendService.getFriends(1L)).thenReturn(List.of());
        assertThat(handler.handle(update()).getText())
                .contains(Messages.get(Lang.EN, Messages.BIRTHDAYS_PICK));
    }

    @Test
    @DisplayName("handle → keyboard has 4 rows (current-month row + 3 month rows of 4)")
    void handle_keyboardHas4Rows() {
        when(friendService.getFriends(1L)).thenReturn(List.of());
        var keyboard = (org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup)
                handler.handle(update()).getReplyMarkup();
        assertThat(keyboard.getKeyboard()).hasSize(4);
    }

    @Test
    @DisplayName("friend in January → January button shows count (1)")
    void friendInJanuary_buttonShowsCount() {
        when(friendService.getFriends(1L)).thenReturn(List.of(
                new Friend("Alice", LocalDate.of(1990, 1, 15))));

        var keyboard = (org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup)
                handler.handle(update()).getReplyMarkup();
        var allLabels = keyboard.getKeyboard().stream()
                .flatMap(Collection::stream)
                .map(InlineKeyboardButton::getText)
                .collect(Collectors.toList());
        assertThat(allLabels).anyMatch(label -> label.contains("(1)") && label.toLowerCase(Locale.ROOT).contains("jan"));
    }

    @Test
    @DisplayName("handle RU → prompt in Russian")
    void handle_ru_containsRuPrompt() {
        when(userStateService.getLanguage(anyLong())).thenReturn(Lang.RU);
        when(friendService.getFriends(1L)).thenReturn(List.of());
        assertThat(handler.handle(update()).getText())
                .contains(Messages.get(Lang.RU, Messages.BIRTHDAYS_PICK));
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
