package com.festiva.command.handler;

import com.festiva.command.CommandHandler;
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class EditFriendCommandHandler implements StatefulCommandHandler {

    private final FriendService friendService;
    private final UserStateService userStateService;

    @Override
    public String command() { return "/edit"; }

    @Override
    public Set<BotState> handledStates() { return Set.of(BotState.WAITING_FOR_EDIT_NAME); }

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
                                .text(f.getName() + " (" + f.getBirthDate().format(MessageBuilder.DATE_FORMATTER) + ")")
                                .callbackData("EDIT_" + f.getName())
                                .build()))
                .toList();

        return MessageBuilder.html(chatId, Messages.get(lang, Messages.EDIT_SELECT),
                InlineKeyboardMarkup.builder().keyboard(rows).build());
    }

    @Override
    public SendMessage handleState(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        Lang lang = userStateService.getLanguage(userId);
        String newName = update.getMessage().getText().trim();
        String oldName = userStateService.getPendingName(userId);

        if (newName.isBlank()) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.NAME_EMPTY));
        }
        if (friendService.friendExists(userId, newName)) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.NAME_EXISTS, newName));
        }

        friendService.updateFriendName(userId, oldName, newName);
        userStateService.clearState(userId);
        return MessageBuilder.html(chatId, Messages.get(lang, Messages.EDIT_NAME_DONE, newName));
    }
}
