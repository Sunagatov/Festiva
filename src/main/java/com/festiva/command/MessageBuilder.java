package com.festiva.command;

import com.festiva.friend.api.FriendAction;
import com.festiva.command.handler.MoreCommandHandler;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
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

    public static InlineKeyboardMarkup emptyStateAddMarkup(Lang lang) {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text(Messages.get(lang, Messages.REMOVE_EMPTY_ADD))
                                .callbackData(FriendAction.ACTION_ADD)
                                .build())))
                .build();
    }

    public static InlineKeyboardMarkup listButtonMarkup(Lang lang) {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text(Messages.get(lang, Messages.QUICK_LIST))
                                .callbackData("LIST_SORT_DATE_0")
                                .build())))
                .build();
    }

    public static InlineKeyboardMarkup addAndListMarkup(Lang lang) {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text(Messages.get(lang, Messages.QUICK_LIST))
                                .callbackData("LIST_SORT_DATE_0")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text(Messages.get(lang, Messages.QUICK_ADD_ANOTHER))
                                .callbackData(FriendAction.ACTION_ADD)
                                .build())))
                .build();
    }

    public static InlineKeyboardMarkup editAndListMarkup(Lang lang) {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text(Messages.get(lang, Messages.QUICK_EDIT))
                                .callbackData(MoreCommandHandler.CALLBACK_EDIT)
                                .build(),
                        InlineKeyboardButton.builder()
                                .text(Messages.get(lang, Messages.QUICK_LIST))
                                .callbackData("LIST_SORT_DATE_0")
                                .build())))
                .build();
    }

    public static InlineKeyboardMarkup settingsButtonMarkup(Lang lang) {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text(Messages.get(lang, Messages.MORE_SETTINGS_BTN))
                                .callbackData(MoreCommandHandler.CALLBACK_SETTINGS)
                                .build())))
                .build();
    }

    // EN labels → command
    private static final Map<String, String> LABEL_TO_COMMAND_EN = Map.ofEntries(
            Map.entry("➕ Add", "/add"),
            Map.entry("🎂 Today", "/today"),
            Map.entry("👥 Friends", "/list"),
            Map.entry("⚙️ More", "/more"),
            Map.entry("\uD83D\uDDD1 Remove", "/remove"),
            Map.entry("\uD83D\uDCCB List", "/list"),
            Map.entry("\uD83C\uDF82 Birthdays", "/birthdays"),
            Map.entry("\uD83D\uDD14 Upcoming", "/upcomingbirthdays"),
            Map.entry("\uD83C\uDF89 Today", "/today"),
            Map.entry("\uD83C\uDFC6 Jubilee", "/jubilee"),
            Map.entry("\uD83D\uDCDD Edit", "/edit"),
            Map.entry("\uD83C\uDF10 Language", "/language"),
            Map.entry("\uD83D\uDCD6 Menu", "/menu"),
            Map.entry("ℹ️ About", "/about"),
            Map.entry("\uD83D\uDD0D Search", "/search"),
            Map.entry("\uD83D\uDCCA Stats", "/stats"),
            Map.entry("\uD83D\uDD27 Settings", "/settings")
    );

    // RU labels → command
    private static final Map<String, String> LABEL_TO_COMMAND_RU = Map.ofEntries(
            Map.entry("➕ Добавить", "/add"),
            Map.entry("🎂 Сегодня", "/today"),
            Map.entry("👥 Друзья", "/list"),
            Map.entry("⚙️ Ещё", "/more"),
            Map.entry("\uD83D\uDDD1 Удалить", "/remove"),
            Map.entry("\uD83D\uDCCB Список", "/list"),
            Map.entry("\uD83C\uDF82 Дни рождения", "/birthdays"),
            Map.entry("\uD83D\uDD14 Ближайшие", "/upcomingbirthdays"),
            Map.entry("\uD83C\uDF89 Сегодня", "/today"),
            Map.entry("\uD83C\uDFC6 Юбилеи", "/jubilee"),
            Map.entry("\uD83D\uDCDD Изменить", "/edit"),
            Map.entry("\uD83C\uDF10 Язык", "/language"),
            Map.entry("\uD83D\uDCD6 Меню", "/menu"),
            Map.entry("ℹ️ О боте", "/about"),
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
                        new KeyboardRow(List.of(
                                new KeyboardButton(ru ? "➕ Добавить" : "➕ Add"),
                                new KeyboardButton(ru ? "🎂 Сегодня" : "🎂 Today"),
                                new KeyboardButton(ru ? "🔔 Ближайшие" : "🔔 Upcoming"))),
                        new KeyboardRow(List.of(
                                new KeyboardButton(ru ? "👥 Друзья" : "👥 Friends"),
                                new KeyboardButton(ru ? "⚙️ Ещё" : "⚙️ More")))
                ))
                .resizeKeyboard(true)
                .isPersistent(true)
                .build();
    }
}
