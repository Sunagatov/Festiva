package com.festiva.command;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MessageBuilder {

    public static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.ROOT);

    private MessageBuilder() {}

    public static SendMessage html(long chatId, String text) {
        return SendMessage.builder().chatId(chatId).parseMode("HTML").text(text).build();
    }

    public static SendMessage html(long chatId, String text, InlineKeyboardMarkup markup) {
        return SendMessage.builder().chatId(chatId).parseMode("HTML").text(text).replyMarkup(markup).build();
    }

    public static SendMessage html(long chatId, String text, ReplyKeyboardMarkup markup) {
        return SendMessage.builder().chatId(chatId).parseMode("HTML").text(text).replyMarkup(markup).build();
    }

    // EN labels → command
    private static final Map<String, String> LABEL_TO_COMMAND_EN = Map.ofEntries(
            Map.entry("\u2795 Add", "/add"),
            Map.entry("\uD83D\uDDD1 Remove", "/remove"),
            Map.entry("\uD83D\uDCCB List", "/list"),
            Map.entry("\uD83C\uDF82 Birthdays", "/birthdays"),
            Map.entry("\uD83D\uDD14 Upcoming", "/upcomingbirthdays"),
            Map.entry("\uD83C\uDF82 Today", "/today"),
            Map.entry("\uD83C\uDFC6 Jubilee", "/jubilee"),
            Map.entry("\uD83D\uDCDD Edit", "/edit"),
            Map.entry("\uD83C\uDF10 Language", "/language"),
            Map.entry("\uD83D\uDCD6 Menu", "/menu"),
            Map.entry("\u2139\uFE0F About", "/about"),
            Map.entry("\uD83D\uDD0D Search", "/search"),
            Map.entry("\uD83D\uDCCA Stats", "/stats"),
            Map.entry("\uD83D\uDD27 Settings", "/settings")
    );

    // RU labels → command
    private static final Map<String, String> LABEL_TO_COMMAND_RU = Map.ofEntries(
            Map.entry("\u2795 Добавить", "/add"),
            Map.entry("\uD83D\uDDD1 Удалить", "/remove"),
            Map.entry("\uD83D\uDCCB Список", "/list"),
            Map.entry("\uD83C\uDF82 Дни рождения", "/birthdays"),
            Map.entry("\uD83D\uDD14 Ближайшие", "/upcomingbirthdays"),
            Map.entry("\uD83C\uDF82 Сегодня", "/today"),
            Map.entry("\uD83C\uDFC6 Юбилеи", "/jubilee"),
            Map.entry("\uD83D\uDCDD Изменить", "/edit"),
            Map.entry("\uD83C\uDF10 Язык", "/language"),
            Map.entry("\uD83D\uDCD6 Меню", "/menu"),
            Map.entry("\u2139\uFE0F О боте", "/about"),
            Map.entry("\uD83D\uDD0D Поиск", "/search"),
            Map.entry("\uD83D\uDCCA Статистика", "/stats"),
            Map.entry("\uD83D\uDD27 Настройки", "/settings")
    );

    public static final Map<String, String> LABEL_TO_COMMAND;
    static {
        var combined = new java.util.HashMap<String, String>();
        combined.putAll(LABEL_TO_COMMAND_EN);
        combined.putAll(LABEL_TO_COMMAND_RU);
        LABEL_TO_COMMAND = java.util.Collections.unmodifiableMap(combined);
    }

    public static ReplyKeyboardMarkup mainMenu(com.festiva.i18n.Lang lang) {
        boolean ru = lang == com.festiva.i18n.Lang.RU;
        return ReplyKeyboardMarkup.builder()
                .keyboard(List.of(
                        new KeyboardRow(List.of(new KeyboardButton(ru ? "\u2795 Добавить" : "\u2795 Add"), new KeyboardButton(ru ? "\uD83D\uDDD1 Удалить" : "\uD83D\uDDD1 Remove"), new KeyboardButton(ru ? "\uD83D\uDCCB Список" : "\uD83D\uDCCB List"))),
                        new KeyboardRow(List.of(new KeyboardButton(ru ? "\uD83C\uDF82 Дни рождения" : "\uD83C\uDF82 Birthdays"), new KeyboardButton(ru ? "\uD83D\uDD14 Ближайшие" : "\uD83D\uDD14 Upcoming"), new KeyboardButton(ru ? "\uD83C\uDF82 Сегодня" : "\uD83C\uDF82 Today"))),
                        new KeyboardRow(List.of(new KeyboardButton(ru ? "\uD83C\uDFC6 Юбилеи" : "\uD83C\uDFC6 Jubilee"), new KeyboardButton(ru ? "\uD83D\uDCDD Изменить" : "\uD83D\uDCDD Edit"), new KeyboardButton(ru ? "\uD83D\uDD0D Поиск" : "\uD83D\uDD0D Search"))),
                        new KeyboardRow(List.of(new KeyboardButton(ru ? "\uD83D\uDCCA Статистика" : "\uD83D\uDCCA Stats"), new KeyboardButton(ru ? "\uD83D\uDD27 Настройки" : "\uD83D\uDD27 Settings"), new KeyboardButton(ru ? "\uD83C\uDF10 Язык" : "\uD83C\uDF10 Language"))),
                        new KeyboardRow(List.of(new KeyboardButton(ru ? "\uD83D\uDCD6 Меню" : "\uD83D\uDCD6 Menu"), new KeyboardButton(ru ? "\u2139\uFE0F О боте" : "\u2139\uFE0F About")))
                ))
                .resizeKeyboard(true)
                .isPersistent(true)
                .build();
    }
}
