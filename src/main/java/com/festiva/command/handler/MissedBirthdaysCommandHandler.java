package com.festiva.command.handler;

import com.festiva.command.CommandHandler;
import com.festiva.command.MessageBuilder;
import com.festiva.i18n.Lang;
import com.festiva.user.api.UserPreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@RequiredArgsConstructor
public class MissedBirthdaysCommandHandler implements CommandHandler {

    private final BirthdayPeriodListService birthdayPeriodListService;
    private final UserPreferenceService userPreferenceService;

    @Override
    public String command() {
        return "/missed";
    }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        Lang lang = userPreferenceService.getLanguage(userId);
        BirthdayPeriodListService.View view = birthdayPeriodListService.view(
                userId, lang, BirthdayPeriodListService.Period.MISSED, true, 0);
        return MessageBuilder.html(chatId, view.text(), view.markup());
    }
}
