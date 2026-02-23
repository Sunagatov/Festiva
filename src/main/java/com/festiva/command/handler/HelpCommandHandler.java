package com.festiva.command.handler;

import com.festiva.command.CommandHandler;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class HelpCommandHandler implements CommandHandler {

    private static final String HELP_TEXT = """
            <b>Список доступных команд:</b>
            
            <b>Основные команды:</b>
            /start - Запуск бота и вывод приветственного сообщения
            /help - Вывод этого списка команд
            
            <b>Управление списком друзей:</b>
            /list - Показать список друзей
            /add - Добавить нового друга
            /remove - Удалить существующего друга
            
            <b>Просмотр дней рождения:</b>
            /birthdays - Дни рождения по месяцам
            /upcomingbirthdays - Ближайшие дни рождения
            /jubilee - Юбилейные дни рождения
            
            <b>Отмена операций:</b>
            /cancel - Отмена текущей команды
            """;

    @Override
    public String command() {
        return "/help";
    }

    @Override
    public SendMessage handle(Update update) {
        return SendMessage.builder()
                .chatId(update.getMessage().getChatId())
                .parseMode("HTML")
                .text(HELP_TEXT)
                .build();
    }
}
