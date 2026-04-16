package com.festiva.command.handler;

import com.festiva.command.DatePickerKeyboard;
import com.festiva.command.MessageBuilder;
import com.festiva.command.StatefulCommandHandler;
import com.festiva.friend.api.FriendService;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.state.BotState;
import com.festiva.state.UserStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

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
        return Set.of(BotState.WAITING_FOR_ADD_FRIEND_NAME, BotState.WAITING_FOR_ADD_FRIEND_DATE, BotState.WAITING_FOR_ADD_FRIEND_RELATIONSHIP);
    }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        Lang lang = userStateService.getLanguage(userId);

        if (friendService.getFriends(userId).size() >= FriendService.FRIEND_CAP) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.FRIEND_CAP, FriendService.FRIEND_CAP));
        }

        userStateService.setState(userId, BotState.WAITING_FOR_ADD_FRIEND_NAME);
        return MessageBuilder.html(chatId, Messages.get(lang, Messages.ENTER_NAME));
    }

    @Override
    public SendMessage handleState(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        Lang lang = userStateService.getLanguage(userId);

        BotState state = userStateService.getState(userId);
        if (state != BotState.WAITING_FOR_ADD_FRIEND_NAME) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.USE_BUTTONS));
        }

        String name = update.getMessage().getText().trim();

        if (name.isBlank()) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.NAME_EMPTY));
        }
        if (name.length() > 100) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.NAME_TOO_LONG));
        }
        if (friendService.friendExists(userId, name)) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.NAME_EXISTS, name));
        }

        userStateService.setPendingName(userId, name);
        userStateService.setYearPageOffset(userId, DatePickerKeyboard.DEFAULT_YEAR_OFFSET);
        userStateService.setState(userId, BotState.WAITING_FOR_ADD_FRIEND_DATE);
        return MessageBuilder.html(chatId,
                Messages.get(lang, Messages.DATE_PICK_YEAR, name),
                DatePickerKeyboard.yearKeyboard(DatePickerKeyboard.DEFAULT_YEAR_OFFSET, lang));
    }
}