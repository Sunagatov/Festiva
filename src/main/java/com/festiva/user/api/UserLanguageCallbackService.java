package com.festiva.user.api;

import com.festiva.bot.BotCommandsService;
import com.festiva.bot.CallbackResult;
import com.festiva.command.MessageBuilder;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.state.UserStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.List;

@Service
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class UserLanguageCallbackService {

    private final UserStateService userStateService;
    private final UserPreferenceService userPreferenceService;
    private final BotCommandsService commandsService;

    public CallbackResult handle(String data, long userId) {
        if (!data.startsWith(UserLanguageAction.PREFIX)) {
            return null;
        }
        return handleLanguage(userId, data.substring(UserLanguageAction.PREFIX.length()));
    }

    public InlineKeyboardMarkup keyboard(Lang lang) {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(new InlineKeyboardRow(
                        InlineKeyboardButton.builder().text((lang == Lang.EN ? "✅ " : "") + Messages.get(lang, Messages.LANG_EN_BTN)).callbackData(UserLanguageAction.callback(Lang.EN)).build(),
                        InlineKeyboardButton.builder().text((lang == Lang.RU ? "✅ " : "") + Messages.get(lang, Messages.LANG_RU_BTN)).callbackData(UserLanguageAction.callback(Lang.RU)).build()
                )))
                .build();
    }

    private CallbackResult handleLanguage(long userId, String code) {
        try {
            Lang newLang = Lang.valueOf(code);
            userPreferenceService.setLanguage(userId, newLang);
            userStateService.clearState(userId);
            commandsService.updateCommandsForUser(userId, newLang);
            return new CallbackResult(Messages.get(newLang, Messages.LANGUAGE_SET),
                    MessageBuilder.withBackToMore(newLang, keyboard(newLang)));
        } catch (IllegalArgumentException e) {
            Lang lang = userPreferenceService.getLanguage(userId);
            return new CallbackResult(Messages.get(lang, Messages.SESSION_EXPIRED), null);
        }
    }
}
