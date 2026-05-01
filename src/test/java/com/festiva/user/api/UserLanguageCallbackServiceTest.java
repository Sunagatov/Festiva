package com.festiva.user.api;

import com.festiva.bot.BotCommandsService;
import com.festiva.bot.CallbackResult;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.i18n.MessagesTestSupport;
import com.festiva.state.UserStateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("UserLanguageCallbackService")
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unused")
class UserLanguageCallbackServiceTest extends MessagesTestSupport {

    @Mock private UserStateService userStateService;
    @Mock private UserPreferenceService userPreferenceService;
    @Mock private BotCommandsService commandsService;
    @InjectMocks private UserLanguageCallbackService service;

    @Test
    @DisplayName("LANG_EN callback updates language, clears state, and refreshes commands")
    void languageCallback_updatesStateAndCommands() {
        CallbackResult result = service.handle(UserLanguageAction.callback(Lang.EN), 1L);

        verify(userPreferenceService).setLanguage(1L, Lang.EN);
        verify(userStateService).clearState(1L);
        verify(commandsService).updateCommandsForUser(1L, Lang.EN);
        assertThat(result.text).contains(Messages.get(Lang.EN, Messages.LANGUAGE_SET));
        assertThat(result.markup).isNotNull();
    }

    @Test
    @DisplayName("LANG_RU callback returns RU confirmation")
    void languageCallback_ruReturnsRuMessage() {
        CallbackResult result = service.handle(UserLanguageAction.callback(Lang.RU), 1L);

        assertThat(result.text).contains(Messages.get(Lang.RU, Messages.LANGUAGE_SET));
    }

    @Test
    @DisplayName("unknown LANG callback returns session expired in current language")
    void unknownLanguage_returnsSessionExpired() {
        when(userPreferenceService.getLanguage(1L)).thenReturn(Lang.RU);

        CallbackResult result = service.handle("LANG_UNKNOWN", 1L);

        assertThat(result.text).contains(Messages.get(Lang.RU, Messages.SESSION_EXPIRED));
    }

    @Test
    @DisplayName("non-language callback returns null")
    void nonLanguageCallback_returnsNull() {
        assertThat(service.handle("SETTINGS_HOUR_9", 1L)).isNull();
    }
}
