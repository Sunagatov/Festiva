package com.festiva.friend.handler;

import com.festiva.bot.CallbackResult;
import com.festiva.command.MessageBuilder;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.friend.workflow.FriendWorkflowSessionService;
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
@SuppressWarnings("unused")
public class EditCallbackHandler {

    public static final String EDIT_PREFIX = "EDIT_";
    public static final String EDIT_FIELD_NAME = "EDIT_FIELD_NAME_";
    public static final String EDIT_FIELD_DATE = "EDIT_FIELD_DATE_";
    public static final String EDIT_FIELD_NOTIFY = "EDIT_FIELD_NOTIFY_";
    public static final String EDIT_FIELD_REL = "EDIT_FIELD_REL_";

    private final FriendService friendService;
    private final UserStateService userStateService;
    private final FriendWorkflowSessionService friendWorkflowSessionService;

    public CallbackResult handleEditNotify(String data, long userId, Lang lang) {
        String id = data.substring(EDIT_FIELD_NOTIFY.length());
        Friend friend = friendService.findOwnedFriend(id, userId).orElse(null);
        if (friend == null)
            return new CallbackResult(Messages.get(lang, Messages.SESSION_EXPIRED), null);
        boolean enabled = friendService.toggleFriendNotifyById(id, userId);
        return new CallbackResult(Messages.get(lang, Messages.EDIT_NOTIFY_TOGGLED, friend.getName(),
                Messages.get(lang, enabled ? Messages.NOTIFY_STATUS_ON : Messages.NOTIFY_STATUS_OFF)),
                MessageBuilder.editAndListMarkup(lang));
    }

    public CallbackResult handleEditFieldName(String data, long userId, Lang lang) {
        String id = data.substring(EDIT_FIELD_NAME.length());
        Friend friend = friendService.findOwnedFriend(id, userId).orElse(null);
        if (friend == null)
            return new CallbackResult(Messages.get(lang, Messages.SESSION_EXPIRED), null);
        friendWorkflowSessionService.setPendingName(userId, friend.getName());
        friendWorkflowSessionService.setPendingId(userId, id);
        userStateService.setState(userId, BotState.WAITING_FOR_EDIT_NAME);
        return new CallbackResult(Messages.get(lang, Messages.EDIT_ENTER_NAME, friend.getName()), null);
    }

    public CallbackResult handleEditFieldDate(String data, long userId, Lang lang) {
        String id = data.substring(EDIT_FIELD_DATE.length());
        Friend friend = friendService.findOwnedFriend(id, userId).orElse(null);
        if (friend == null)
            return new CallbackResult(Messages.get(lang, Messages.SESSION_EXPIRED), null);
        friendWorkflowSessionService.setPendingName(userId, friend.getName());
        friendWorkflowSessionService.setPendingId(userId, id);
        friendWorkflowSessionService.setYearPageOffset(userId, DatePickerKeyboard.DEFAULT_YEAR_OFFSET);
        userStateService.setState(userId, BotState.WAITING_FOR_EDIT_DATE);
        return new CallbackResult(Messages.get(lang, Messages.DATE_PICK_YEAR, friend.getName()),
                DatePickerKeyboard.yearKeyboard(DatePickerKeyboard.DEFAULT_YEAR_OFFSET, lang));
    }

    public CallbackResult handleEditSelect(String data, long userId, Lang lang) {
        String id = data.substring(EDIT_PREFIX.length());
        Friend found = friendService.findOwnedFriend(id, userId).orElse(null);
        if (found == null) return new CallbackResult(Messages.get(lang, Messages.SESSION_EXPIRED), null);
        String name = found.getName();
        String currentDate = found.hasYear()
                ? found.getBirthDate().format(MessageBuilder.DATE_FORMATTER)
                : String.format("%02d.%02d", found.getBirthMonthDay().getDayOfMonth(), found.getBirthMonthDay().getMonthValue());
        boolean notifyOn = found.isNotifyEnabled();
        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder().keyboard(List.of(
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder().text(Messages.get(lang, Messages.EDIT_FIELD_NAME_BTN)).callbackData(EDIT_FIELD_NAME + id).build(),
                        InlineKeyboardButton.builder().text(Messages.get(lang, Messages.EDIT_FIELD_DATE_BTN)).callbackData(EDIT_FIELD_DATE + id).build()),
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder().text(Messages.get(lang, Messages.EDIT_FIELD_REL_BTN)).callbackData(EDIT_FIELD_REL + id).build(),
                        InlineKeyboardButton.builder().text(notifyOn ? Messages.get(lang, Messages.EDIT_NOTIFS_ON) : Messages.get(lang, Messages.EDIT_NOTIFS_OFF)).callbackData(EDIT_FIELD_NOTIFY + id).build())
        )).build();
        return new CallbackResult(Messages.get(lang, Messages.EDIT_CHOOSE_FIELD, name, currentDate), markup);
    }

}
