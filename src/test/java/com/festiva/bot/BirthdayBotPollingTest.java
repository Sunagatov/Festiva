package com.festiva.bot;

import com.festiva.command.CommandRouter;
import com.festiva.abuse.BotAbuseGuard;
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
import static org.mockito.Mockito.reset;

class BirthdayBotPollingTest extends com.festiva.i18n.MessagesTestSupport {
    @Test
    void telegramProcessingCreatesAnObservationAndMarksCaughtFailures() throws Exception {
        var registry = io.micrometer.observation.ObservationRegistry.create();
        var stopped = new java.util.concurrent.atomic.AtomicReference<io.micrometer.observation.Observation.Context>();
        registry.observationConfig().observationHandler(new io.micrometer.observation.ObservationHandler<>() {
            public boolean supportsContext(io.micrometer.observation.Observation.Context context) { return true; }
            public void onStop(io.micrometer.observation.Observation.Context context) { stopped.set(context); }
        });
        var router = mock(CommandRouter.class);
        when(router.route(org.mockito.ArgumentMatchers.any())).thenThrow(new IllegalStateException("test failure"));
        var preferences = mock(UserPreferenceService.class);
        when(preferences.getLanguage(42L)).thenReturn(Lang.EN);
        var bot = new BirthdayBot(router, mock(CallbackQueryHandler.class), mock(TelegramClient.class), "unused",
                mock(MetricsSender.class), mock(BotCommandsService.class), null, preferences,
                new BotAbuseGuard(30, 300, 3, 20), registry);
        User user = mock(User.class);
        when(user.getId()).thenReturn(42L);
        Message message = mock(Message.class);
        when(message.getFrom()).thenReturn(user);
        when(message.getChatId()).thenReturn(42L);
        Update update = mock(Update.class);
        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        try {
            bot.consume(update);
            org.assertj.core.api.Assertions.assertThat(stopped.get().getName()).isEqualTo("festiva.telegram.update");
            org.assertj.core.api.Assertions.assertThat(stopped.get().getError()).isInstanceOf(IllegalStateException.class);
            org.assertj.core.api.Assertions.assertThat(registry.getCurrentObservation()).isNull();
        } finally { bot.stop(); }
    }

    @Test
    void pollingBatchCannotCreateAnUnboundedQueue() throws Exception {
        TelegramClient client = mock(TelegramClient.class);
        CommandRouter router = mock(CommandRouter.class);
        MetricsSender metrics = mock(MetricsSender.class);
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        when(router.route(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            entered.countDown();
            release.await(5, java.util.concurrent.TimeUnit.SECONDS);
            return null;
        });
        BirthdayBot bot = new BirthdayBot(router, mock(CallbackQueryHandler.class), client, "unused", metrics,
                mock(BotCommandsService.class), null, mock(UserPreferenceService.class),
                new BotAbuseGuard(2000, 2000, 3, 20), io.micrometer.observation.ObservationRegistry.NOOP);
        User user = mock(User.class);
        when(user.getId()).thenReturn(42L);
        Message message = mock(Message.class);
        when(message.getFrom()).thenReturn(user);
        when(message.getChatId()).thenReturn(42L);
        Update update = mock(Update.class);
        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        try {
            bot.consume(java.util.List.of(update));
            org.assertj.core.api.Assertions.assertThat(entered.await(2, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            bot.consume(java.util.Collections.nCopies(1000, update));
        } finally { release.countDown(); bot.stop(); }
        verify(router, org.mockito.Mockito.times(33)).route(update);
    }

    @Test
    void deniedCallbacksDoNotAcknowledgeOrReadPreferencesOrRoute() throws Exception {
        TelegramClient client = mock(TelegramClient.class);
        CommandRouter router = mock(CommandRouter.class);
        CallbackQueryHandler callbacks = mock(CallbackQueryHandler.class);
        MetricsSender metrics = mock(MetricsSender.class);
        UserPreferenceService preferences = mock(UserPreferenceService.class);
        BirthdayBot bot = new BirthdayBot(router, callbacks, client, "unused", metrics,
                mock(BotCommandsService.class), null, preferences, new BotAbuseGuard(1, 100, 3, 20), io.micrometer.observation.ObservationRegistry.NOOP);
        User user = mock(User.class);
        when(user.getId()).thenReturn(42L);
        Message message = mock(Message.class);
        when(message.getFrom()).thenReturn(user);
        when(message.getChatId()).thenReturn(42L);
        Update first = mock(Update.class);
        when(first.hasMessage()).thenReturn(true);
        when(first.getMessage()).thenReturn(message);
        bot.consume(first);
        reset(client, router, callbacks, metrics, preferences);

        var callback = mock(org.telegram.telegrambots.meta.api.objects.CallbackQuery.class);
        when(callback.getFrom()).thenReturn(user);
        Update second = mock(Update.class);
        when(second.hasCallbackQuery()).thenReturn(true);
        when(second.getCallbackQuery()).thenReturn(callback);
        bot.consume(second);
        verifyNoInteractions(client, router, callbacks, metrics, preferences);
        bot.stop();
    }

    @Test
    void disabledPollingDoesNotRegisterWithTelegram() {
        TelegramClient client = mock(TelegramClient.class);
        BotCommandsService commands = mock(BotCommandsService.class);
        BirthdayBot bot = new BirthdayBot(null, null, client, "unused", null, commands, null, null, new BotAbuseGuard(30, 300, 3, 20), io.micrometer.observation.ObservationRegistry.NOOP);
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
                "unused", metrics, mock(BotCommandsService.class), null, preferences, new BotAbuseGuard(30, 300, 3, 20), io.micrometer.observation.ObservationRegistry.NOOP);

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
