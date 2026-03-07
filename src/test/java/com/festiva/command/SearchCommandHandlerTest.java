package com.festiva.command;

import com.festiva.command.handler.SearchCommandHandler;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
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

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("SearchCommandHandler")
@ExtendWith(MockitoExtension.class)
class SearchCommandHandlerTest extends MessagesTestSupport {

    @Mock FriendService friendService;
    @Mock UserStateService userStateService;
    @InjectMocks SearchCommandHandler handler;

    @BeforeEach
    void defaultLang() {
        lenient().when(userStateService.getLanguage(anyLong())).thenReturn(Lang.EN);
    }

    @Test
    @DisplayName("handle → sets WAITING_FOR_SEARCH state and returns prompt")
    void handle_setsStateAndReturnsPrompt() {
        var result = handler.handle(update(""));
        verify(userStateService).setState(1L, BotState.WAITING_FOR_SEARCH);
        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.SEARCH_PROMPT));
    }

    @Test
    @DisplayName("handleState with matching name → returns friend in results")
    void handleState_matchingName_returnsFriend() {
        when(friendService.getFriendsSortedByDayMonth(1L)).thenReturn(
                List.of(new Friend("Alice", LocalDate.now().minusYears(25))));

        var result = handler.handleState(update("ali"));
        assertThat(result.getText()).contains("Alice");
    }

    @Test
    @DisplayName("handleState with no match → returns search-none message")
    void handleState_noMatch_returnsNone() {
        when(friendService.getFriendsSortedByDayMonth(1L)).thenReturn(
                List.of(new Friend("Bob", LocalDate.now().minusYears(25))));

        var result = handler.handleState(update("xyz"));
        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.SEARCH_NONE, "xyz"));
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
