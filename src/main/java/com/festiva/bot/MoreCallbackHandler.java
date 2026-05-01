package com.festiva.bot;

import com.festiva.command.CommandHandler;
import com.festiva.command.handler.MoreCommandHandler;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@SuppressWarnings("unused")
public class MoreCallbackHandler {

    private final Map<String, CommandHandler> handlers;

    public MoreCallbackHandler(List<CommandHandler> allHandlers) {
        this.handlers = allHandlers.stream()
                .filter(handler -> handler.command() != null)
                .collect(Collectors.toMap(CommandHandler::command, Function.identity()));
    }

    public CallbackResult handle(String data, long chatId, long userId, Lang lang) {
        if (MoreCommandHandler.CALLBACK_BACK.equals(data)) {
            return new CallbackResult(Messages.get(lang, Messages.MORE_HEADER), MoreCommandHandler.keyboard(lang));
        }

        String command = MoreCommandHandler.commandForCallback(data);
        if (command == null) {
            return null;
        }

        CommandHandler handler = handlers.get(command);
        if (handler == null) {
            return null;
        }

        SendMessage response = handler.handle(syntheticUpdate(chatId, userId, command));
        if (response == null) {
            return new CallbackResult(Messages.get(lang, Messages.MORE_HEADER), MoreCommandHandler.keyboard(lang));
        }
        return new CallbackResult(response);
    }

    private Update syntheticUpdate(long chatId, long userId, String command) {
        User user = new User(userId, "Festiva", false);
        Chat chat = new Chat(chatId, "private");

        Message message = new Message();
        message.setChat(chat);
        message.setText(command);
        message.setFrom(user);

        Update update = new Update();
        update.setMessage(message);
        return update;
    }
}
