package com.festiva.command;

import com.festiva.command.handler.DeleteAccountCommandHandler;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.i18n.MessagesTestSupport;
import com.festiva.state.UserStateService;
import com.festiva.user.api.AccountDeletionAction;
import com.festiva.user.api.UserPreferenceService;
import org.junit.jupiter.api.BeforeEach;
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("DeleteAccountCommandHandler")
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unused")
class DeleteAccountCommandHandlerTest extends MessagesTestSupport {

    @Mock UserPreferenceService userPreferenceService;
    @Mock UserStateService userStateService;
    @InjectMocks DeleteAccountCommandHandler handler;

    @BeforeEach
    void defaultLang() {
        lenient().when(userPreferenceService.getLanguage(anyLong())).thenReturn(Lang.EN);
    }

    @Test
    @DisplayName("/deleteaccount → returns confirmation keyboard")
    void handle_returnsConfirmationKeyboard() {
        SendMessage result = handler.handle(update());

        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.DELETE_ACCOUNT_ASK));
        assertThat(result.getReplyMarkup()).isNotNull();
    }

    @Test
    @DisplayName("/deleteaccount → keyboard has Yes and No buttons")
    void handle_keyboardHasYesAndNoButtons() {
        var markup = (org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup)
                handler.handle(update()).getReplyMarkup();
        var callbacks = markup.getKeyboard().getFirst().stream()
                .map(InlineKeyboardButton::getCallbackData).toList();
        assertThat(callbacks).contains(AccountDeletionAction.CONFIRM_DELETE);
        assertThat(callbacks).contains(AccountDeletionAction.CANCEL_DELETE);
    }

    @Test
    @DisplayName("/deleteaccount RU → returns RU confirmation prompt")
    void handle_ru_returnsRuPrompt() {
        when(userPreferenceService.getLanguage(anyLong())).thenReturn(Lang.RU);
        assertThat(handler.handle(update()).getText())
                .contains(Messages.get(Lang.RU, Messages.DELETE_ACCOUNT_ASK));
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
