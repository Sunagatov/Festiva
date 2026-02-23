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

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Component
@RequiredArgsConstructor
public class AddFriendCommandHandler implements CommandHandler {

    private final FriendService friendService;
    private final UserStateService userStateService;

    @Override
    public String command() {
        return "/add";
    }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.getMessage().getChatId();
        Long userId = update.getMessage().getFrom().getId();

        userStateService.setState(userId, BotState.WAITING_FOR_ADD_FRIEND_INPUT);
        return response(chatId, "Введите имя и дату рождения следующим образом:\nИмя гггг-мм-дд");
    }

    public SendMessage handleAwaitingInput(Update update) {
        long chatId = update.getMessage().getChatId();
        Long userId = update.getMessage().getFrom().getId();
        String text = update.getMessage().getText().trim();

        int lastSpace = text.lastIndexOf(" ");
        if (lastSpace <= 0) {
            return response(chatId, "Неверный формат. Используйте: Имя гггг-мм-дд");
        }

        String name = text.substring(0, lastSpace).trim();
        String dateStr = text.substring(lastSpace + 1).trim();

        if (name.isEmpty()) {
            return response(chatId, "Имя не может быть пустым.");
        }
        if (friendService.friendExists(userId, name)) {
            return response(chatId, "Друг с таким именем уже существует.");
        }

        LocalDate birthDate;
        try {
            birthDate = LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            return response(chatId, "Неверный формат даты. Используйте: гггг-мм-дд");
        }

        if (birthDate.isAfter(LocalDate.now())) {
            return response(chatId, "Дата рождения не может быть в будущем.");
        }

        friendService.addFriend(userId, new Friend(name, birthDate));
        userStateService.clearState(userId);
        return response(chatId, "Пользователь " + name + " успешно добавлен!");
    }

    private SendMessage response(long chatId, String text) {
        return SendMessage.builder().chatId(chatId).text(text).build();
    }
}
