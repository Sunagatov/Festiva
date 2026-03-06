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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class RemoveCommandHandler implements StatefulCommandHandler {

    private final FriendService friendService;
    private final UserStateService userStateService;

    @Override
    public String command() {
        return "/remove";
    }

    @Override
    public Set<BotState> handledStates() {
        return Set.of(BotState.WAITING_FOR_REMOVE_FRIEND_INPUT);
    }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        Lang lang = userStateService.getLanguage(userId);
        List<Friend> friends = friendService.getFriendsSortedByDayMonth(userId);

        if (friends.isEmpty()) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.FRIENDS_EMPTY));
        }

        List<InlineKeyboardRow> rows = friends.stream()
                .map(f -> new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text(f.getName())
                                .callbackData("REMOVE_" + f.getName())
                                .build()))
                .toList();

        userStateService.setState(userId, BotState.WAITING_FOR_REMOVE_FRIEND_INPUT);
        return MessageBuilder.html(chatId, Messages.get(lang, Messages.SELECT_REMOVE),
                InlineKeyboardMarkup.builder().keyboard(rows).build());
    }

    @Override
    public SendMessage handleState(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        Lang lang = userStateService.getLanguage(userId);
        String name = update.getMessage().getText().trim();

        if (name.isBlank()) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.NAME_EMPTY));
        }
        if (!friendService.friendExists(userId, name)) {
            log.debug("friend.remove.not_found: userId={}, name={}", userId, name);
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.FRIEND_NOT_FOUND, name));
        }

        friendService.deleteFriend(userId, name);
        userStateService.clearState(userId);
        log.debug("friend.removed: userId={}, name={}", userId, name);
        return MessageBuilder.html(chatId, Messages.get(lang, Messages.FRIEND_REMOVED, name));
    }
}
