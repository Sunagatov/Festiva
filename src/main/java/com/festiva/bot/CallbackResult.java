package com.festiva.bot;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

public final class CallbackResult {
    public final String text;
    public final InlineKeyboardMarkup markup;
    public final SendMessage sendMessage;

    public CallbackResult(String text, InlineKeyboardMarkup markup) {
        this.text = text; this.markup = markup; this.sendMessage = null;
    }

    public CallbackResult(SendMessage sendMessage) {
        this.text = null; this.markup = null; this.sendMessage = sendMessage;
    }
}
