package com.festiva.command.handler;

import com.festiva.command.CommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class HelpAliasCommandHandler implements CommandHandler {

    private final HelpCommandHandler delegate;

    @Override
    public String command() { return "/help"; }

    @Override
    public SendMessage handle(Update update) {
        return delegate.handle(update);
    }
}
