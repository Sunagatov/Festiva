package com.festiva.command.handler;

import com.festiva.i18n.Lang;
import com.festiva.i18n.MessagesTestSupport;
import com.festiva.state.UserStateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Navigation command state reset")
@ExtendWith(MockitoExtension.class)
class NavigationCommandStateResetTest extends MessagesTestSupport {

    @Mock UserStateService userStateService;

    @Test
    @DisplayName("/start clears state before rendering the welcome menu")
    void startClearsState() {
        when(userStateService.getLanguage(1L)).thenReturn(Lang.EN);

        new StartCommandHandler(userStateService).handle(update());

        verify(userStateService).clearState(1L);
    }

    @Test
    @DisplayName("/menu clears state before rendering the menu")
    void menuClearsState() {
        when(userStateService.getLanguage(1L)).thenReturn(Lang.EN);

        new HelpCommandHandler(userStateService).handle(update());

        verify(userStateService).clearState(1L);
    }

    @Test
    @DisplayName("/about clears state before rendering about text")
    void aboutClearsState() {
        when(userStateService.getLanguage(1L)).thenReturn(Lang.EN);

        new AboutCommandHandler(userStateService).handle(update());

        verify(userStateService).clearState(1L);
    }

    @Test
    @DisplayName("/language clears state before rendering the picker")
    void languageClearsState() {
        when(userStateService.getLanguage(1L)).thenReturn(Lang.EN);

        new LanguageCommandHandler(userStateService).handle(update());

        verify(userStateService).clearState(1L);
    }

    @Test
    @DisplayName("/help delegates to the menu handler")
    void helpDelegatesToMenuHandler() {
        Update update = mock(Update.class);
        HelpCommandHandler helpHandler = mock(HelpCommandHandler.class);
        HelpAliasCommandHandler aliasHandler = new HelpAliasCommandHandler(helpHandler);

        aliasHandler.handle(update);

        verify(helpHandler).handle(update);
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
