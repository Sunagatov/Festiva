package com.festiva.command;

import com.festiva.command.handler.BulkAddCommandHandler;
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
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("BulkAddCommandHandler")
@ExtendWith(MockitoExtension.class)
class BulkAddCommandHandlerTest extends MessagesTestSupport {

    @Mock FriendService friendService;
    @Mock UserStateService userStateService;
    @Mock TelegramClient telegramClient;
    @InjectMocks BulkAddCommandHandler handler;

    @BeforeEach
    void defaults() {
        lenient().when(userStateService.getLanguage(anyLong())).thenReturn(Lang.EN);
        lenient().when(friendService.getFriends(anyLong())).thenReturn(List.of());
    }

    @Test
    @DisplayName("promptPaste → sets WAITING_FOR_BULK_ADD and returns prompt with /cancel")
    void promptPaste_setsStateAndContainsCancelHint() {
        var result = handler.promptPaste(1L, 1L, Lang.EN);
        verify(userStateService).setState(1L, BotState.WAITING_FOR_BULK_ADD);
        assertThat(result.getText()).contains("/cancel");
    }

    @Test
    @DisplayName("handleState with empty text → returns no-data error, state preserved")
    void handleState_emptyText_returnsNoDataError() {
        var result = handler.handleState(update("   "));
        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.BULK_ERROR_NO_DATA));
        verify(userStateService, never()).clearState(anyLong());
    }

    @Test
    @DisplayName("handleState with header-only CSV → returns no-data error, state preserved")
    void handleState_headerOnly_returnsNoDataError() {
        var result = handler.handleState(update("name,birthday,relationship"));
        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.BULK_ERROR_NO_DATA));
        verify(userStateService, never()).clearState(anyLong());
    }

    @Test
    @DisplayName("handleState with valid entries → adds friends, clears state, returns success")
    void handleState_validEntries_addsFriendsAndClearsState() {
        var result = handler.handleState(update("Alice,15.03.1990\nBob,22.07.1985"));
        verify(friendService, times(2)).addFriend(eq(1L), any(Friend.class));
        verify(userStateService).clearState(1L);
        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.BULK_ADD_SUCCESS, 2));
    }

    @Test
    @DisplayName("handleState success → message contains next-step hint")
    void handleState_success_containsNextStepHint() {
        var result = handler.handleState(update("Alice,15.03.1990"));
        assertThat(result.getText()).contains("/list");
    }

    @Test
    @DisplayName("handleState with errors → message contains errors section with /addmany hint")
    void handleState_withErrors_containsErrorsAndHint() {
        var result = handler.handleState(update("Alice,15.03.1990\nbad-line"));
        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.BULK_ADD_SUCCESS, 1));
        assertThat(result.getText()).contains("/addmany");
    }

    @Test
    @DisplayName("handleState at friend cap → caps additions and adds cap error")
    void handleState_atCap_capsAndAddsCap() {
        List<Friend> full = new java.util.ArrayList<>();
        for (int i = 0; i < FriendService.FRIEND_CAP; i++) {
            full.add(new Friend("Person" + i, LocalDate.of(1990, 1, 1)));
        }
        when(friendService.getFriends(1L)).thenReturn(full);
        var result = handler.handleState(update("Alice,15.03.1990"));
        verify(friendService, never()).addFriend(anyLong(), any());
        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.BULK_CAP_EXCEEDED, 0, FriendService.FRIEND_CAP));
    }

    @Test
    @DisplayName("handleState RU valid entry → returns RU success message")
    void handleState_ru_returnsRuSuccess() {
        when(userStateService.getLanguage(anyLong())).thenReturn(Lang.RU);
        var result = handler.handleState(update("Alice,15.03.1990"));
        assertThat(result.getText()).contains(Messages.get(Lang.RU, Messages.BULK_ADD_SUCCESS, 1));
    }

    private Update update(String text) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        Message message = mock(Message.class);
        when(message.getFrom()).thenReturn(user);
        when(message.getChatId()).thenReturn(1L);
        when(message.hasText()).thenReturn(true);
        when(message.hasDocument()).thenReturn(false);
        lenient().when(message.getText()).thenReturn(text);
        Update update = mock(Update.class);
        when(update.getMessage()).thenReturn(message);
        return update;
    }
}
