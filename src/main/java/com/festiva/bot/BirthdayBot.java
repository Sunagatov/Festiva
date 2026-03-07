package com.festiva.bot;

import com.festiva.command.CommandRouter;
import com.festiva.metrics.MetricsSender;
import com.festiva.notification.NotificationSender;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
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
public class BirthdayBot implements LongPollingSingleThreadUpdateConsumer, NotificationSender {

    private final String botToken;
    private final TelegramClient telegramClient;
    private final CommandRouter commandRouter;
    private final CallbackQueryHandler callbackQueryHandler;
    private final MetricsSender metricsSender;

    public BirthdayBot(CommandRouter commandRouter,
                       CallbackQueryHandler callbackQueryHandler,
                       TelegramClient telegramClient,
                       @Value("${telegram.bot.token}") String botToken,
                       MetricsSender metricsSender) {
        this.botToken = botToken;
        this.telegramClient = telegramClient;
        this.commandRouter = commandRouter;
        this.callbackQueryHandler = callbackQueryHandler;
        this.metricsSender = metricsSender;
    }

    @PostConstruct
    public void start() {
        try {
            new TelegramBotsLongPollingApplication().registerBot(botToken, this);
            log.info("bot.started");
        } catch (TelegramApiException e) {
            throw new RuntimeException("bot.start.failed", e);
        }
        try {
            telegramClient.execute(SetMyCommands.builder()
                    .commands(List.of(
                            new BotCommand("start",             "Start / Запустить"),
                            new BotCommand("list",              "Friends / Друзья"),
                            new BotCommand("add",               "Add friend / Добавить друга"),
                            new BotCommand("remove",            "Remove friend / Удалить друга"),
                            new BotCommand("edit",              "Edit friend / Редактировать друга"),
                            new BotCommand("search",            "Search / Поиск"),
                            new BotCommand("birthdays",         "By month / По месяцам"),
                            new BotCommand("today",             "Today's birthdays / Сегодня"),
                            new BotCommand("upcomingbirthdays", "Upcoming / Ближайшие"),
                            new BotCommand("jubilee",           "Milestones / Юбилеи"),
                            new BotCommand("stats",             "Stats / Статистика"),
                            new BotCommand("settings",          "Settings / Настройки"),
                            new BotCommand("language",          "Language / Язык"),
                            new BotCommand("help",              "Help / Помощь"),
                            new BotCommand("addmany",           "Bulk add / Добавить несколько"),
                            new BotCommand("export",            "Export / Экспорт"),
                            new BotCommand("deleteaccount",     "Delete my data / Удалить данные"),
                            new BotCommand("cancel",            "Cancel / Отмена")
                    ))
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

    @Override
    public void send(long telegramUserId, String text) {
        try {
            telegramClient.execute(SendMessage.builder().chatId(telegramUserId).parseMode("HTML").text(text).build());
        } catch (TelegramApiException e) {
            log.error("bot.notification.failed: userId={}, message={}", telegramUserId, e.getMessage(), e);
        }
    }
}
