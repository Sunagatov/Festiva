package com.festiva.command;

import com.festiva.command.handler.SettingsCommandHandler;
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

import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("SettingsCommandHandler")
@ExtendWith(MockitoExtension.class)
class SettingsCommandHandlerTest extends MessagesTestSupport {

    @Mock UserStateService userStateService;
    @InjectMocks SettingsCommandHandler handler;

    @BeforeEach
    void defaults() {
        lenient().when(userStateService.getLanguage(anyLong())).thenReturn(Lang.EN);
        lenient().when(userStateService.getNotifyHour(anyLong())).thenReturn(9);
        lenient().when(userStateService.getTimezone(anyLong())).thenReturn("UTC");
    }

    @Test
    @DisplayName("handle → response contains settings header text")
    void handle_containsSettingsHeader() {
        assertThat(handler.handle(update()).getText())
                .contains(Messages.get(Lang.EN, Messages.SETTINGS_HEADER));
    }

    @Test
    @DisplayName("hourKeyboard → contains all 24 hours")
    void hourKeyboard_containsAll24Hours() {
        InlineKeyboardMarkup markup = SettingsCommandHandler.hourKeyboard(9);
        var allLabels = markup.getKeyboard().stream()
                .flatMap(row -> row.stream())
                .map(btn -> btn.getCallbackData())
                .collect(Collectors.toList());
        for (int h = 0; h < 24; h++) {
            assertThat(allLabels).contains(SettingsCommandHandler.SETTINGS_HOUR_PREFIX + h);
        }
    }

    @Test
    @DisplayName("hourKeyboard → active hour has checkmark in label")
    void hourKeyboard_activeHourHasCheckmark() {
        InlineKeyboardMarkup markup = SettingsCommandHandler.hourKeyboard(9);
        var activeBtn = markup.getKeyboard().stream()
                .flatMap(row -> row.stream())
                .filter(btn -> btn.getCallbackData().equals(SettingsCommandHandler.SETTINGS_HOUR_PREFIX + 9))
                .findFirst().orElseThrow();
        assertThat(activeBtn.getText()).startsWith("✅");
    }

    @Test
    @DisplayName("tzKeyboard → active timezone has checkmark in label")
    void tzKeyboard_activeTzHasCheckmark() {
        InlineKeyboardMarkup markup = SettingsCommandHandler.tzKeyboard("UTC");
        var activeBtn = markup.getKeyboard().stream()
                .flatMap(row -> row.stream())
                .filter(btn -> btn.getCallbackData().equals(SettingsCommandHandler.SETTINGS_TZ_PREFIX + "UTC"))
                .findFirst().orElseThrow();
        assertThat(activeBtn.getText()).startsWith("✅");
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
