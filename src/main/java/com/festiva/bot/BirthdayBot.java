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
            log.info("bot.started");
        } catch (TelegramApiException e) {
            throw new RuntimeException("bot.start.failed", e);
        }
        commandsService.registerGlobalCommands();
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
                String callbackId = update.getCallbackQuery().getId();

                // Always answer callback query immediately
                try {
                    telegramClient.execute(AnswerCallbackQuery.builder().callbackQueryId(callbackId).build());
                } catch (TelegramApiException e) {
                    log.debug("bot.callback.answer.failed: callbackId={}", callbackId, e);
                }
                
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
            
            // Send user-visible error message
            try {
                long chatId = update.hasCallbackQuery() 
                    ? update.getCallbackQuery().getMessage().getChatId()
                    : update.hasMessage() ? update.getMessage().getChatId() : 0;
                long userId = update.hasCallbackQuery()
                    ? update.getCallbackQuery().getFrom().getId()
                    : update.hasMessage() ? update.getMessage().getFrom().getId() : 0;
                
                if (chatId > 0 && userId > 0) {
                    Lang lang = userStateService.getLanguage(userId);
                    String errorMsg = lang == Lang.RU 
                        ? "⚠️ Произошла ошибка. Попробуйте снова или используйте /cancel"
                        : "⚠️ An error occurred. Please try again or use /cancel";
                    telegramClient.execute(SendMessage.builder().chatId(chatId).text(errorMsg).build());
                }
            } catch (Exception fallbackError) {
                log.error("bot.error.fallback.failed", fallbackError);
            }
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
