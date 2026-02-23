package com.festiva.command.handler;

import com.festiva.command.CommandHandler;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class StartCommandHandler implements CommandHandler {

    private static final String WELCOME_TEXT = """
            <b>Добро пожаловать!</b>
            Я бот для учета дней рождения. Я помогу вам управлять списком друзей и отслеживать их дни рождения.
            
            <b>Основные команды:</b>
            /start - Запуск бота и вывод этого сообщения
            /help - Вывод списка команд
            
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
        return "/start";
    }

    @Override
    public SendMessage handle(Update update) {
        return SendMessage.builder()
                .chatId(update.getMessage().getChatId())
                .parseMode("HTML")
                .text(WELCOME_TEXT)
                .build();
    }
}
