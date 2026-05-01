package com.festiva.command.handler;

import com.festiva.command.CommandHandler;
import com.festiva.command.MessageBuilder;
import com.festiva.i18n.Messages;
import com.festiva.state.UserStateService;
import com.festiva.user.api.UserPreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class AboutCommandHandler implements CommandHandler {

    private final UserStateService userStateService;
    private final UserPreferenceService userPreferenceService;

    @Override
    public String command() { return "/about"; }

    @Override
    public SendMessage handle(Update update) {
        long userId = update.getMessage().getFrom().getId();

        userStateService.clearState(userId);

        return MessageBuilder.html(update.getMessage().getChatId(),
                Messages.get(userPreferenceService.getLanguage(userId), Messages.ABOUT));
    }
}
