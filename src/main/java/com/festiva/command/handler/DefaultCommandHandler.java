package com.festiva.command.handler;

import com.festiva.command.CommandHandler;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class DefaultCommandHandler implements CommandHandler {

    @Override
    public String command() {
        return null;
    }

    @Override
    public SendMessage handle(Update update) {
        return SendMessage.builder()
                .chatId(update.getMessage().getChatId())
                .parseMode("HTML")
                .text("<b>Неизвестная команда.</b> Используйте /help для списка доступных команд.")
                .build();
    }
}
