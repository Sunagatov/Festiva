package com.festiva.bot;

import com.festiva.command.handler.BulkAddCommandHandler;
import com.festiva.command.handler.MoreCommandHandler;
import com.festiva.command.handler.SettingsCommandHandler;
import com.festiva.command.handler.UpcomingBirthdaysCommandHandler;
import com.festiva.friend.api.FriendAction;
import com.festiva.friend.api.FriendCallbackService;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.state.UserStateService;
import com.festiva.user.api.AccountDeletionService;
import com.festiva.user.api.UserLanguageCallbackService;
import com.festiva.user.api.UserPreferenceService;
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("CallbackQueryHandler")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressWarnings("unused")
class CallbackQueryHandlerTest extends com.festiva.i18n.MessagesTestSupport {

    @Mock private UserStateService userStateService;
    @Mock private UpcomingBirthdaysCommandHandler upcomingHandler;
    @Mock private BulkAddCommandHandler bulkAddHandler;
    @Mock private FriendCallbackService friendCallbackService;
    @Mock private MoreCallbackHandler moreCallbackHandler;
    @Mock private AccountDeletionService accountDeletionService;
    @Mock private UserLanguageCallbackService userLanguageCallbackService;
    @Mock private UserPreferenceService userPreferenceService;
    @InjectMocks private CallbackQueryHandler handler;

    @BeforeEach
    void defaultLang() {
        lenient().when(userPreferenceService.getLanguage(anyLong())).thenReturn(Lang.EN);
    }

    @Test
    @DisplayName("LANG_EN callback → confirmation contains next-step hint")
    void langCallback_en_containsNextStepHint() {
        when(userLanguageCallbackService.handle("LANG_EN", 1L)).thenReturn(new CallbackResult("/language", null));
        EditMessageText result = handler.handle(callback("LANG_EN"));
        assertThat(result.getText()).contains("/language");
    }

