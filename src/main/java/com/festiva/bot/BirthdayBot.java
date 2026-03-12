package com.festiva.bot;

import com.festiva.command.CommandRouter;
import com.festiva.i18n.Lang;
import com.festiva.metrics.MetricsSender;
import com.festiva.notification.NotificationSender;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Component
public class BirthdayBot implements LongPollingSingleThreadUpdateConsumer, NotificationSender {

    private final String botToken;
    private final TelegramClient telegramClient;
    private TelegramBotsLongPollingApplication botsApplication;
    private final CommandRouter commandRouter;
    private final CallbackQueryHandler callbackQueryHandler;
    private final MetricsSender metricsSender;
    private final BotCommandsProvider commandsProvider;

    public BirthdayBot(CommandRouter commandRouter,
                       CallbackQueryHandler callbackQueryHandler,
                       TelegramClient telegramClient,
                       @Value("${telegram.bot.token}") String botToken,
                       MetricsSender metricsSender,
                       BotCommandsProvider commandsProvider) {
        this.botToken = botToken;
        this.telegramClient = telegramClient;
        this.commandRouter = commandRouter;
        this.callbackQueryHandler = callbackQueryHandler;
        this.metricsSender = metricsSender;
        this.commandsProvider = commandsProvider;
    }

    @PostConstruct
    public void start() {
        try {
            botsApplication = new TelegramBotsLongPollingApplication();
            botsApplication.registerBot(botToken, this);
            log.info("bot.started");
        } catch (TelegramApiException e) {
            throw new RuntimeException("bot.start.failed", e);
        }
        try {
            // Set default commands (English)
            telegramClient.execute(SetMyCommands.builder()
                    .commands(commandsProvider.getCommandsForLanguage(Lang.EN))
                    .languageCode(Lang.EN.code())
                    .build());
            // Set Russian commands
            telegramClient.execute(SetMyCommands.builder()
                    .commands(commandsProvider.getCommandsForLanguage(Lang.RU))
                    .languageCode(Lang.RU.code())
                    .build());
            log.info("bot.commands.registered");
        } catch (TelegramApiException e) {
            log.error("bot.commands.register.failed: message={}", e.getMessage(), e);
        }
    }

    @Override
    public void consume(Update update) {
        if (update == null) {
            log.warn("bot.update.null");
            return;
        }
        long startTime = System.currentTimeMillis();
        String updateType = update.hasCallbackQuery() ? "callback" : update.hasMessage() ? "message" : "other";
        try {
            if (update.hasCallbackQuery()) {
                EditMessageText edit = callbackQueryHandler.handle(update.getCallbackQuery());
                if (edit != null) telegramClient.execute(edit);
            } else if (update.hasMessage()) {
                SendMessage response = commandRouter.route(update);
                if (response != null) telegramClient.execute(response);
            }
            metricsSender.sendMetrics(update, "SUCCESS", System.currentTimeMillis() - startTime);
        } catch (TelegramApiException | RuntimeException e) {
            metricsSender.sendMetrics(update, "ERROR", System.currentTimeMillis() - startTime);
            log.error("bot.update.failed: updateId={}, type={}, message={}", update.getUpdateId(), updateType, e.getMessage(), e);
        }
    }

    @PreDestroy
    public void stop() throws Exception {
        if (botsApplication != null) botsApplication.close();
    }

    @Override
    public void send(long telegramUserId, String text) {
        try {
            telegramClient.execute(SendMessage.builder().chatId(telegramUserId).parseMode("HTML").text(text).build());
        } catch (TelegramApiException e) {
            log.error("bot.notification.failed: userId={}, message={}", telegramUserId, e.getMessage(), e);
        }
    }

    public void updateCommandsForUser(long chatId, Lang lang) {
        try {
            telegramClient.execute(SetMyCommands.builder()
                    .commands(commandsProvider.getCommandsForLanguage(lang))
                    .scope(new org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeChat(String.valueOf(chatId)))
                    .build());
            log.debug("bot.commands.updated: chatId={}, lang={}", chatId, lang);
        } catch (TelegramApiException e) {
            log.error("bot.commands.update.failed: chatId={}, message={}", chatId, e.getMessage(), e);
        }
    }
}
