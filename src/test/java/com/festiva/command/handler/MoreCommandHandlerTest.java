package com.festiva.command.handler;

import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.i18n.MessagesTestSupport;
import com.festiva.state.UserStateService;
import com.festiva.user.api.UserPreferenceService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("MoreCommandHandler")
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unused")
class MoreCommandHandlerTest extends MessagesTestSupport {

    @Mock UserStateService userStateService;
    @Mock UserPreferenceService userPreferenceService;
    @InjectMocks MoreCommandHandler handler;

    @Test
    @DisplayName("/more → returns inline hub with search and reminders buttons")
    void handle_returnsInlineHub() {
        when(userPreferenceService.getLanguage(1L)).thenReturn(Lang.EN);

        SendMessage result = handler.handle(update());

        verify(userStateService).clearState(1L);
        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.MORE_HEADER));
        var markup = (org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup) result.getReplyMarkup();
        assertThat(markup).isNotNull();
        assertThat(markup.getKeyboard()).flatExtracting(row -> row)
                .extracting(org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton::getText)
                .contains(Messages.get(Lang.EN, Messages.MORE_SEARCH_BTN), Messages.get(Lang.EN, Messages.MORE_SETTINGS_BTN));
    }

    @Test
    @DisplayName("more keyboard does not show back button on the hub itself")
    void keyboard_hubDoesNotShowBackButton() {
        var markup = MoreCommandHandler.keyboard(Lang.EN);

        assertThat(markup.getKeyboard()).flatExtracting(row -> row)
                .extracting(org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton::getCallbackData)
                .doesNotContain(MoreCommandHandler.CALLBACK_BACK);
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
