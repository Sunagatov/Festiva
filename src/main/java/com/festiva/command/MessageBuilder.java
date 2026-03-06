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

    public static final Map<String, String> LABEL_TO_COMMAND = Map.of(
            "➕ Add", "/add",
            "🗑 Remove", "/remove",
            "📋 List", "/list",
            "🎂 Birthdays", "/birthdays",
            "⏰ Upcoming", "/upcomingbirthdays",
            "🏆 Jubilee", "/jubilee",
            "✏️ Edit", "/edit",
            "🌐 Language", "/language",
            "❓ Help", "/help"
    );

    public static ReplyKeyboardMarkup mainMenu() {
        return ReplyKeyboardMarkup.builder()
                .keyboard(List.of(
                        new KeyboardRow(List.of(new KeyboardButton("➕ Add"), new KeyboardButton("🗑 Remove"), new KeyboardButton("📋 List"))),
                        new KeyboardRow(List.of(new KeyboardButton("🎂 Birthdays"), new KeyboardButton("⏰ Upcoming"), new KeyboardButton("🏆 Jubilee"))),
                        new KeyboardRow(List.of(new KeyboardButton("✏️ Edit"), new KeyboardButton("🌐 Language"), new KeyboardButton("❓ Help")))
                ))
                .resizeKeyboard(true)
                .build();
    }
}
