package com.festiva.command.handler;

import com.festiva.command.CommandHandler;
import com.festiva.state.BotState;
import com.festiva.state.UserStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@RequiredArgsConstructor
public class CancelCommandHandler implements CommandHandler {

    private final UserStateService userStateService;

    @Override
    public String command() {
        return "/cancel";
    }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.getMessage().getChatId();
        Long userId = update.getMessage().getFrom().getId();
        boolean active = userStateService.getState(userId) != BotState.IDLE;

        if (active) userStateService.clearState(userId);

        String text = active
                ? "<b><i>Текущая команда отменена. Чем ещё могу помочь? Отправьте /help для списка команд.</i></b>"
                : "<b><i>Нет активной команды для отмены. Я и так ничего не делал. Zzzzz...</i></b>";

        return SendMessage.builder().chatId(chatId).parseMode("HTML").text(text).build();
    }
}
