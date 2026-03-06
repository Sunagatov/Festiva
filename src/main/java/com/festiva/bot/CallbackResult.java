package com.festiva.bot;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

final class CallbackResult {
    final String text;
    final InlineKeyboardMarkup markup;
    final SendMessage sendMessage;

    CallbackResult(String text, InlineKeyboardMarkup markup) {
        this.text = text; this.markup = markup; this.sendMessage = null;
    }

    CallbackResult(SendMessage sendMessage) {
        this.text = null; this.markup = null; this.sendMessage = sendMessage;
    }
}
