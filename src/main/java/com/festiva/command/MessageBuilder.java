package com.festiva.command;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

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
}
