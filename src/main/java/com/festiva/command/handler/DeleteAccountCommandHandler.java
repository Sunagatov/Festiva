package com.festiva.command.handler;

import com.festiva.command.CommandHandler;
import com.festiva.command.MessageBuilder;
import com.festiva.friend.repository.FriendMongoRepository;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.state.UserStateService;
import com.festiva.user.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeleteAccountCommandHandler implements CommandHandler {

    public static final String CONFIRM_DELETE  = "CONFIRM_DELETE_ACCOUNT";
    public static final String CANCEL_DELETE   = "CANCEL_DELETE_ACCOUNT";

    private final FriendMongoRepository friendRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final UserStateService userStateService;

    @Override
    public String command() { return "/deleteaccount"; }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        Lang lang = userStateService.getLanguage(userId);
        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                .keyboard(List.of(new InlineKeyboardRow(
                        InlineKeyboardButton.builder().text(Messages.get(lang, Messages.CONFIRM_YES)).callbackData(CONFIRM_DELETE).build(),
                        InlineKeyboardButton.builder().text(Messages.get(lang, Messages.CONFIRM_NO)).callbackData(CANCEL_DELETE).build())))
                .build();
        return MessageBuilder.html(chatId, Messages.get(lang, Messages.DELETE_ACCOUNT_ASK), keyboard);
    }

    public void deleteAccount(long userId) {
        friendRepository.deleteByTelegramUserId(userId);
        userPreferenceRepository.deleteById(userId);
        userStateService.clearState(userId);
        log.info("account.deleted: userId={}", userId);
    }
}
