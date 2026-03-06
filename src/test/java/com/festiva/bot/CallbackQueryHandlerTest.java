package com.festiva.bot;

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
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.MaybeInaccessibleMessage;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("CallbackQueryHandler")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CallbackQueryHandlerTest {

    @Mock FriendService friendService;
    @Mock UserStateService userStateService;
    @InjectMocks CallbackQueryHandler handler;

    @BeforeEach
    void defaultLang() {
        lenient().when(userStateService.getLanguage(anyLong())).thenReturn(Lang.EN);
    }

    @Test
    @DisplayName("LANG_EN callback — sets language to EN and returns confirmation")
    void langCallback_setsLanguageAndReturnsConfirmation() {
        EditMessageText result = handler.handle(callback(1L, "LANG_EN"));
        verify(userStateService).setLanguage(1L, Lang.EN);
        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.LANGUAGE_SET));
    }

    @Test
    @DisplayName("LANG_RU callback — sets language to RU and returns RU confirmation")
    void langCallback_ru_setsRuLanguage() {
        when(userStateService.getLanguage(1L)).thenReturn(Lang.RU);
        EditMessageText result = handler.handle(callback(1L, "LANG_RU"));
        verify(userStateService).setLanguage(1L, Lang.RU);
        assertThat(result.getText()).contains(Messages.get(Lang.RU, Messages.LANGUAGE_SET));
    }

    @Test
    @DisplayName("LANG_INVALID callback — returns unknown command message, does not crash")
    void langCallback_invalid_returnsUnknownCommand() {
        EditMessageText result = handler.handle(callback(1L, "LANG_INVALID"));
        verify(userStateService, never()).setLanguage(anyLong(), any());
        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.UNKNOWN_COMMAND));
    }

    @Test
    @DisplayName("REMOVE_ callback — deletes friend and returns removed confirmation")
    void removeCallback_deletesFriendAndConfirms() {
        when(friendService.friendExists(1L, "Alice")).thenReturn(true);
        EditMessageText result = handler.handle(callback(1L, "REMOVE_Alice"));
        verify(friendService).deleteFriend(1L, "Alice");
        assertThat(result.getText()).contains("Alice");
    }

    @Test
    @DisplayName("REMOVE_ callback — friend not found returns not-found message")
    void removeCallback_friendNotFound() {
        when(friendService.friendExists(1L, "Ghost")).thenReturn(false);
        EditMessageText result = handler.handle(callback(1L, "REMOVE_Ghost"));
        verify(friendService, never()).deleteFriend(anyLong(), anyString());
        assertThat(result.getText()).contains("Ghost");
    }

    @Test
    @DisplayName("MONTH_6 callback — returns friends born in June")
    void monthCallback_returnsFilteredFriends() {
        Friend june = new Friend("Alice", LocalDate.of(1990, 6, 15));
        Friend dec  = new Friend("Bob",   LocalDate.of(1990, 12, 1));
        when(friendService.getFriendsSortedByDayMonth(1L)).thenReturn(List.of(june, dec));

        EditMessageText result = handler.handle(callback(1L, "MONTH_6"));

        assertThat(result.getText()).contains("Alice").doesNotContain("Bob");
    }

    @Test
    @DisplayName("MONTH_CURRENT callback — resolves to current month, returns no-birthdays message")
    void monthCallback_current_resolvesWithoutError() {
        when(friendService.getFriendsSortedByDayMonth(1L)).thenReturn(List.of());
        EditMessageText result = handler.handle(callback(1L, "MONTH_CURRENT"));
        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.BIRTHDAYS_NONE,
                Month.of(LocalDate.now().getMonthValue())
                        .getDisplayName(TextStyle.FULL_STANDALONE, Lang.EN.locale())));
    }

    @Test
    @DisplayName("unknown callback prefix — returns null")
    void unknownCallback_returnsNull() {
        assertThat(handler.handle(callback(1L, "UNKNOWN_data"))).isNull();
    }

    @Test
    @DisplayName("null CallbackQuery — returns null without throwing")
    void nullCallback_returnsNull() {
        assertThat(handler.handle(null)).isNull();
    }

    // --- helper ---

    private CallbackQuery callback(long userId, String data) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);

        MaybeInaccessibleMessage message = mock(MaybeInaccessibleMessage.class);
        when(message.getChatId()).thenReturn(userId);
        when(message.getMessageId()).thenReturn(1);

        CallbackQuery cq = mock(CallbackQuery.class);
        when(cq.getFrom()).thenReturn(user);
        when(cq.getData()).thenReturn(data);
        when(cq.getMessage()).thenReturn(message);
        return cq;
    }
}
