package com.festiva.bot;

import com.festiva.command.CommandRouter;
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
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

@Slf4j
@Component
public class BirthdayBot implements LongPollingSingleThreadUpdateConsumer, NotificationSender {

    private final String botToken;
    private final TelegramClient telegramClient;
    private TelegramBotsLongPollingApplication botsApplication;
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
            botsApplication = new TelegramBotsLongPollingApplication();
            botsApplication.registerBot(botToken, this);
            log.info("bot.started");
        } catch (TelegramApiException e) {
            throw new RuntimeException("bot.start.failed", e);
        }
        try {
            List<BotCommand> commands = List.of(
                    new BotCommand("start",             "Start"),
                    new BotCommand("add",               "Add friend"),
                    new BotCommand("addmany",           "Bulk add friends"),
                    new BotCommand("list",              "List friends"),
                    new BotCommand("edit",              "Edit friend"),
                    new BotCommand("remove",            "Remove friend"),
                    new BotCommand("search",            "Search friends"),
                    new BotCommand("birthdays",         "Birthdays by month"),
                    new BotCommand("today",             "Today's birthdays"),
                    new BotCommand("upcomingbirthdays", "Upcoming birthdays"),
                    new BotCommand("jubilee",           "Milestone birthdays"),
                    new BotCommand("stats",             "Statistics"),
                    new BotCommand("export",            "Export friends"),
                    new BotCommand("settings",          "Settings"),
                    new BotCommand("language",          "Change language"),
                    new BotCommand("menu",              "Show all commands"),
                    new BotCommand("about",             "About Festiva"),
                    new BotCommand("deleteaccount",     "Delete my data"),
                    new BotCommand("cancel",            "Cancel"),
                    new BotCommand("importics",         "Import from Google Calendar")
            );
            List<BotCommand> commandsRu = List.of(
                    new BotCommand("start",             "Запустить"),
                    new BotCommand("add",               "Добавить друга"),
                    new BotCommand("addmany",           "Добавить несколько"),
                    new BotCommand("list",              "Список друзей"),
                    new BotCommand("edit",              "Редактировать друга"),
                    new BotCommand("remove",            "Удалить друга"),
                    new BotCommand("search",            "Поиск друзей"),
                    new BotCommand("birthdays",         "Дни рождения по месяцам"),
                    new BotCommand("today",             "Сегодняшние дни рождения"),
                    new BotCommand("upcomingbirthdays", "Ближайшие дни рождения"),
                    new BotCommand("jubilee",           "Юбилейные дни рождения"),
                    new BotCommand("stats",             "Статистика"),
                    new BotCommand("export",            "Экспорт друзей"),
                    new BotCommand("settings",          "Настройки"),
                    new BotCommand("language",          "Сменить язык"),
                    new BotCommand("menu",              "Все команды"),
                    new BotCommand("about",             "О боте"),
                    new BotCommand("deleteaccount",     "Удалить данные"),
                    new BotCommand("cancel",            "Отмена"),
                    new BotCommand("importics",         "Импорт из Google Календаря")
            );
            telegramClient.execute(SetMyCommands.builder().commands(commands).build());
            telegramClient.execute(SetMyCommands.builder().commands(commandsRu)
                    .languageCode("ru").build());
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
}
