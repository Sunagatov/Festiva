package com.festiva.bot;

import com.festiva.command.CommandRouter;
import com.festiva.i18n.Lang;
import com.festiva.metrics.MetricsSender;
import com.festiva.user.api.UserPreferenceService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BirthdayBotPollingTest extends com.festiva.i18n.MessagesTestSupport {
    @Test
    void disabledPollingDoesNotRegisterWithTelegram() {
        TelegramClient client = mock(TelegramClient.class);
        BotCommandsService commands = mock(BotCommandsService.class);
        BirthdayBot bot = new BirthdayBot(null, null, client, "unused", null, commands, null, null);
        ReflectionTestUtils.setField(bot, "pollingEnabled", false);
        bot.start();
        verifyNoInteractions(client, commands);
    }

    @Test
    void groupMessageIsRejectedBeforeCommandRouting() throws Exception {
        TelegramClient client = mock(TelegramClient.class);
        CommandRouter router = mock(CommandRouter.class);
        MetricsSender metrics = mock(MetricsSender.class);
        UserPreferenceService preferences = mock(UserPreferenceService.class);
        when(preferences.getLanguage(42L)).thenReturn(Lang.EN);
        BirthdayBot bot = new BirthdayBot(router, mock(CallbackQueryHandler.class), client,
                "unused", metrics, mock(BotCommandsService.class), null, preferences);

        User user = mock(User.class);
        when(user.getId()).thenReturn(42L);
        Message message = mock(Message.class);
        when(message.getFrom()).thenReturn(user);
        when(message.getChatId()).thenReturn(-100L);
        Update update = mock(Update.class);
        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);

        bot.consume(update);

        verify(router, never()).route(update);
        var captor = org.mockito.ArgumentCaptor.forClass(SendMessage.class);
        verify(client).execute(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getText()).contains("private chat");
    }
}
