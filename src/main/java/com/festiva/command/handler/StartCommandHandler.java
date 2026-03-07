package com.festiva.command.handler;

import com.festiva.command.CommandHandler;
import com.festiva.command.MessageBuilder;
import com.festiva.friend.api.FriendService;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.state.UserStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@RequiredArgsConstructor
public class StartCommandHandler implements CommandHandler {

    private final UserStateService userStateService;
    private final FriendService friendService;

    @Override
    public String command() { return "/start"; }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        Lang lang = userStateService.getLanguage(userId);
        String welcome = Messages.get(lang, Messages.WELCOME);

        if (friendService.getFriends(userId).isEmpty()) {
            return MessageBuilder.html(chatId, welcome, MessageBuilder.mainMenu());
        }
        return MessageBuilder.html(chatId, welcome, MessageBuilder.mainMenu());
    }
}
