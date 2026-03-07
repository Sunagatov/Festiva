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

    public static final Map<String, String> LABEL_TO_COMMAND = Map.ofEntries(
            Map.entry("➕ Add", "/add"),
            Map.entry("➕ Add Many", "/addmany"),
            Map.entry("\uD83D\uDDD1 Remove", "/remove"),
            Map.entry("\uD83D\uDCCB List", "/list"),
            Map.entry("\uD83C\uDF82 Birthdays", "/birthdays"),
            Map.entry("\uD83D\uDD14 Upcoming", "/upcomingbirthdays"),
            Map.entry("\uD83C\uDF82 Today", "/today"),
            Map.entry("\uD83C\uDFC6 Jubilee", "/jubilee"),
            Map.entry("\uD83D\uDCDD Edit", "/edit"),
            Map.entry("\uD83C\uDF10 Language", "/language"),
            Map.entry("\uD83D\uDCD6 Help", "/help"),
            Map.entry("\uD83D\uDD0D Search", "/search"),
            Map.entry("\uD83D\uDCCA Stats", "/stats"),
            Map.entry("\uD83D\uDD27 Settings", "/settings")
    );

    public static ReplyKeyboardMarkup mainMenu() {
        return ReplyKeyboardMarkup.builder()
                .keyboard(List.of(
                        new KeyboardRow(List.of(new KeyboardButton("➕ Add"), new KeyboardButton("\uD83D\uDDD1 Remove"), new KeyboardButton("\uD83D\uDCCB List"))),
                        new KeyboardRow(List.of(new KeyboardButton("\uD83C\uDF82 Birthdays"), new KeyboardButton("\uD83D\uDD14 Upcoming"), new KeyboardButton("\uD83C\uDF82 Today"))),
                        new KeyboardRow(List.of(new KeyboardButton("\uD83C\uDFC6 Jubilee"), new KeyboardButton("\uD83D\uDCDD Edit"), new KeyboardButton("\uD83D\uDD0D Search"))),
                        new KeyboardRow(List.of(new KeyboardButton("\uD83D\uDCCA Stats"), new KeyboardButton("\uD83D\uDD27 Settings"), new KeyboardButton("\uD83C\uDF10 Language"))),
                        new KeyboardRow(List.of(new KeyboardButton("\uD83D\uDCD6 Help")))
                ))
                .resizeKeyboard(true)
                .build();
    }
}
