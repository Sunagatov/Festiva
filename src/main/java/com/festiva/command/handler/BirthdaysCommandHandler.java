package com.festiva.command.handler;

import com.festiva.command.CommandHandler;
import com.festiva.command.MessageBuilder;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.state.UserStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BirthdaysCommandHandler implements CommandHandler {

    private static final int COLUMNS_PER_ROW = 4;
    private static final int TOTAL_MONTHS = 12;

    private final UserStateService userStateService;

    @Override
    public String command() {
        return "/birthdays";
    }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        var lang = userStateService.getLanguage(userId);

        return MessageBuilder.html(chatId,
                Messages.get(lang, Messages.BIRTHDAYS_PICK),
                InlineKeyboardMarkup.builder().keyboard(buildKeyboard(lang)).build());
    }

    private List<InlineKeyboardRow> buildKeyboard(Lang lang) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(new InlineKeyboardRow(button(Messages.get(lang, Messages.CURRENT_MONTH), "MONTH_CURRENT")));

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
