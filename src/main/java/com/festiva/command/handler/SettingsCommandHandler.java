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

    // Comprehensive timezone list organized by region and UTC offset
    private static final String[][] TIMEZONES = {
        // UTC
        {"UTC", "UTC"},
        
        // Europe
        {"Europe/London", "London (GMT)"},
        {"Europe/Paris", "Paris (CET)"},
        {"Europe/Berlin", "Berlin (CET)"},
        {"Europe/Rome", "Rome (CET)"},
        {"Europe/Madrid", "Madrid (CET)"},
        {"Europe/Amsterdam", "Amsterdam (CET)"},
        {"Europe/Brussels", "Brussels (CET)"},
        {"Europe/Vienna", "Vienna (CET)"},
        {"Europe/Warsaw", "Warsaw (CET)"},
        {"Europe/Prague", "Prague (CET)"},
        {"Europe/Athens", "Athens (EET)"},
        {"Europe/Bucharest", "Bucharest (EET)"},
        {"Europe/Helsinki", "Helsinki (EET)"},
        {"Europe/Istanbul", "Istanbul (TRT)"},
        {"Europe/Moscow", "Moscow (MSK)"},
        {"Europe/Kyiv", "Kyiv (EET)"},
        {"Europe/Minsk", "Minsk (MSK)"},
        
        // Asia
        {"Asia/Dubai", "Dubai (GST)"},
        {"Asia/Riyadh", "Riyadh (AST)"},
        {"Asia/Tehran", "Tehran (IRST)"},
        {"Asia/Baku", "Baku (AZT)"},
        {"Asia/Yerevan", "Yerevan (AMT)"},
        {"Asia/Tbilisi", "Tbilisi (GET)"},
        {"Asia/Kabul", "Kabul (AFT)"},
        {"Asia/Karachi", "Karachi (PKT)"},
        {"Asia/Kolkata", "Delhi/Mumbai (IST)"},
        {"Asia/Kathmandu", "Kathmandu (NPT)"},
        {"Asia/Dhaka", "Dhaka (BST)"},
        {"Asia/Almaty", "Almaty (ALMT)"},
        {"Asia/Tashkent", "Tashkent (UZT)"},
        {"Asia/Bishkek", "Bishkek (KGT)"},
        {"Asia/Yangon", "Yangon (MMT)"},
        {"Asia/Bangkok", "Bangkok (ICT)"},
        {"Asia/Jakarta", "Jakarta (WIB)"},
        {"Asia/Singapore", "Singapore (SGT)"},
        {"Asia/Hong_Kong", "Hong Kong (HKT)"},
        {"Asia/Shanghai", "Beijing/Shanghai (CST)"},
        {"Asia/Taipei", "Taipei (CST)"},
        {"Asia/Tokyo", "Tokyo (JST)"},
        {"Asia/Seoul", "Seoul (KST)"},
        
        // Australia & Pacific
        {"Australia/Perth", "Perth (AWST)"},
        {"Australia/Darwin", "Darwin (ACST)"},
        {"Australia/Adelaide", "Adelaide (ACDT)"},
        {"Australia/Brisbane", "Brisbane (AEST)"},
        {"Australia/Sydney", "Sydney (AEDT)"},
        {"Australia/Melbourne", "Melbourne (AEDT)"},
        {"Pacific/Auckland", "Auckland (NZDT)"},
        {"Pacific/Fiji", "Fiji (FJT)"},
        
        // Americas - North
        {"America/New_York", "New York (EST)"},
        {"America/Chicago", "Chicago (CST)"},
        {"America/Denver", "Denver (MST)"},
        {"America/Phoenix", "Phoenix (MST)"},
        {"America/Los_Angeles", "Los Angeles (PST)"},
        {"America/Anchorage", "Anchorage (AKST)"},
        {"Pacific/Honolulu", "Honolulu (HST)"},
        {"America/Toronto", "Toronto (EST)"},
        {"America/Vancouver", "Vancouver (PST)"},
        {"America/Mexico_City", "Mexico City (CST)"},
        
        // Americas - South
        {"America/Bogota", "Bogotá (COT)"},
        {"America/Lima", "Lima (PET)"},
        {"America/Caracas", "Caracas (VET)"},
        {"America/Santiago", "Santiago (CLT)"},
        {"America/Buenos_Aires", "Buenos Aires (ART)"},
        {"America/Sao_Paulo", "São Paulo (BRT)"},
        
        // Africa
        {"Africa/Cairo", "Cairo (EET)"},
        {"Africa/Johannesburg", "Johannesburg (SAST)"},
        {"Africa/Lagos", "Lagos (WAT)"},
        {"Africa/Nairobi", "Nairobi (EAT)"},
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
            if (row.size() == 2) { // 2 columns for better readability with longer names
                rows.add(row); 
                row = new InlineKeyboardRow(); 
            }
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
