package com.festiva.command;

import com.festiva.i18n.Lang;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;

public final class DatePickerKeyboard {

    public static final String DATE_YEAR_PREFIX      = "DATE_YEAR_";
    public static final String DATE_MONTH_PREFIX     = "DATE_MONTH_";
    public static final String DATE_DAY_PREFIX       = "DATE_DAY_";
    public static final String DATE_YEAR_PAGE_PREFIX = "DATE_YEAR_PAGE_";

    private static final int YEARS_PER_PAGE = 8;

    private DatePickerKeyboard() {}

    public static InlineKeyboardMarkup yearKeyboard(int pageOffset) {
        int currentYear = java.time.LocalDate.now().getYear();
        int startYear = currentYear - pageOffset;
        List<InlineKeyboardRow> rows = new ArrayList<>();

        for (int i = 0; i < YEARS_PER_PAGE; i += 4) {
            InlineKeyboardRow row = new InlineKeyboardRow();
            for (int j = 0; j < 4 && i + j < YEARS_PER_PAGE; j++) {
                int year = startYear - i - j;
                row.add(btn(String.valueOf(year), DATE_YEAR_PREFIX + year));
            }
            rows.add(row);
        }

        InlineKeyboardRow nav = new InlineKeyboardRow();
        nav.add(btn("◀ Earlier", DATE_YEAR_PAGE_PREFIX + (pageOffset + YEARS_PER_PAGE)));
        if (pageOffset >= YEARS_PER_PAGE) {
            nav.add(btn("Later ▶", DATE_YEAR_PAGE_PREFIX + (pageOffset - YEARS_PER_PAGE)));
        }
        rows.add(nav);

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public static final String DATE_BACK_TO_YEAR  = "DATE_BACK_YEAR";
    public static final String DATE_BACK_TO_MONTH = "DATE_BACK_MONTH";

    public static InlineKeyboardMarkup monthKeyboard(Lang lang, int yearPageOffset) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (int m = 1; m <= 12; m += 4) {
            InlineKeyboardRow row = new InlineKeyboardRow();
            for (int col = 0; col < 4 && m + col <= 12; col++) {
                int month = m + col;
                String raw = Month.of(month).getDisplayName(TextStyle.SHORT, lang.locale());
                String label = Character.toUpperCase(raw.charAt(0)) + raw.substring(1).replace(".", "");
                row.add(btn(label, DATE_MONTH_PREFIX + month));
            }
            rows.add(row);
        }
        rows.add(new InlineKeyboardRow(btn(
                com.festiva.i18n.Messages.get(lang, com.festiva.i18n.Messages.DATE_PICK_BACK),
                DATE_BACK_TO_YEAR + "_" + yearPageOffset)));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public static InlineKeyboardMarkup dayKeyboard(int year, int month, Lang lang) {
        int daysInMonth = YearMonth.of(year, month).lengthOfMonth();
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (int d = 1; d <= daysInMonth; d += 7) {
            InlineKeyboardRow row = new InlineKeyboardRow();
            for (int col = 0; col < 7 && d + col <= daysInMonth; col++) {
                int day = d + col;
                row.add(btn(String.valueOf(day), DATE_DAY_PREFIX + day));
            }
            rows.add(row);
        }
        rows.add(new InlineKeyboardRow(btn(
                com.festiva.i18n.Messages.get(lang, com.festiva.i18n.Messages.DATE_PICK_BACK),
                DATE_BACK_TO_MONTH)));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private static InlineKeyboardButton btn(String text, String data) {
        return InlineKeyboardButton.builder().text(text).callbackData(data).build();
    }
}
