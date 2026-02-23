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
        Long userId = update.getMessage().getFrom().getId();
        userStateService.setState(userId, BotState.WAITING_FOR_ADD_FRIEND_NAME);
        return response(update.getMessage().getChatId(), "Введите имя друга:");
    }

    public SendMessage handleAwaitingName(Update update) {
        long chatId = update.getMessage().getChatId();
        Long userId = update.getMessage().getFrom().getId();
        String name = update.getMessage().getText().trim();

        if (name.isEmpty()) {
            return response(chatId, "Имя не может быть пустым. Введите имя или /cancel для отмены.");
        }
        if (friendService.friendExists(userId, name)) {
            return response(chatId, "Друг с именем \"" + name + "\" уже существует. Введите другое имя или /cancel.");
        }

        userStateService.setPendingName(userId, name);
        userStateService.setState(userId, BotState.WAITING_FOR_ADD_FRIEND_DATE);
        return response(chatId, "Введите дату рождения " + name + " в формате ДД.ММ.ГГГГ\nНапример: 15.03.1990");
    }

    public SendMessage handleAwaitingDate(Update update) {
        long chatId = update.getMessage().getChatId();
        Long userId = update.getMessage().getFrom().getId();
        String dateStr = update.getMessage().getText().trim();
        String name = userStateService.getPendingName(userId);

        if (name == null) {
            userStateService.clearState(userId);
            return response(chatId, "Что-то пошло не так. Начните заново с /add.");
        }

        LocalDate birthDate;
        try {
            birthDate = LocalDate.parse(dateStr, java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        } catch (DateTimeParseException e) {
            return response(chatId, "Неверный формат даты. Используйте ДД.ММ.ГГГГ, например: 15.03.1990");
        }

        if (birthDate.isAfter(LocalDate.now())) {
            return response(chatId, "Дата рождения не может быть в будущем.");
        }

        friendService.addFriend(userId, new Friend(name, birthDate));
        userStateService.clearState(userId);
        return response(chatId, "✅ " + name + " добавлен!");
    }

    private SendMessage response(long chatId, String text) {
        return SendMessage.builder().chatId(chatId).text(text).build();
    }
}
