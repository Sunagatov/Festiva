package com.festiva.command.handler;

import com.festiva.command.CommandHandler;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class HelpCommandHandler implements CommandHandler {

    private static final String HELP_TEXT = """
            📖 <b>Команды Festiva:</b>
            
            👥 <b>Друзья:</b>
            /list — список друзей
            /add — добавить друга
            /remove — удалить друга
            
            🎂 <b>Дни рождения:</b>
            /birthdays — по месяцам
            /upcomingbirthdays — ближайшие
            /jubilee — юбилейные
            
            /cancel — отменить текущую операцию
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
