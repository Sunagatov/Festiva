package com.festiva.command.handler;

import com.festiva.command.CommandHandler;
import com.festiva.command.MessageBuilder;
import com.festiva.i18n.Messages;
import com.festiva.state.BotState;
import com.festiva.state.UserStateService;
import com.festiva.user.api.UserPreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class CancelCommandHandler implements CommandHandler {

    private final UserStateService userStateService;
    private final UserPreferenceService userPreferenceService;

    @Override
    public String command() {
        return "/cancel";
    }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        boolean active = userStateService.getState(userId) != BotState.IDLE;

        if (active) {
            userStateService.clearState(userId);
        }

        String key = active ? Messages.CANCEL_ACTIVE : Messages.CANCEL_IDLE;
        var lang = userPreferenceService.getLanguage(userId);
        return MessageBuilder.html(chatId, Messages.get(lang, key), MessageBuilder.mainMenu(lang));
    }
}
