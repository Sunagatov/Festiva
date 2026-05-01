package com.festiva.command.handler;

import com.festiva.command.CommandHandler;
import com.festiva.command.MessageBuilder;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.state.UserStateService;
import com.festiva.user.api.UserPreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class MoreCommandHandler implements CommandHandler {

    public static final String CALLBACK_SEARCH = "MORE_SEARCH";
    public static final String CALLBACK_EDIT = "MORE_EDIT";
    public static final String CALLBACK_REMOVE = "MORE_REMOVE";
    public static final String CALLBACK_JUBILEE = "MORE_JUBILEE";
    public static final String CALLBACK_BROWSE = "MORE_BROWSE";
    public static final String CALLBACK_STATS = "MORE_STATS";
    public static final String CALLBACK_IMPORT = "MORE_IMPORT";
    public static final String CALLBACK_EXPORT = "MORE_EXPORT";
    public static final String CALLBACK_LANGUAGE = "MORE_LANGUAGE";
    public static final String CALLBACK_SETTINGS = "MORE_SETTINGS";
    public static final String CALLBACK_ABOUT = "MORE_ABOUT";
    public static final String CALLBACK_BACK = "MORE_BACK";

    private static final Map<String, String> CALLBACK_TO_COMMAND = Map.ofEntries(
            Map.entry(CALLBACK_SEARCH, "/search"),
            Map.entry(CALLBACK_EDIT, "/edit"),
            Map.entry(CALLBACK_REMOVE, "/remove"),
            Map.entry(CALLBACK_JUBILEE, "/jubilee"),
            Map.entry(CALLBACK_BROWSE, "/birthdays"),
            Map.entry(CALLBACK_STATS, "/stats"),
            Map.entry(CALLBACK_IMPORT, "/addmany"),
            Map.entry(CALLBACK_EXPORT, "/export"),
            Map.entry(CALLBACK_LANGUAGE, "/language"),
            Map.entry(CALLBACK_SETTINGS, "/settings"),
            Map.entry(CALLBACK_ABOUT, "/about")
    );

    private final UserStateService userStateService;
    private final UserPreferenceService userPreferenceService;

    @Override
    public String command() {
        return "/more";
    }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        Lang lang = userPreferenceService.getLanguage(userId);

        userStateService.clearState(userId);

        return MessageBuilder.html(chatId, Messages.get(lang, Messages.MORE_HEADER), keyboard(lang));
    }

    public static String commandForCallback(String data) {
        return CALLBACK_TO_COMMAND.get(data);
    }

    public static InlineKeyboardMarkup keyboard(Lang lang) {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        new InlineKeyboardRow(
                                button(lang, Messages.MORE_SEARCH_BTN, CALLBACK_SEARCH),
                                button(lang, Messages.MORE_EDIT_BTN, CALLBACK_EDIT),
                                button(lang, Messages.MORE_REMOVE_BTN, CALLBACK_REMOVE)
                        ),
                        new InlineKeyboardRow(
                                button(lang, Messages.MORE_JUBILEE_BTN, CALLBACK_JUBILEE),
                                button(lang, Messages.MORE_BROWSE_BTN, CALLBACK_BROWSE)
                        ),
                        new InlineKeyboardRow(
                                button(lang, Messages.MORE_STATS_BTN, CALLBACK_STATS),
                                button(lang, Messages.MORE_IMPORT_BTN, CALLBACK_IMPORT)
                        ),
                        new InlineKeyboardRow(
                                button(lang, Messages.MORE_EXPORT_BTN, CALLBACK_EXPORT),
                                button(lang, Messages.MORE_LANGUAGE_BTN, CALLBACK_LANGUAGE)
                        ),
                        new InlineKeyboardRow(
                                button(lang, Messages.MORE_SETTINGS_BTN, CALLBACK_SETTINGS),
                                button(lang, Messages.MORE_ABOUT_BTN, CALLBACK_ABOUT)
                        )
                ))
                .build();
    }

    private static InlineKeyboardButton button(Lang lang, String key, String callback) {
        return InlineKeyboardButton.builder()
                .text(Messages.get(lang, key))
                .callbackData(callback)
                .build();
    }
}
