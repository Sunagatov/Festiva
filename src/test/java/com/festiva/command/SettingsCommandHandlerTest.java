package com.festiva.command;

import com.festiva.command.handler.SettingsCommandHandler;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.i18n.MessagesTestSupport;
import com.festiva.user.api.UserPreferenceService;
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.Collection;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("SettingsCommandHandler")
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unused")
class SettingsCommandHandlerTest extends MessagesTestSupport {

    @Mock UserPreferenceService userPreferenceService;
    @InjectMocks SettingsCommandHandler handler;

    @BeforeEach
    void defaults() {
        lenient().when(userPreferenceService.getLanguage(anyLong())).thenReturn(Lang.EN);
        lenient().when(userPreferenceService.getNotifyHour(anyLong())).thenReturn(9);
        lenient().when(userPreferenceService.getTimezone(anyLong())).thenReturn("UTC");
    }

    @Test
    @DisplayName("handle → response contains settings header text")
    void handle_containsSettingsHeader() {
        assertThat(handler.handle(update()).getText())
                .contains(Messages.get(Lang.EN, Messages.SETTINGS_HEADER));
        assertThat(handler.handle(update()).getText())
                .contains(Messages.get(Lang.EN, Messages.SETTINGS_TZ_HEADER));
    }

    @Test
    @DisplayName("hourKeyboard → contains all 24 hours")
    void hourKeyboard_containsAll24Hours() {
        InlineKeyboardMarkup markup = SettingsCommandHandler.hourKeyboard(9);
        var allLabels = markup.getKeyboard().stream()
                .flatMap(Collection::stream)
                .map(InlineKeyboardButton::getCallbackData)
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
                .flatMap(Collection::stream)
                .filter(btn -> btn.getCallbackData().equals(SettingsCommandHandler.SETTINGS_HOUR_PREFIX + 9))
                .findFirst().orElseThrow();
        assertThat(activeBtn.getText()).startsWith("✅");
    }

    @Test
    @DisplayName("tzRegionKeyboard → contains regional submenu buttons")
    void tzRegionKeyboard_containsRegionalButtons() {
        InlineKeyboardMarkup markup = SettingsCommandHandler.tzRegionKeyboard(Lang.EN, null);
        var callbacks = markup.getKeyboard().stream()
                .flatMap(Collection::stream)
                .map(InlineKeyboardButton::getCallbackData)
                .collect(Collectors.toList());
        assertThat(callbacks).containsExactly(
                SettingsCommandHandler.SETTINGS_TZ_REGION_PREFIX + SettingsCommandHandler.REGION_EUROPE,
                SettingsCommandHandler.SETTINGS_TZ_REGION_PREFIX + SettingsCommandHandler.REGION_ASIA,
                SettingsCommandHandler.SETTINGS_TZ_REGION_PREFIX + SettingsCommandHandler.REGION_AMERICAS,
                SettingsCommandHandler.SETTINGS_TZ_REGION_PREFIX + SettingsCommandHandler.REGION_AFRICA,
                SettingsCommandHandler.SETTINGS_TZ_REGION_PREFIX + SettingsCommandHandler.REGION_PACIFIC,
                SettingsCommandHandler.SETTINGS_TZ_REGION_PREFIX + SettingsCommandHandler.REGION_UTC
        );
    }

    @Test
    @DisplayName("tzKeyboard(region) → active timezone has checkmark in label")
    void tzKeyboard_activeTzHasCheckmark() {
        InlineKeyboardMarkup markup = SettingsCommandHandler.tzKeyboard(SettingsCommandHandler.REGION_UTC, "UTC");
        var activeBtn = markup.getKeyboard().stream()
                .flatMap(Collection::stream)
                .filter(btn -> btn.getCallbackData().equals(SettingsCommandHandler.SETTINGS_TZ_PREFIX + "UTC"))
                .findFirst().orElseThrow();
        assertThat(activeBtn.getText()).startsWith("✅");
    }

    @Test
    @DisplayName("combined with expanded region → includes only that region's timezones")
    void combined_expandedRegionShowsFilteredTimezones() {
        InlineKeyboardMarkup markup = SettingsCommandHandler.combined(9, "UTC", Lang.EN, SettingsCommandHandler.REGION_UTC);
        var callbacks = markup.getKeyboard().stream()
                .flatMap(Collection::stream)
                .map(InlineKeyboardButton::getCallbackData)
                .collect(Collectors.toList());
        assertThat(callbacks).contains(SettingsCommandHandler.SETTINGS_TZ_PREFIX + "UTC");
        assertThat(callbacks).doesNotContain(SettingsCommandHandler.SETTINGS_TZ_PREFIX + "Europe/London");
    }

    @Test
    @DisplayName("handle RU → response contains RU settings header")
    void handle_ru_containsRuHeader() {
        when(userPreferenceService.getLanguage(anyLong())).thenReturn(Lang.RU);
        assertThat(handler.handle(update()).getText())
                .contains(Messages.get(Lang.RU, Messages.SETTINGS_HEADER));
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
