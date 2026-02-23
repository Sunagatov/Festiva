package com.festiva.command.handler;

import com.festiva.command.CommandHandler;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
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

@Component
@RequiredArgsConstructor
public class RemoveCommandHandler implements CommandHandler {

    private final FriendService friendService;
    private final UserStateService userStateService;

    @Override
    public String command() {
        return "/remove";
    }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.getMessage().getChatId();
        Long userId = update.getMessage().getFrom().getId();
        List<Friend> friends = friendService.getFriendsSortedByDayMonth(userId);

        if (friends.isEmpty()) {
            return response(chatId, "Список друзей пуст.", null);
        }

        List<InlineKeyboardRow> rows = friends.stream()
                .map(f -> new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text(f.getName())
                                .callbackData("REMOVE_" + f.getName())
                                .build()))
                .toList();

        userStateService.setState(userId, BotState.WAITING_FOR_REMOVE_FRIEND_INPUT);
        return response(chatId, "Выберите друга для удаления:",
                InlineKeyboardMarkup.builder().keyboard(rows).build());
    }

    public SendMessage handleAwaitingInput(Update update) {
        long chatId = update.getMessage().getChatId();
        Long userId = update.getMessage().getFrom().getId();
        String name = update.getMessage().getText().trim();

        if (name.isEmpty()) {
            return response(chatId, "Имя не может быть пустым. Введите имя или /cancel.", null);
        }
        if (!friendService.friendExists(userId, name)) {
            return response(chatId, "Друг \"" + name + "\" не найден. Введите другое имя или /cancel.", null);
        }

        friendService.deleteFriend(userId, name);
        userStateService.clearState(userId);
        return response(chatId, "✅ \"" + name + "\" удалён!", null);
    }

    private SendMessage response(long chatId, String text, InlineKeyboardMarkup markup) {
        SendMessage.SendMessageBuilder builder = SendMessage.builder().chatId(chatId).text(text);
        if (markup != null) builder.replyMarkup(markup);
        return builder.build();
    }
}
