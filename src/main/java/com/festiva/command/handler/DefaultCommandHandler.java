package com.festiva.command.handler;

import com.festiva.command.CommandHandler;
import com.festiva.command.MessageBuilder;
import com.festiva.i18n.Messages;
import com.festiva.state.UserStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@RequiredArgsConstructor
public class DefaultCommandHandler implements CommandHandler {

    private final UserStateService userStateService;

    @Override
    public String command() {
        return null;
    }

    @Override
    public SendMessage handle(Update update) {
        long userId = update.getMessage().getFrom().getId();
        return MessageBuilder.html(update.getMessage().getChatId(),
                Messages.get(userStateService.getLanguage(userId), Messages.UNKNOWN_COMMAND));
    }
}
