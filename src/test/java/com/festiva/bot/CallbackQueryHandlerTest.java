package com.festiva.bot;

import com.festiva.bot.DatePickerCallbackHandler;
import com.festiva.bot.EditCallbackHandler;
import com.festiva.command.handler.BulkAddCommandHandler;
import com.festiva.command.handler.DeleteAccountCommandHandler;
import com.festiva.command.handler.EditFriendCommandHandler;
import com.festiva.command.handler.ListCommandHandler;
import com.festiva.command.handler.RemoveCommandHandler;
import com.festiva.command.handler.UpcomingBirthdaysCommandHandler;
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
class CallbackQueryHandlerTest extends com.festiva.i18n.MessagesTestSupport {

    @Mock FriendService friendService;
    @Mock UserStateService userStateService;
    @Mock DeleteAccountCommandHandler deleteAccountHandler;
    @Mock UpcomingBirthdaysCommandHandler upcomingHandler;
    @Mock ListCommandHandler listHandler;
    @Mock BulkAddCommandHandler bulkAddHandler;
    @Mock DatePickerCallbackHandler datePickerHandler;
    @Mock EditCallbackHandler editHandler;
    @Mock RemoveCommandHandler removeCommandHandler;
    @Mock EditFriendCommandHandler editFriendCommandHandler;
    @InjectMocks CallbackQueryHandler handler;

    @BeforeEach
    void defaultLang() {
        lenient().when(userStateService.getLanguage(anyLong())).thenReturn(Lang.EN);
    }

    @Test
    @DisplayName("LANG_EN callback → confirmation contains next-step hint")
    void langCallback_en_containsNextStepHint() {
        EditMessageText result = handler.handle(callback("LANG_EN"));
        assertThat(result.getText()).contains("/language");
    }

