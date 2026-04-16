package com.festiva.command;

import com.festiva.command.handler.SearchCommandHandler;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.i18n.MessagesTestSupport;
import com.festiva.state.BotState;
import com.festiva.state.UserStateService;
import com.festiva.util.UserDateService;
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
    @Mock UserDateService userDateService;
    @InjectMocks SearchCommandHandler handler;

    @BeforeEach
    void defaultLang() {
        lenient().when(userStateService.getLanguage(anyLong())).thenReturn(Lang.EN);
        lenient().when(userDateService.todayFor(anyLong())).thenReturn(LocalDate.now());
    }

    @Test
    @DisplayName("handle → sets WAITING_FOR_SEARCH state and returns prompt")
    void handle_setsStateAndReturnsPrompt() {
        var result = handler.handle(update(""));
        verify(userStateService).setState(1L, BotState.WAITING_FOR_SEARCH);
        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.SEARCH_PROMPT));
    }

    @Test
    @DisplayName("handle → prompt contains /cancel hint")
    void handle_prompt_containsCancelHint() {
        assertThat(handler.handle(update("")).getText()).contains("/cancel");
    }

    @Test
    @DisplayName("handleState blank input → re-shows prompt, state stays open")
    void handleState_blank_reshowsPrompt() {
        var result = handler.handleState(update("   "));
        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.SEARCH_PROMPT));
        verify(userStateService, never()).clearState(anyLong());
    }

    @Test
    @DisplayName("handleState too-long query → returns search_too_long, state preserved")
    void handleState_tooLong_returnsError() {
        var result = handler.handleState(update("A".repeat(101)));
        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.SEARCH_TOO_LONG));
        verify(userStateService, never()).clearState(anyLong());
    }

    @Test
    @DisplayName("handleState no match → state kept open for retry")
    void handleState_noMatch_stateKeptOpen() {
        when(friendService.getFriendsSortedByDayMonth(1L)).thenReturn(
                List.of(new Friend("Bob", LocalDate.now().minusYears(25))));
        handler.handleState(update("xyz"));
        verify(userStateService).setState(1L, BotState.WAITING_FOR_SEARCH);
    }

    @Test
    @DisplayName("handleState match → clears state")
    void handleState_match_clearsState() {
        when(friendService.getFriendsSortedByDayMonth(1L)).thenReturn(
                List.of(new Friend("Alice", LocalDate.now().minusYears(25))));
        handler.handleState(update("alice"));
        verify(userStateService).clearState(1L);
    }

    @Test
    @DisplayName("handleState match → results contain next-step hint")
    void handleState_match_containsNextStepHint() {
        when(friendService.getFriendsSortedByDayMonth(1L)).thenReturn(
                List.of(new Friend("Alice", LocalDate.now().minusYears(25))));
        assertThat(handler.handleState(update("alice")).getText())
                .contains(Messages.get(Lang.EN, Messages.SEARCH_RESULTS_HINT));
    }

    @Test
    @DisplayName("handleState match → original query casing preserved in results header")
    void handleState_match_preservesQueryCasing() {
        when(friendService.getFriendsSortedByDayMonth(1L)).thenReturn(
                List.of(new Friend("Alice", LocalDate.now().minusYears(25))));
        assertThat(handler.handleState(update("ALI")).getText())
                .contains(Messages.get(Lang.EN, Messages.SEARCH_RESULTS, "ALI"));
    }

    @Test
    @DisplayName("handleState RU no match → returns RU search_none")
    void handleState_ru_noMatch_returnsRuMessage() {
        when(userStateService.getLanguage(anyLong())).thenReturn(Lang.RU);
        when(friendService.getFriendsSortedByDayMonth(1L)).thenReturn(List.of());
        assertThat(handler.handleState(update("xyz")).getText())
                .contains(Messages.get(Lang.RU, Messages.SEARCH_NONE, "xyz"));
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