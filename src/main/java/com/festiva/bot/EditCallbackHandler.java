package com.festiva.bot;

import com.festiva.command.DatePickerKeyboard;
import com.festiva.command.MessageBuilder;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.state.BotState;
import com.festiva.state.UserStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.List;

@Component
@RequiredArgsConstructor
class EditCallbackHandler {

    static final String EDIT_PREFIX       = "EDIT_";
    static final String EDIT_FIELD_NAME   = "EDIT_FIELD_NAME_";
    static final String EDIT_FIELD_DATE   = "EDIT_FIELD_DATE_";
    static final String EDIT_FIELD_NOTIFY = "EDIT_FIELD_NOTIFY_";
    static final String EDIT_FIELD_REL    = "EDIT_FIELD_REL_";

    private final FriendService friendService;
    private final UserStateService userStateService;

    CallbackResult handleEditNotify(String data, long userId, Lang lang) {
        String name = data.substring(EDIT_FIELD_NOTIFY.length());
        boolean enabled = friendService.toggleFriendNotify(userId, name);
        return new CallbackResult(Messages.get(lang, Messages.EDIT_NOTIFY_TOGGLED, name,
                Messages.get(lang, enabled ? Messages.NOTIFY_STATUS_ON : Messages.NOTIFY_STATUS_OFF)), null);
    }

    CallbackResult handleEditFieldName(String data, long userId, Lang lang) {
        String name = data.substring(EDIT_FIELD_NAME.length());
        userStateService.setPendingName(userId, name);
        userStateService.setState(userId, BotState.WAITING_FOR_EDIT_NAME);
        return new CallbackResult(Messages.get(lang, Messages.EDIT_ENTER_NAME, name), null);
    }

    CallbackResult handleEditFieldDate(String data, long userId, Lang lang) {
        String name = data.substring(EDIT_FIELD_DATE.length());
        userStateService.setPendingName(userId, name);
        userStateService.setYearPageOffset(userId, DatePickerKeyboard.DEFAULT_YEAR_OFFSET);
        userStateService.setState(userId, BotState.WAITING_FOR_EDIT_DATE);
        return new CallbackResult(Messages.get(lang, Messages.DATE_PICK_YEAR, name),
                DatePickerKeyboard.yearKeyboard(DatePickerKeyboard.DEFAULT_YEAR_OFFSET, lang));
    }

    CallbackResult handleEditSelect(String data, long userId, Lang lang) {
        String id = data.substring(EDIT_PREFIX.length());
        Friend found = friendService.findFriendById(id).orElse(null);
        String name = found != null ? found.getName() : "?";
        String currentDate = found != null ? found.getBirthDate().format(MessageBuilder.DATE_FORMATTER) : "?";
        boolean notifyOn = found == null || found.isNotifyEnabled();
        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder().keyboard(List.of(
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder().text(Messages.get(lang, Messages.EDIT_FIELD_NAME_BTN)).callbackData(EDIT_FIELD_NAME + name).build(),
                        InlineKeyboardButton.builder().text(Messages.get(lang, Messages.EDIT_FIELD_DATE_BTN)).callbackData(EDIT_FIELD_DATE + name).build()),
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder().text(Messages.get(lang, Messages.EDIT_FIELD_REL_BTN)).callbackData(EDIT_FIELD_REL + name).build(),
                        InlineKeyboardButton.builder().text(notifyOn ? Messages.get(lang, Messages.EDIT_NOTIFS_ON) : Messages.get(lang, Messages.EDIT_NOTIFS_OFF)).callbackData(EDIT_FIELD_NOTIFY + name).build())
        )).build();
        return new CallbackResult(Messages.get(lang, Messages.EDIT_CHOOSE_FIELD, name, currentDate), markup);
    }
}
