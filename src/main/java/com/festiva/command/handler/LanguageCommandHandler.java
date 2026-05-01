package com.festiva.command.handler;

import com.festiva.command.CommandHandler;
import com.festiva.command.MessageBuilder;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.state.UserStateService;
import com.festiva.user.api.UserLanguageCallbackService;
import com.festiva.user.api.UserPreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class LanguageCommandHandler implements CommandHandler {

    private final UserStateService userStateService;
    private final UserPreferenceService userPreferenceService;
    private final UserLanguageCallbackService userLanguageCallbackService;

    @Override
    public String command() {
        return "/language";
    }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();

        userStateService.clearState(userId);

        Lang lang = userPreferenceService.getLanguage(userId);
        return MessageBuilder.html(chatId, Messages.get(lang, Messages.LANGUAGE_CHOOSE), userLanguageCallbackService.keyboard(lang));
    }
}
