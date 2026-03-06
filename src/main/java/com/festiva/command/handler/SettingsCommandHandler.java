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
public class SettingsCommandHandler implements CommandHandler {

    public static final String SETTINGS_HOUR_PREFIX = "SETTINGS_HOUR_";

    private final UserStateService userStateService;

    @Override
    public String command() { return "/settings"; }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        Lang lang = userStateService.getLanguage(userId);
        int currentHour = userStateService.getNotifyHour(userId);
        return MessageBuilder.html(chatId, Messages.get(lang, Messages.SETTINGS_HEADER), hourKeyboard(currentHour));
    }

    public static InlineKeyboardMarkup hourKeyboard(int activeHour) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        InlineKeyboardRow row = new InlineKeyboardRow();
        for (int h : new int[]{7, 8, 9, 10, 11, 12}) {
            row.add(btn(h, activeHour));
            if (row.size() == 3) { rows.add(row); row = new InlineKeyboardRow(); }
        }
        if (!row.isEmpty()) rows.add(row);
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private static InlineKeyboardButton btn(int hour, int activeHour) {
        String label = (hour == activeHour ? "✅ " : "") + String.format("%02d:00", hour);
        return InlineKeyboardButton.builder().text(label).callbackData(SETTINGS_HOUR_PREFIX + hour).build();
    }
}
