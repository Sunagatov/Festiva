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
    public static final String SETTINGS_TZ_PREFIX   = "SETTINGS_TZ_";

    private static final String[][] TIMEZONES = {
        {"UTC",             "UTC"},
        {"Europe/Moscow",   "Moscow"},
        {"Europe/London",   "London"},
        {"Europe/Berlin",   "Berlin"},
        {"America/New_York","New York"},
        {"America/Chicago", "Chicago"},
        {"America/Denver",  "Denver"},
        {"America/Los_Angeles", "LA"},
        {"Asia/Dubai",      "Dubai"},
        {"Asia/Almaty",     "Almaty"},
        {"Asia/Tashkent",   "Tashkent"},
        {"Asia/Tokyo",      "Tokyo"}
    };

    private final UserStateService userStateService;

    @Override
    public String command() { return "/settings"; }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        Lang lang = userStateService.getLanguage(userId);
        int currentHour = userStateService.getNotifyHour(userId);
        String currentTz = userStateService.getTimezone(userId);
        String text = Messages.get(lang, Messages.SETTINGS_HEADER) + "\n\n" +
                      Messages.get(lang, Messages.SETTINGS_TZ_HEADER);
        InlineKeyboardMarkup keyboard = combined(currentHour, currentTz);
        return MessageBuilder.html(chatId, text, keyboard);
    }

    public static InlineKeyboardMarkup combined(int activeHour, String activeTz) {
        List<InlineKeyboardRow> rows = new ArrayList<>(hourKeyboard(activeHour).getKeyboard());
        rows.addAll(tzKeyboard(activeTz).getKeyboard());
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public static InlineKeyboardMarkup tzKeyboard(String activeTz) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        InlineKeyboardRow row = new InlineKeyboardRow();
        for (String[] tz : TIMEZONES) {
            String label = (tz[0].equals(activeTz) ? "✅ " : "") + tz[1];
            row.add(InlineKeyboardButton.builder().text(label).callbackData(SETTINGS_TZ_PREFIX + tz[0]).build());
            if (row.size() == 3) { rows.add(row); row = new InlineKeyboardRow(); }
        }
        if (!row.isEmpty()) rows.add(row);
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public static InlineKeyboardMarkup hourKeyboard(int activeHour) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        InlineKeyboardRow row = new InlineKeyboardRow();
        for (int h = 0; h < 24; h++) {
            row.add(btn(h, activeHour));
            if (row.size() == 4) { rows.add(row); row = new InlineKeyboardRow(); }
        }
        if (!row.isEmpty()) rows.add(row);
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private static InlineKeyboardButton btn(int hour, int activeHour) {
        String label = (hour == activeHour ? "✅ " : "") + String.format("%02d:00", hour);
        return InlineKeyboardButton.builder().text(label).callbackData(SETTINGS_HOUR_PREFIX + hour).build();
    }
}
