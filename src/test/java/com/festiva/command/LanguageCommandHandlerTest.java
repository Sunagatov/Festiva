package com.festiva.command;

import com.festiva.command.handler.LanguageCommandHandler;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.i18n.MessagesTestSupport;
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("LanguageCommandHandler")
@ExtendWith(MockitoExtension.class)
class LanguageCommandHandlerTest extends MessagesTestSupport {

    @Mock UserStateService userStateService;
    @InjectMocks LanguageCommandHandler handler;

    @BeforeEach
    void defaults() {
        lenient().when(userStateService.getLanguage(anyLong())).thenReturn(Lang.EN);
    }

    @Test
    @DisplayName("handle EN → returns language_choose prompt with keyboard")
    void handle_en_returnsPromptWithKeyboard() {
        var result = handler.handle(update());
        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.LANGUAGE_CHOOSE));
        assertThat(result.getReplyMarkup()).isNotNull();
    }

    @Test
    @DisplayName("handle EN → EN button has checkmark, RU button does not")
    void handle_en_enButtonHasCheckmark() {
        var result = handler.handle(update());
        var buttons = ((InlineKeyboardMarkup) result.getReplyMarkup()).getKeyboard().getFirst();
        assertThat(buttons.get(0).getText()).startsWith("✅");
        assertThat(buttons.get(1).getText()).doesNotStartWith("✅");
    }

    @Test
    @DisplayName("handle RU → RU button has checkmark, EN button does not")
    void handle_ru_ruButtonHasCheckmark() {
        when(userStateService.getLanguage(anyLong())).thenReturn(Lang.RU);
        var result = handler.handle(update());
        var buttons = ((InlineKeyboardMarkup) result.getReplyMarkup()).getKeyboard().getFirst();
        assertThat(buttons.get(0).getText()).doesNotStartWith("✅");
        assertThat(buttons.get(1).getText()).startsWith("✅");
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
