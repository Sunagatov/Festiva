package com.festiva.command.handler;

import com.festiva.command.CommandHandler;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.List;

@Component
public class BirthdaysCommandHandler implements CommandHandler {

    private static final int COLUMNS_PER_ROW = 4;
    private static final int TOTAL_MONTHS = 12;

    @Override
    public String command() {
        return "/birthdays";
    }

    @Override
    public SendMessage handle(Update update) {
        return SendMessage.builder()
                .chatId(update.getMessage().getChatId())
                .parseMode("HTML")
                .text("<b>Просмотр дней рождения</b>\n\nВыберите месяц, чтобы увидеть список дней рождения:")
                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(buildKeyboard()).build())
                .build();
    }

    private List<InlineKeyboardRow> buildKeyboard() {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(new InlineKeyboardRow(button("Текущий месяц", "MONTH_CURRENT")));

        InlineKeyboardRow row = new InlineKeyboardRow();
        for (int m = 1; m <= TOTAL_MONTHS; m++) {
            row.add(button(String.valueOf(m), "MONTH_" + m));
            if (m % COLUMNS_PER_ROW == 0) {
                rows.add(new InlineKeyboardRow(row));
                row.clear();
            }
        }
        if (!row.isEmpty()) rows.add(row);
        return rows;
    }

    private InlineKeyboardButton button(String text, String callbackData) {
        return InlineKeyboardButton.builder().text(text).callbackData(callbackData).build();
    }
}
