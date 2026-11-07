package com.festiva.command.handler;

import com.festiva.command.MessageBuilder;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.i18n.MessagesTestSupport;
import com.festiva.state.UserStateService;
import com.festiva.user.api.UserPreferenceService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SupportProjectCommandHandlerTest extends MessagesTestSupport {
    @ParameterizedTest
    @EnumSource(Lang.class)
    void supportCommandPreservesNavigationAndResetsConversation(Lang lang) {
        UserStateService state = mock(UserStateService.class);
        UserPreferenceService preferences = mock(UserPreferenceService.class);
        when(preferences.getLanguage(42L)).thenReturn(lang);
        var handler = new SupportProjectCommandHandler(state, preferences);
        var response = handler.handle(update());
        assertThat(handler.command()).isEqualTo("/supportproject");
        verify(state).clearState(42L);
        assertThat(response.getChatId()).isEqualTo("42");
        assertThat(response.getText()).isEqualTo(Messages.get(lang, Messages.SUPPORT_PROJECT));
        var keyboard = (InlineKeyboardMarkup) response.getReplyMarkup();
        assertThat(keyboard.getKeyboard()).flatExtracting(row -> row)
                .anySatisfy(button -> assertThat(button.getUrl()).isEqualTo(MessageBuilder.COFFEE_URL))
                .anySatisfy(button -> assertThat(button.getCallbackData()).isEqualTo(MoreCommandHandler.CALLBACK_BACK));
    }

    @ParameterizedTest
    @EnumSource(Lang.class)
    void welcomeKeepsMainKeyboardAndHelpAboutMoreHaveCoffeeButtons(Lang lang) {
        UserStateService state = mock(UserStateService.class);
        UserPreferenceService preferences = mock(UserPreferenceService.class);
        when(preferences.getLanguage(42L)).thenReturn(lang);
        var welcome = new StartCommandHandler(state, preferences).handle(update());
        assertThat(welcome.getReplyMarkup()).isInstanceOf(ReplyKeyboardMarkup.class);
        assertThat(welcome.getText()).contains(MessageBuilder.COFFEE_URL);
        assertThat(welcome.getLinkPreviewOptions().getIsDisabled()).isTrue();
        for (var response : java.util.List.of(
                new HelpCommandHandler(state, preferences).handle(update()),
                new AboutCommandHandler(state, preferences).handle(update()),
                new MoreCommandHandler(state, preferences).handle(update()))) {
            var keyboard = (InlineKeyboardMarkup) response.getReplyMarkup();
            assertThat(keyboard.getKeyboard()).flatExtracting(row -> row)
                    .filteredOn(button -> MessageBuilder.COFFEE_URL.equals(button.getUrl())).hasSize(1);
        }
    }

    private Update update() {
        Message message = new Message();
        message.setChat(new Chat(42L, "private"));
        message.setFrom(new User(42L, "Test", false));
        Update update = new Update();
        update.setMessage(message);
        return update;
    }
}
