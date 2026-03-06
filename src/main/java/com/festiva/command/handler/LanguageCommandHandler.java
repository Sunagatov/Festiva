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

import java.util.List;

@Component
@RequiredArgsConstructor
public class LanguageCommandHandler implements CommandHandler {

    private final UserStateService userStateService;

    @Override
    public String command() {
        return "/language";
    }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        Lang lang = userStateService.getLanguage(userId);

        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                .keyboard(List.of(new InlineKeyboardRow(
                        InlineKeyboardButton.builder().text(lang == Lang.EN ? "✅ 🇬🇧 English" : "🇬🇧 English").callbackData("LANG_" + Lang.EN.name()).build(),
                        InlineKeyboardButton.builder().text(lang == Lang.RU ? "✅ 🇷🇺 Русский" : "🇷🇺 Русский").callbackData("LANG_" + Lang.RU.name()).build()
                )))
                .build();

        return MessageBuilder.html(chatId, Messages.get(lang, Messages.LANGUAGE_CHOOSE), keyboard);
    }
}
