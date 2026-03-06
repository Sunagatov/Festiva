package com.festiva.command.handler;

import com.festiva.command.MessageBuilder;
import com.festiva.command.StatefulCommandHandler;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.state.BotState;
import com.festiva.state.UserStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AddFriendCommandHandler implements StatefulCommandHandler {

    private final FriendService friendService;
    private final UserStateService userStateService;

    @Override
    public String command() {
        return "/add";
    }

    @Override
    public Set<BotState> handledStates() {
        return Set.of(BotState.WAITING_FOR_ADD_FRIEND_NAME, BotState.WAITING_FOR_ADD_FRIEND_DATE);
    }

    @Override
    public SendMessage handle(Update update) {
        long userId = update.getMessage().getFrom().getId();
        userStateService.setState(userId, BotState.WAITING_FOR_ADD_FRIEND_NAME);
        return MessageBuilder.html(update.getMessage().getChatId(),
                Messages.get(userStateService.getLanguage(userId), Messages.ENTER_NAME));
    }

    @Override
    public SendMessage handleState(Update update) {
        BotState state = userStateService.getState(update.getMessage().getFrom().getId());
        return state == BotState.WAITING_FOR_ADD_FRIEND_DATE
                ? handleAwaitingDate(update)
                : handleAwaitingName(update);
    }

    private SendMessage handleAwaitingName(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        Lang lang = userStateService.getLanguage(userId);
        String name = update.getMessage().getText().trim();

        if (name.isEmpty()) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.NAME_EMPTY));
        }
        if (friendService.friendExists(userId, name)) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.NAME_EXISTS, name));
        }

        userStateService.setPendingName(userId, name);
        userStateService.setState(userId, BotState.WAITING_FOR_ADD_FRIEND_DATE);
        return MessageBuilder.html(chatId, Messages.get(lang, Messages.ENTER_DATE, name));
    }

    private SendMessage handleAwaitingDate(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        Lang lang = userStateService.getLanguage(userId);
        String name = userStateService.getPendingName(userId);

        if (name == null) {
            userStateService.clearState(userId);
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.ADD_ERROR));
        }

        LocalDate birthDate;
        try {
            birthDate = LocalDate.parse(update.getMessage().getText().trim(), MessageBuilder.DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.DATE_FORMAT_ERROR));
        }

        if (birthDate.isAfter(LocalDate.now())) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.DATE_FUTURE_ERROR));
        }

        friendService.addFriend(userId, new Friend(name, birthDate));
        userStateService.clearState(userId);
        return MessageBuilder.html(chatId, Messages.get(lang, Messages.FRIEND_ADDED, name));
    }
}
