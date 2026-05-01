package com.festiva.command.handler;

import com.festiva.command.CommandHandler;
import com.festiva.command.MessageBuilder;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.state.UserStateService;
import com.festiva.user.api.AccountDeletionAction;
import com.festiva.user.api.UserPreferenceService;
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
@SuppressWarnings("unused")
public class DeleteAccountCommandHandler implements CommandHandler {
    private final UserPreferenceService userPreferenceService;
    private final UserStateService userStateService;

    @Override
    public String command() { return "/deleteaccount"; }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        Lang lang = userPreferenceService.getLanguage(userId);
        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                .keyboard(List.of(new InlineKeyboardRow(
                        InlineKeyboardButton.builder().text(Messages.get(lang, Messages.CONFIRM_YES)).callbackData(AccountDeletionAction.CONFIRM_DELETE).build(),
                        InlineKeyboardButton.builder().text(Messages.get(lang, Messages.CONFIRM_NO)).callbackData(AccountDeletionAction.CANCEL_DELETE).build())))
                .build();
        return MessageBuilder.html(chatId, Messages.get(lang, Messages.DELETE_ACCOUNT_ASK), keyboard);
    }
}