    @Test
    @DisplayName("SETTINGS_HOUR_ callback → sets hour and returns compact confirmation")
    void settingsHourCallback_setsHourAndContainsHint() {
        when(userPreferenceService.getNotifyHour(1L)).thenReturn(9);
        when(userPreferenceService.getTimezone(1L)).thenReturn("UTC");
        EditMessageText result = handler.handle(callback("SETTINGS_HOUR_9"));
        verify(userPreferenceService).setNotifyHour(1L, 9);
        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.SETTINGS_HOUR_SET, 9));
        assertThat(result.getText()).doesNotContain("/settings");
    }

    @Test
    @DisplayName("SETTINGS_TZ_ callback → sets timezone and returns compact confirmation")
    void settingsTzCallback_setsTzAndContainsHint() {
        when(userPreferenceService.getNotifyHour(1L)).thenReturn(9);
        when(userPreferenceService.getTimezone(1L)).thenReturn("UTC");
        EditMessageText result = handler.handle(callback("SETTINGS_TZ_UTC"));
        verify(userPreferenceService).setTimezone(1L, "UTC");
        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.SETTINGS_TZ_SET, "UTC"));
        assertThat(result.getText()).doesNotContain("/settings");
    }

    @Test
    @DisplayName("SETTINGS_TZ_REGION_ callback → re-renders settings with filtered timezone choices")
    void settingsTzRegionCallback_rerendersFilteredTimezoneChoices() {
        when(userPreferenceService.getNotifyHour(1L)).thenReturn(9);
        when(userPreferenceService.getTimezone(1L)).thenReturn("UTC");

        EditMessageText result = handler.handle(callback("SETTINGS_TZ_REGION_" + SettingsCommandHandler.REGION_EUROPE));

        verify(userPreferenceService, never()).setTimezone(anyLong(), anyString());
        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.SETTINGS_HEADER));
        assertThat(result.getReplyMarkup()).isNotNull();
        assertThat(result.getReplyMarkup().getKeyboard()).flatExtracting(row -> row)
                .extracting(InlineKeyboardButton::getCallbackData)
                .contains(SettingsCommandHandler.SETTINGS_TZ_PREFIX + "Europe/London")
                .doesNotContain(SettingsCommandHandler.SETTINGS_TZ_PREFIX + "UTC");
    }

    @Test
    @DisplayName("LANG_EN callback — sets language to EN and returns confirmation")
    void langCallback_setsLanguageAndReturnsConfirmation() {
        when(userLanguageCallbackService.handle("LANG_EN", 1L))
                .thenReturn(new CallbackResult(Messages.get(Lang.EN, Messages.LANGUAGE_SET), InlineKeyboardMarkup.builder().keyboard(List.of()).build()));
        EditMessageText result = handler.handle(callback("LANG_EN"));
        verify(userLanguageCallbackService).handle("LANG_EN", 1L);
        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.LANGUAGE_SET));
    }

    @Test
    @DisplayName("LANG_RU callback — sets language to RU and returns RU confirmation")
    void langCallback_ru_setsRuLanguage() {
        when(userPreferenceService.getLanguage(1L)).thenReturn(Lang.RU);
        when(userLanguageCallbackService.handle("LANG_RU", 1L))
                .thenReturn(new CallbackResult(Messages.get(Lang.RU, Messages.LANGUAGE_SET), InlineKeyboardMarkup.builder().keyboard(List.of()).build()));
        EditMessageText result = handler.handle(callback("LANG_RU"));
        verify(userLanguageCallbackService).handle("LANG_RU", 1L);
        assertThat(result.getText()).contains(Messages.get(Lang.RU, Messages.LANGUAGE_SET));
    }

    @Test
    @DisplayName("LANG_INVALID callback → returns SESSION_EXPIRED, does not crash")
    void langCallback_invalid_returnsSessionExpired() {
        when(userLanguageCallbackService.handle("LANG_INVALID", 1L))
                .thenReturn(new CallbackResult(Messages.get(Lang.EN, Messages.SESSION_EXPIRED), null));
        EditMessageText result = handler.handle(callback("LANG_INVALID"));
        verify(userLanguageCallbackService).handle("LANG_INVALID", 1L);
        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.SESSION_EXPIRED));
    }

    @Test
    @DisplayName("REMOVE_ callback → shows confirmation prompt with Yes/No buttons")
    void removeCallback_showsConfirmation() {
        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder().keyboard(List.of()).build();
        when(friendCallbackService.handle("REMOVE_id-alice", 1L, Lang.EN))
                .thenReturn(new CallbackResult("Alice", markup));

        EditMessageText result = handler.handle(callback("REMOVE_id-alice"));

        verify(friendCallbackService).handle("REMOVE_id-alice", 1L, Lang.EN);
        assertThat(result.getText()).contains("Alice");
        assertThat(result.getReplyMarkup()).isNotNull();
    }

    @Test
    @DisplayName("CONFIRM_REMOVE_ callback → deletes friend and returns removed confirmation")
    void confirmRemoveCallback_deletesFriendAndConfirms() {
        when(friendCallbackService.handle("CONFIRM_REMOVE_id-alice", 1L, Lang.EN))
                .thenReturn(new CallbackResult("Alice", null));

        EditMessageText result = handler.handle(callback("CONFIRM_REMOVE_id-alice"));

        verify(friendCallbackService).handle("CONFIRM_REMOVE_id-alice", 1L, Lang.EN);
        assertThat(result.getText()).contains("Alice");
    }

    @Test
    @DisplayName("REMOVE_ callback → stale friend returns SESSION_EXPIRED")
    void removeCallback_stale_returnsSessionExpired() {
        when(friendCallbackService.handle("REMOVE_ghost", 1L, Lang.EN))
                .thenReturn(new CallbackResult(Messages.get(Lang.EN, Messages.SESSION_EXPIRED), null));

        EditMessageText result = handler.handle(callback("REMOVE_ghost"));

        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.SESSION_EXPIRED));
    }

    @Test
    @DisplayName("CONFIRM_REMOVE_ callback → stale friend returns SESSION_EXPIRED")
    void confirmRemoveCallback_stale_returnsSessionExpired() {
        when(friendCallbackService.handle("CONFIRM_REMOVE_id-ghost", 1L, Lang.EN))
                .thenReturn(new CallbackResult(Messages.get(Lang.EN, Messages.SESSION_EXPIRED), null));

        EditMessageText result = handler.handle(callback("CONFIRM_REMOVE_id-ghost"));

        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.SESSION_EXPIRED));
    }

    @Test
    @DisplayName("CONFIRM_REMOVE_ callback → success message keeps quick actions")
    void confirmRemoveCallback_success_containsQuickActions() {
        when(friendCallbackService.handle("CONFIRM_REMOVE_id-alice", 1L, Lang.EN))
                .thenReturn(new CallbackResult("removed", InlineKeyboardMarkup.builder().keyboard(List.of()).build()));

        EditMessageText result = handler.handle(callback("CONFIRM_REMOVE_id-alice"));

        assertThat(result.getText()).contains("removed");
        assertThat(result.getReplyMarkup()).isNotNull();
    }

    @Test
    @DisplayName("CANCEL_REMOVE callback — clears state and returns cancelled message")
    void cancelRemoveCallback_clearsState() {
        when(friendCallbackService.handle("CANCEL_REMOVE", 1L, Lang.EN))
                .thenReturn(new CallbackResult(Messages.get(Lang.EN, Messages.CONFIRM_REMOVE_CANCEL),
                        org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup.builder().keyboard(List.of()).build()));

        EditMessageText result = handler.handle(callback("CANCEL_REMOVE"));

        verify(friendCallbackService).handle("CANCEL_REMOVE", 1L, Lang.EN);
        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.CONFIRM_REMOVE_CANCEL));
        assertThat(result.getReplyMarkup()).isNotNull();
    }

    @Test
    @DisplayName("MONTH_ callback with no friends in that month → birthdays_none contains next-step hint")
    void monthCallback_noFriends_noneContainsHint() {
        when(friendCallbackService.handle("MONTH_6", 1L, Lang.EN))
                .thenReturn(new CallbackResult("/add", null));

        EditMessageText result = handler.handle(callback("MONTH_6"));

        assertThat(result.getText()).contains("/add");
    }

    @Test
    @DisplayName("MORE_SETTINGS callback → dispatches through more callback handler")
    void moreCallback_dispatchesThroughMoreHandler() {
        when(moreCallbackHandler.handle(MoreCommandHandler.CALLBACK_SETTINGS, 1L, 1L, Lang.EN))
                .thenReturn(new CallbackResult("settings", InlineKeyboardMarkup.builder().keyboard(List.of()).build()));

        EditMessageText result = handler.handle(callback(MoreCommandHandler.CALLBACK_SETTINGS));

        verify(moreCallbackHandler).handle(MoreCommandHandler.CALLBACK_SETTINGS, 1L, 1L, Lang.EN);
        assertThat(result.getText()).contains("settings");
    }

    @Test
    @DisplayName("MORE_BACK callback → re-renders more hub")
    void moreBackCallback_rerendersMoreHub() {
        when(moreCallbackHandler.handle(MoreCommandHandler.CALLBACK_BACK, 1L, 1L, Lang.EN))
                .thenReturn(new CallbackResult(Messages.get(Lang.EN, Messages.MORE_HEADER), MoreCommandHandler.keyboard(Lang.EN)));

        EditMessageText result = handler.handle(callback(MoreCommandHandler.CALLBACK_BACK));

        verify(moreCallbackHandler).handle(MoreCommandHandler.CALLBACK_BACK, 1L, 1L, Lang.EN);
        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.MORE_HEADER));
        assertThat(result.getReplyMarkup()).isNotNull();
    }

    @Test
    @DisplayName("EDIT_PAGE callback with empty list — keeps add-first-friend CTA")
    void editPageCallback_emptyList_keepsAddCta() {
        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder().keyboard(List.of(
                new org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text(Messages.get(Lang.EN, Messages.REMOVE_EMPTY_ADD))
                                .callbackData(FriendAction.ACTION_ADD)
                                .build()))).build();
        when(friendCallbackService.handle("EDIT_PAGE_0", 1L, Lang.EN))
                .thenReturn(new CallbackResult(Messages.get(Lang.EN, Messages.FRIENDS_EMPTY), markup));

        EditMessageText result = handler.handle(callback("EDIT_PAGE_0"));

        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.FRIENDS_EMPTY));
        assertThat(result.getReplyMarkup()).isNotNull();
        assertThat(result.getReplyMarkup().getKeyboard().getFirst().getFirst().getText())
                .isEqualTo(Messages.get(Lang.EN, Messages.REMOVE_EMPTY_ADD));
        assertThat(result.getReplyMarkup().getKeyboard().getFirst().getFirst().getCallbackData())
                .isEqualTo(FriendAction.ACTION_ADD);
    }

    @Test
    @DisplayName("MONTH_6 callback — returns friends born in June")
    void monthCallback_returnsFilteredFriends() {
        when(friendCallbackService.handle("MONTH_6", 1L, Lang.EN))
                .thenReturn(new CallbackResult("Alice", null));

        EditMessageText result = handler.handle(callback("MONTH_6"));

        assertThat(result.getText()).contains("Alice");
    }

    @Test
    @DisplayName("MONTH_CURRENT callback — resolves to current month, returns no-birthdays message")
    void monthCallback_current_resolvesWithoutError() {
        when(friendCallbackService.handle("MONTH_CURRENT", 1L, Lang.EN))
                .thenReturn(new CallbackResult(Messages.get(Lang.EN, Messages.BIRTHDAYS_NONE, "March"), null));

        EditMessageText result = handler.handle(callback("MONTH_CURRENT"));

        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.BIRTHDAYS_NONE, "March"));
    }

    @Test
    @DisplayName("CONFIRM_DELETE_ACCOUNT callback → deletes all data")
    void confirmDeleteAccount_deletesAllData() {
        EditMessageText result = handler.handle(callback("CONFIRM_DELETE_ACCOUNT"));
        verify(accountDeletionService).deleteAccount(1L);
        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.DELETE_ACCOUNT_DONE));
    }

    @Test
    @DisplayName("BULK_CSV callback → enters bulk-add state and shows upload prompt")
    void bulkCsvCallback_entersBulkAddState() {
        when(bulkAddHandler.promptPaste(1L, 1L, Lang.EN))
                .thenReturn(org.telegram.telegrambots.meta.api.methods.send.SendMessage.builder()
                        .chatId(1L)
                        .parseMode("HTML")
                        .text(Messages.get(Lang.EN, Messages.BULK_ADD_PROMPT))
                        .build());

        EditMessageText result = handler.handle(callback(BulkAddCommandHandler.CALLBACK_CSV));

        verify(bulkAddHandler).sendCsvTemplate(1L, Lang.EN);
        verify(bulkAddHandler).promptPaste(1L, 1L, Lang.EN);
        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.BULK_ADD_PROMPT));
    }

    @Test
    @DisplayName("CANCEL_DELETE_ACCOUNT callback → returns cancel message with quick settings action")
    void cancelDeleteAccount_returnsCancelWithHint() {
        EditMessageText result = handler.handle(callback("CANCEL_DELETE_ACCOUNT"));
        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.DELETE_ACCOUNT_CANCEL));
        assertThat(result.getText()).doesNotContain("/settings");
        assertThat(result.getReplyMarkup()).isNotNull();
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