    @Test
    @DisplayName("SETTINGS_HOUR_ callback → sets hour and returns confirmation with next-step hint")
    void settingsHourCallback_setsHourAndContainsHint() {
        when(userStateService.getNotifyHour(1L)).thenReturn(9);
        when(userStateService.getTimezone(1L)).thenReturn("UTC");
        EditMessageText result = handler.handle(callback("SETTINGS_HOUR_9"));
        verify(userStateService).setNotifyHour(1L, 9);
        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.SETTINGS_HOUR_SET, 9));
        assertThat(result.getText()).contains("/settings");
    }

    @Test
    @DisplayName("SETTINGS_TZ_ callback → sets timezone and returns confirmation with next-step hint")
    void settingsTzCallback_setsTzAndContainsHint() {
        when(userStateService.getNotifyHour(1L)).thenReturn(9);
        when(userStateService.getTimezone(1L)).thenReturn("UTC");
        EditMessageText result = handler.handle(callback("SETTINGS_TZ_UTC"));
        verify(userStateService).setTimezone(1L, "UTC");
        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.SETTINGS_TZ_SET, "UTC"));
        assertThat(result.getText()).contains("/settings");
    }

    @Test
    @DisplayName("LANG_EN callback — sets language to EN and returns confirmation")
    void langCallback_setsLanguageAndReturnsConfirmation() {
        EditMessageText result = handler.handle(callback("LANG_EN"));
        verify(userStateService).setLanguage(1L, Lang.EN);
        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.LANGUAGE_SET));
    }

    @Test
    @DisplayName("LANG_RU callback — sets language to RU and returns RU confirmation")
    void langCallback_ru_setsRuLanguage() {
        when(userStateService.getLanguage(1L)).thenReturn(Lang.RU);
        EditMessageText result = handler.handle(callback("LANG_RU"));
        verify(userStateService).setLanguage(1L, Lang.RU);
        assertThat(result.getText()).contains(Messages.get(Lang.RU, Messages.LANGUAGE_SET));
    }

    @Test
    @DisplayName("LANG_INVALID callback → returns SESSION_EXPIRED, does not crash")
    void langCallback_invalid_returnsSessionExpired() {
        EditMessageText result = handler.handle(callback("LANG_INVALID"));
        verify(userStateService, never()).setLanguage(anyLong(), any());
        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.SESSION_EXPIRED));
    }

    @Test
    @DisplayName("REMOVE_ callback → shows confirmation prompt with Yes/No buttons")
    void removeCallback_showsConfirmation() {
        Friend alice = new Friend("Alice", java.time.LocalDate.of(1990, 1, 1));
        alice.setId("id-alice");
        alice.setTelegramUserId(1L);
        when(friendService.findFriendById("id-alice")).thenReturn(java.util.Optional.of(alice));
        EditMessageText result = handler.handle(callback("REMOVE_id-alice"));
        verify(friendService, never()).deleteFriend(anyLong(), anyString());
        assertThat(result.getText()).contains("Alice");
        assertThat(result.getReplyMarkup()).isNotNull();
    }

    @Test
    @DisplayName("CONFIRM_REMOVE_ callback → deletes friend and returns removed confirmation")
    void confirmRemoveCallback_deletesFriendAndConfirms() {
        Friend alice = new Friend("Alice", java.time.LocalDate.of(1990, 1, 1));
        alice.setId("id-alice");
        alice.setTelegramUserId(1L);
        when(friendService.findFriendById("id-alice")).thenReturn(java.util.Optional.of(alice));
        EditMessageText result = handler.handle(callback("CONFIRM_REMOVE_id-alice"));
        verify(friendService).deleteFriend(1L, "Alice");
        assertThat(result.getText()).contains("Alice");
    }

    @Test
    @DisplayName("REMOVE_ callback → stale friend returns SESSION_EXPIRED")
    void removeCallback_stale_returnsSessionExpired() {
        when(friendService.findFriendById("ghost")).thenReturn(java.util.Optional.empty());
        EditMessageText result = handler.handle(callback("REMOVE_ghost"));
        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.SESSION_EXPIRED));
    }

    @Test
    @DisplayName("CONFIRM_REMOVE_ callback → stale friend returns SESSION_EXPIRED")
    void confirmRemoveCallback_stale_returnsSessionExpired() {
        when(friendService.findFriendById("id-ghost")).thenReturn(java.util.Optional.empty());
        EditMessageText result = handler.handle(callback("CONFIRM_REMOVE_id-ghost"));
        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.SESSION_EXPIRED));
    }

    @Test
    @DisplayName("CONFIRM_REMOVE_ callback → success message contains next-step hint")
    void confirmRemoveCallback_success_containsNextStepHint() {
        Friend alice = new Friend("Alice", java.time.LocalDate.of(1990, 1, 1));
        alice.setId("id-alice");
        alice.setTelegramUserId(1L);
        when(friendService.findFriendById("id-alice")).thenReturn(java.util.Optional.of(alice));
        EditMessageText result = handler.handle(callback("CONFIRM_REMOVE_id-alice"));
        assertThat(result.getText()).contains("/list");
    }

    @Test
    @DisplayName("CANCEL_REMOVE callback — clears state and returns cancelled message")
    void cancelRemoveCallback_clearsState() {
        EditMessageText result = handler.handle(callback("CANCEL_REMOVE"));
        verify(userStateService).clearState(1L);
        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.CONFIRM_REMOVE_CANCEL));
    }

    @Test
    @DisplayName("MONTH_ callback with no friends in that month → birthdays_none contains next-step hint")
    void monthCallback_noFriends_noneContainsHint() {
        when(friendService.getFriendsSortedByDayMonth(1L)).thenReturn(List.of());
        EditMessageText result = handler.handle(callback("MONTH_6"));
        assertThat(result.getText()).contains("/add");
    }

    @Test
    @DisplayName("MONTH_6 callback — returns friends born in June")
    void monthCallback_returnsFilteredFriends() {
        Friend june = new Friend("Alice", LocalDate.of(1990, 6, 15));
        Friend dec  = new Friend("Bob",   LocalDate.of(1990, 12, 1));
        when(friendService.getFriendsSortedByDayMonth(1L)).thenReturn(List.of(june, dec));

        EditMessageText result = handler.handle(callback("MONTH_6"));

        assertThat(result.getText()).contains("Alice").doesNotContain("Bob");
    }

    @Test
    @DisplayName("MONTH_CURRENT callback — resolves to current month, returns no-birthdays message")
    void monthCallback_current_resolvesWithoutError() {
        when(friendService.getFriendsSortedByDayMonth(1L)).thenReturn(List.of());
        EditMessageText result = handler.handle(callback("MONTH_CURRENT"));
        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.BIRTHDAYS_NONE,
                Month.of(LocalDate.now().getMonthValue())
                        .getDisplayName(TextStyle.FULL_STANDALONE, Lang.EN.locale())));
    }

    @Test
    @DisplayName("CONFIRM_DELETE_ACCOUNT callback → deletes all data")
    void confirmDeleteAccount_deletesAllData() {
        EditMessageText result = handler.handle(callback("CONFIRM_DELETE_ACCOUNT"));
        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.DELETE_ACCOUNT_DONE));
    }

    @Test
    @DisplayName("CANCEL_DELETE_ACCOUNT callback → returns cancel message with /settings hint")
    void cancelDeleteAccount_returnsCancelWithHint() {
        EditMessageText result = handler.handle(callback("CANCEL_DELETE_ACCOUNT"));
        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.DELETE_ACCOUNT_CANCEL));
        assertThat(result.getText()).contains("/settings");
    }

    @Test
    @DisplayName("unknown callback prefix — returns null")
    void unknownCallback_returnsNull() {
        assertThat(handler.handle(callback("UNKNOWN_data"))).isNull();
    }

    @Test
    @DisplayName("null CallbackQuery — returns null without throwing")
    void nullCallback_returnsNull() {
        assertThat(handler.handle(null)).isNull();
    }

    // --- helper ---

    private CallbackQuery callback(String data) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);

        MaybeInaccessibleMessage message = mock(MaybeInaccessibleMessage.class);
        when(message.getChatId()).thenReturn(1L);
        when(message.getMessageId()).thenReturn(1);

        CallbackQuery cq = mock(CallbackQuery.class);
        when(cq.getFrom()).thenReturn(user);
        when(cq.getData()).thenReturn(data);
        when(cq.getMessage()).thenReturn(message);
        return cq;
    }
}
