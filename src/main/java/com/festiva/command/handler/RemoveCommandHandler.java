package com.festiva.command.handler;

import com.festiva.command.CommandHandler;
import com.festiva.friend.api.FriendService;
import com.festiva.state.BotState;
import com.festiva.state.UserStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

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
        Long userId = update.getMessage().getFrom().getId();
        userStateService.setState(userId, BotState.WAITING_FOR_REMOVE_FRIEND_INPUT);
        return response(update.getMessage().getChatId(),
                "<b><i>Введите имя пользователя, которого необходимо удалить:</i></b>");
    }

    public SendMessage handleAwaitingInput(Update update) {
        long chatId = update.getMessage().getChatId();
        Long userId = update.getMessage().getFrom().getId();
        String name = update.getMessage().getText().trim();

        if (name.isEmpty()) {
            return response(chatId, "<b><i>Имя не может быть пустым. Введите имя или /cancel для отмены.</i></b>");
        }
        if (!friendService.friendExists(userId, name)) {
            return response(chatId, "<b><i>Пользователь с именем \"" + name +
                    "\" не найден. Введите другое имя или /cancel для отмены.</i></b>");
        }

        friendService.deleteFriend(userId, name);
        userStateService.clearState(userId);
        return response(chatId, "<b><i>Пользователь \"" + name + "\" успешно удалён!</i></b>");
    }

    private SendMessage response(long chatId, String text) {
        return SendMessage.builder().chatId(chatId).parseMode("HTML").text(text).build();
    }
}
