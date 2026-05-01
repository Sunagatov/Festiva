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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class SettingsCommandHandler implements CommandHandler {

    public static final String SETTINGS_HOUR_PREFIX = "SETTINGS_HOUR_";
    public static final String SETTINGS_TZ_PREFIX   = "SETTINGS_TZ_";
    public static final String SETTINGS_TZ_REGION_PREFIX = "SETTINGS_TZ_REGION_";

    public static final String REGION_EUROPE = "EUROPE";
    public static final String REGION_ASIA = "ASIA";
    public static final String REGION_AMERICAS = "AMERICAS";
    public static final String REGION_AFRICA = "AFRICA";
    public static final String REGION_PACIFIC = "PACIFIC";
    public static final String REGION_UTC = "UTC";

    private static final Map<String, TimezoneRegion> TIMEZONE_REGIONS = new LinkedHashMap<>();
    static {
        TIMEZONE_REGIONS.put(REGION_EUROPE, new TimezoneRegion(
                REGION_EUROPE, "\uD83C\uDF0D Europe", "\uD83C\uDF0D Европа", List.of(
                tz("Europe/London", "London (GMT)"),
                tz("Europe/Paris", "Paris (CET)"),
                tz("Europe/Berlin", "Berlin (CET)"),
                tz("Europe/Rome", "Rome (CET)"),
                tz("Europe/Madrid", "Madrid (CET)"),
                tz("Europe/Amsterdam", "Amsterdam (CET)"),
                tz("Europe/Brussels", "Brussels (CET)"),
                tz("Europe/Vienna", "Vienna (CET)"),
                tz("Europe/Warsaw", "Warsaw (CET)"),
                tz("Europe/Prague", "Prague (CET)"),
                tz("Europe/Athens", "Athens (EET)"),
                tz("Europe/Bucharest", "Bucharest (EET)"),
                tz("Europe/Helsinki", "Helsinki (EET)"),
                tz("Europe/Istanbul", "Istanbul (TRT)"),
                tz("Europe/Moscow", "Moscow (MSK)"),
                tz("Europe/Kyiv", "Kyiv (EET)"),
                tz("Europe/Minsk", "Minsk (MSK)")
        )));
        TIMEZONE_REGIONS.put(REGION_ASIA, new TimezoneRegion(
                REGION_ASIA, "\uD83C\uDF0F Asia", "\uD83C\uDF0F Азия", List.of(
                tz("Asia/Dubai", "Dubai (GST)"),
                tz("Asia/Riyadh", "Riyadh (AST)"),
                tz("Asia/Tehran", "Tehran (IRST)"),
                tz("Asia/Baku", "Baku (AZT)"),
                tz("Asia/Yerevan", "Yerevan (AMT)"),
                tz("Asia/Tbilisi", "Tbilisi (GET)"),
                tz("Asia/Kabul", "Kabul (AFT)"),
                tz("Asia/Karachi", "Karachi (PKT)"),
                tz("Asia/Kolkata", "Delhi/Mumbai (IST)"),
                tz("Asia/Kathmandu", "Kathmandu (NPT)"),
                tz("Asia/Dhaka", "Dhaka (BST)"),
                tz("Asia/Almaty", "Almaty (ALMT)"),
                tz("Asia/Tashkent", "Tashkent (UZT)"),
                tz("Asia/Bishkek", "Bishkek (KGT)"),
                tz("Asia/Yangon", "Yangon (MMT)"),
                tz("Asia/Bangkok", "Bangkok (ICT)"),
                tz("Asia/Jakarta", "Jakarta (WIB)"),
                tz("Asia/Singapore", "Singapore (SGT)"),
                tz("Asia/Hong_Kong", "Hong Kong (HKT)"),
                tz("Asia/Shanghai", "Beijing/Shanghai (CST)"),
                tz("Asia/Taipei", "Taipei (CST)"),
                tz("Asia/Tokyo", "Tokyo (JST)"),
                tz("Asia/Seoul", "Seoul (KST)")
        )));
        TIMEZONE_REGIONS.put(REGION_AMERICAS, new TimezoneRegion(
                REGION_AMERICAS, "\uD83C\uDF0E Americas", "\uD83C\uDF0E Америка", List.of(
                tz("America/New_York", "New York (EST)"),
                tz("America/Chicago", "Chicago (CST)"),
                tz("America/Denver", "Denver (MST)"),
                tz("America/Phoenix", "Phoenix (MST)"),
                tz("America/Los_Angeles", "Los Angeles (PST)"),
                tz("America/Anchorage", "Anchorage (AKST)"),
                tz("Pacific/Honolulu", "Honolulu (HST)"),
                tz("America/Toronto", "Toronto (EST)"),
                tz("America/Vancouver", "Vancouver (PST)"),
                tz("America/Mexico_City", "Mexico City (CST)"),
                tz("America/Bogota", "Bogota (COT)"),
                tz("America/Lima", "Lima (PET)"),
                tz("America/Caracas", "Caracas (VET)"),
                tz("America/Santiago", "Santiago (CLT)"),
                tz("America/Buenos_Aires", "Buenos Aires (ART)"),
                tz("America/Sao_Paulo", "Sao Paulo (BRT)")
        )));
        TIMEZONE_REGIONS.put(REGION_AFRICA, new TimezoneRegion(
                REGION_AFRICA, "\uD83C\uDF0D Africa", "\uD83C\uDF0D Африка", List.of(
                tz("Africa/Cairo", "Cairo (EET)"),
                tz("Africa/Johannesburg", "Johannesburg (SAST)"),
                tz("Africa/Lagos", "Lagos (WAT)"),
                tz("Africa/Nairobi", "Nairobi (EAT)")
        )));
        TIMEZONE_REGIONS.put(REGION_PACIFIC, new TimezoneRegion(
                REGION_PACIFIC, "\uD83C\uDF10 Pacific", "\uD83C\uDF10 Тихий океан", List.of(
                tz("Australia/Perth", "Perth (AWST)"),
                tz("Australia/Darwin", "Darwin (ACST)"),
                tz("Australia/Adelaide", "Adelaide (ACDT)"),
                tz("Australia/Brisbane", "Brisbane (AEST)"),
                tz("Australia/Sydney", "Sydney (AEDT)"),
                tz("Australia/Melbourne", "Melbourne (AEDT)"),
                tz("Pacific/Auckland", "Auckland (NZDT)"),
                tz("Pacific/Fiji", "Fiji (FJT)")
        )));
        TIMEZONE_REGIONS.put(REGION_UTC, new TimezoneRegion(
                REGION_UTC, "UTC", "UTC", List.of(
                tz("UTC", "UTC")
        )));
    }

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
        InlineKeyboardMarkup keyboard = combined(currentHour, currentTz, lang);
        return MessageBuilder.html(chatId, text, keyboard);
    }

    public static InlineKeyboardMarkup combined(int activeHour, String activeTz, Lang lang) {
        return combined(activeHour, activeTz, lang, null);
    }

    public static InlineKeyboardMarkup combined(int activeHour, String activeTz, Lang lang, String expandedRegion) {
        List<InlineKeyboardRow> rows = new ArrayList<>(hourKeyboard(activeHour).getKeyboard());
        rows.addAll(tzRegionKeyboard(lang, expandedRegion).getKeyboard());
        if (expandedRegion != null) {
            rows.addAll(tzKeyboard(expandedRegion, activeTz).getKeyboard());
        }
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public static InlineKeyboardMarkup tzRegionKeyboard(Lang lang, String expandedRegion) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        InlineKeyboardRow first = new InlineKeyboardRow();
        first.add(regionButton(TIMEZONE_REGIONS.get(REGION_EUROPE), lang, expandedRegion));
        first.add(regionButton(TIMEZONE_REGIONS.get(REGION_ASIA), lang, expandedRegion));
        first.add(regionButton(TIMEZONE_REGIONS.get(REGION_AMERICAS), lang, expandedRegion));
        rows.add(first);

        InlineKeyboardRow second = new InlineKeyboardRow();
        second.add(regionButton(TIMEZONE_REGIONS.get(REGION_AFRICA), lang, expandedRegion));
        second.add(regionButton(TIMEZONE_REGIONS.get(REGION_PACIFIC), lang, expandedRegion));
        second.add(regionButton(TIMEZONE_REGIONS.get(REGION_UTC), lang, expandedRegion));
        rows.add(second);

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public static InlineKeyboardMarkup tzKeyboard(String regionKey, String activeTz) {
        TimezoneRegion region = TIMEZONE_REGIONS.get(regionKey);
        if (region == null) {
            return InlineKeyboardMarkup.builder().keyboard(List.of()).build();
        }

        List<InlineKeyboardRow> rows = new ArrayList<>();
        InlineKeyboardRow row = new InlineKeyboardRow();
        for (TimezoneOption tz : region.timezones()) {
            String label = (tz.id().equals(activeTz) ? "✅ " : "") + tz.label();
            row.add(InlineKeyboardButton.builder().text(label).callbackData(SETTINGS_TZ_PREFIX + tz.id()).build());
            if (row.size() == 2) {
                rows.add(row);
                row = new InlineKeyboardRow();
            }
        }
        if (!row.isEmpty()) rows.add(row);
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public static String regionForTimezone(String timezone) {
        for (TimezoneRegion region : TIMEZONE_REGIONS.values()) {
            for (TimezoneOption option : region.timezones()) {
                if (option.id().equals(timezone)) {
                    return region.key();
                }
            }
        }
        return null;
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

    private static InlineKeyboardButton regionButton(TimezoneRegion region, Lang lang, String expandedRegion) {
        String label = lang == Lang.RU ? region.labelRu() : region.labelEn();
        if (region.key().equals(expandedRegion)) {
            label = "✅ " + label;
        }
        return InlineKeyboardButton.builder()
                .text(label)
                .callbackData(SETTINGS_TZ_REGION_PREFIX + region.key())
                .build();
    }

    private static TimezoneOption tz(String id, String label) {
        return new TimezoneOption(id, label);
    }

    private record TimezoneOption(String id, String label) {}
    private record TimezoneRegion(String key, String labelEn, String labelRu, List<TimezoneOption> timezones) {}
}
