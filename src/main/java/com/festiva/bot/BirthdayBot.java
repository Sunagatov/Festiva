package com.festiva.bot;

import com.festiva.command.CommandRouter;
import com.festiva.metrics.MetricsSender;
import com.festiva.notification.NotificationSender;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

@Slf4j
@Component
public class BirthdayBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer, NotificationSender {

    private final String botToken;
    private final TelegramClient telegramClient;
    private final CommandRouter commandRouter;
    private final CallbackQueryHandler callbackQueryHandler;
    private final MetricsSender metricsSender;

    public BirthdayBot(CommandRouter commandRouter,
                       CallbackQueryHandler callbackQueryHandler,
                       @Value("${telegram.bot.token}") String botToken,
                       MetricsSender metricsSender) {
        this.botToken = botToken;
        this.telegramClient = new OkHttpTelegramClient(botToken);
        this.commandRouter = commandRouter;
        this.callbackQueryHandler = callbackQueryHandler;
        this.metricsSender = metricsSender;
    }

    @PostConstruct
    public void registerCommands() {
        try {
            telegramClient.execute(SetMyCommands.builder()
                    .commands(List.of(
                            new BotCommand("start",             "Start / Запустить"),
                            new BotCommand("list",              "Friends / Друзья"),
                            new BotCommand("add",               "Add friend / Добавить друга"),
                            new BotCommand("remove",            "Remove friend / Удалить друга"),
                            new BotCommand("birthdays",         "By month / По месяцам"),
                            new BotCommand("upcomingbirthdays", "Upcoming / Ближайшие"),
                            new BotCommand("jubilee",           "Milestones / Юбилеи"),
                            new BotCommand("help",              "Help / Помощь"),
                            new BotCommand("language",          "🌐 Language / Язык"),
                            new BotCommand("cancel",            "Cancel / Отмена")
                    ))
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to register bot commands", e);
        }
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        if (update == null) {
            log.warn("Received null update");
            return;
        }
        long startTime = System.currentTimeMillis();
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
            log.error("Error processing update: updateId={}", update.getUpdateId(), e);
        }
    }

    @Override
    public void send(long telegramUserId, String text) {
        try {
            telegramClient.execute(SendMessage.builder().chatId(telegramUserId).parseMode("HTML").text(text).build());
        } catch (TelegramApiException e) {
            log.error("Failed to send notification to userId={}", telegramUserId, e);
        }
    }
}
