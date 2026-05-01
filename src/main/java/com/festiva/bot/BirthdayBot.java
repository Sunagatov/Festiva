package com.festiva.bot;

import com.festiva.command.CommandRouter;
import com.festiva.i18n.Lang;
import com.festiva.metrics.MetricsSender;
import com.festiva.notification.NotificationSender;
import com.festiva.state.UserStateService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;


@Slf4j
@Component
@SuppressWarnings("unused")
public class BirthdayBot implements LongPollingSingleThreadUpdateConsumer, NotificationSender {

    private final String botToken;
    private final TelegramClient telegramClient;
    private TelegramBotsLongPollingApplication botsApplication;
    private final CommandRouter commandRouter;
    private final CallbackQueryHandler callbackQueryHandler;
    private final MetricsSender metricsSender;
    private final BotCommandsService commandsService;
    private final UserStateService userStateService;

    public BirthdayBot(CommandRouter commandRouter,
                       CallbackQueryHandler callbackQueryHandler,
                       TelegramClient telegramClient,
                       @Value("${telegram.bot.token}") String botToken,
                       MetricsSender metricsSender,
                       BotCommandsService commandsService,
                       UserStateService userStateService) {
        this.botToken = botToken;
        this.telegramClient = telegramClient;
        this.commandRouter = commandRouter;
        this.callbackQueryHandler = callbackQueryHandler;
        this.metricsSender = metricsSender;
        this.commandsService = commandsService;
        this.userStateService = userStateService;
    }

    @PostConstruct
    public void start() {
        try {
            botsApplication = new TelegramBotsLongPollingApplication();
            botsApplication.registerBot(botToken, this);
            log.atInfo()
                    .setMessage("telegram_bot_started")
                    .log();
        } catch (TelegramApiException e) {
            log.atError()
                    .setMessage("telegram_bot_start_failed")
                    .setCause(e)
                    .log();
            throw new RuntimeException("bot.start.failed", e);
        }
        commandsService.registerGlobalCommands();
    }

    @Override
    public void consume(Update update) {
        if (update == null) {
            log.atWarn()
                    .setMessage("telegram_update_missing")
                    .log();
            return;
        }

        if (update.hasCallbackQuery()) {
            String callbackId = update.getCallbackQuery().getId();
            try {
                telegramClient.execute(AnswerCallbackQuery.builder().callbackQueryId(callbackId).build());
            } catch (TelegramApiException e) {
                log.atWarn()
                        .setMessage("telegram_callback_ack_failed")
                        .addKeyValue("callbackId", callbackId)
                        .addKeyValue("userId", update.getCallbackQuery().getFrom().getId())
                        .setCause(e)
                        .log();
            }
        }

        processUpdate(update);
    }

    private void processUpdate(Update update) {
        long startTime = System.currentTimeMillis();
        String updateType = update.hasCallbackQuery() ? "callback" : update.hasMessage() ? "message" : "other";
        long userId = extractUserId(update);
        long chatId = extractChatId(update);
        try {
            if (update.hasCallbackQuery()) {
                EditMessageText edit = callbackQueryHandler.handle(update.getCallbackQuery());
                if (edit != null) {
                    try {
                        telegramClient.execute(edit);
                    } catch (TelegramApiException e) {
                        if (e.getMessage() == null || !e.getMessage().contains("message is not modified")) {
                            throw e;
                        }
                    }
                }
            } else if (update.hasMessage()) {
                SendMessage response = commandRouter.route(update);
                if (response != null) {
                    telegramClient.execute(response);
                }
            }
            metricsSender.sendMetrics(update, "SUCCESS", System.currentTimeMillis() - startTime);
        } catch (TelegramApiException | RuntimeException e) {
            metricsSender.sendMetrics(update, "ERROR", System.currentTimeMillis() - startTime);
            log.atError()
                    .setMessage("telegram_update_processing_failed")
                    .addKeyValue("updateId", update.getUpdateId())
                    .addKeyValue("updateType", updateType)
                    .addKeyValue("userId", userId)
                    .addKeyValue("chatId", chatId)
                    .setCause(e)
                    .log();

            try {
                if (chatId > 0 && userId > 0) {
                    Lang lang = userStateService.getLanguage(userId);
                    String errorMsg = lang == Lang.RU
                            ? "⚠️ Произошла ошибка. Попробуйте снова или используйте /cancel"
                            : "⚠️ An error occurred. Please try again or use /cancel";
                    telegramClient.execute(SendMessage.builder().chatId(chatId).text(errorMsg).build());
                }
            } catch (Exception fallbackError) {
                log.atError()
                        .setMessage("telegram_update_fallback_message_failed")
                        .addKeyValue("updateId", update.getUpdateId())
                        .addKeyValue("userId", userId)
                        .addKeyValue("chatId", chatId)
                        .setCause(fallbackError)
                        .log();
            }
        }
    }

    @PreDestroy
    public void stop() throws Exception {
        if (botsApplication != null) {
            botsApplication.close();
        }
    }

    @Override
    public boolean send(long telegramUserId, String text) {
        try {
            telegramClient.execute(SendMessage.builder().chatId(telegramUserId).parseMode("HTML").text(text).build());
            return true;
        } catch (TelegramApiException | RuntimeException e) {
            log.atError()
                    .setMessage("telegram_notification_send_failed")
                    .addKeyValue("userId", telegramUserId)
                    .setCause(e)
                    .log();
            return false;
        }
    }

    private long extractChatId(Update update) {
        if (update.hasCallbackQuery() && update.getCallbackQuery().getMessage() != null) {
            return update.getCallbackQuery().getMessage().getChatId();
        }
        if (update.hasMessage()) {
            return update.getMessage().getChatId();
        }
        return 0;
    }

    private long extractUserId(Update update) {
        if (update.hasCallbackQuery() && update.getCallbackQuery().getFrom() != null) {
            return update.getCallbackQuery().getFrom().getId();
        }
        if (update.hasMessage() && update.getMessage().getFrom() != null) {
            return update.getMessage().getFrom().getId();
        }
        return 0;
    }
}
