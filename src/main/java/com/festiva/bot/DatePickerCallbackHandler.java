package com.festiva.bot;

import com.festiva.command.DatePickerKeyboard;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.friend.entity.Relationship;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.state.BotState;
import com.festiva.state.UserStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
class DatePickerCallbackHandler {

    static final String RELATIONSHIP_PREFIX = "RELATIONSHIP_";
    static final String EDIT_REL_PREFIX     = "EDIT_REL_";
    private static final String LIST_SORT_DATE = "LIST_SORT_DATE";

    private final FriendService friendService;
    private final UserStateService userStateService;

    CallbackResult handleYearPage(String data, long userId, Lang lang) {
        int offset = Integer.parseInt(data.substring(DatePickerKeyboard.DATE_YEAR_PAGE_PREFIX.length()));
        String name = userStateService.getPendingName(userId);
        if (name == null) return new CallbackResult(Messages.get(lang, Messages.SESSION_EXPIRED), null);
        userStateService.setYearPageOffset(userId, offset);
        return new CallbackResult(Messages.get(lang, Messages.DATE_PICK_YEAR, name),
                DatePickerKeyboard.yearKeyboard(offset, lang));
    }

    CallbackResult handleYearPick(String data, long userId, Lang lang) {
        String name = userStateService.getPendingName(userId);
        if (name == null) return new CallbackResult(Messages.get(lang, Messages.SESSION_EXPIRED), null);
        int year = Integer.parseInt(data.substring(DatePickerKeyboard.DATE_YEAR_PREFIX.length()));
        userStateService.setPendingYear(userId, year);
        return new CallbackResult(Messages.get(lang, Messages.DATE_PICK_MONTH, name),
                DatePickerKeyboard.monthKeyboard(lang, userStateService.getYearPageOffset(userId)));
    }

    CallbackResult handleMonthPick(String data, long userId, Lang lang) {
        int month = Integer.parseInt(data.substring(DatePickerKeyboard.DATE_MONTH_PREFIX.length()));
        Integer year = userStateService.getPendingYear(userId);
        if (year == null) return new CallbackResult(Messages.get(lang, Messages.SESSION_EXPIRED), null);
        String name = userStateService.getPendingName(userId);
        if (name == null) return new CallbackResult(Messages.get(lang, Messages.SESSION_EXPIRED), null);
        userStateService.setPendingMonth(userId, month);
        return new CallbackResult(Messages.get(lang, Messages.DATE_PICK_DAY, name),
                DatePickerKeyboard.dayKeyboard(year, month, lang));
    }

    CallbackResult handleDayPick(String data, long userId, Lang lang) {
        int day = Integer.parseInt(data.substring(DatePickerKeyboard.DATE_DAY_PREFIX.length()));
        Integer year = userStateService.getPendingYear(userId);
        Integer month = userStateService.getPendingMonth(userId);
        String name = userStateService.getPendingName(userId);
        if (year == null || month == null || name == null)
            return new CallbackResult(Messages.get(lang, Messages.SESSION_EXPIRED), null);
        LocalDate birthDate = LocalDate.of(year, month, day);
        if (birthDate.isAfter(LocalDate.now()))
            return new CallbackResult(Messages.get(lang, Messages.DATE_FUTURE_ERROR),
                    DatePickerKeyboard.dayKeyboard(year, month, lang));
        if (userStateService.getState(userId) == BotState.WAITING_FOR_EDIT_DATE) {
            friendService.updateFriendDate(userId, name, birthDate);
            userStateService.clearState(userId);
            log.debug("friend.date.updated: userId={}, name={}", userId, name);
            return new CallbackResult(Messages.get(lang, Messages.EDIT_DATE_DONE, name), null);
        }
        userStateService.setPendingYear(userId, year);
        userStateService.setPendingMonth(userId, month);
        userStateService.setPendingDay(userId, day);
        userStateService.setState(userId, BotState.WAITING_FOR_ADD_FRIEND_RELATIONSHIP);
        return new CallbackResult(Messages.get(lang, Messages.RELATIONSHIP_PICK, name), relationshipKeyboard(lang));
    }

    CallbackResult handleBackToYear(String data, long userId, Lang lang) {
        String name = userStateService.getPendingName(userId);
        if (name == null) return new CallbackResult(Messages.get(lang, Messages.SESSION_EXPIRED), null);
        int offset = Integer.parseInt(data.substring(DatePickerKeyboard.DATE_BACK_TO_YEAR.length() + 1));
        userStateService.setYearPageOffset(userId, offset);
        userStateService.setPendingYear(userId, null);
        return new CallbackResult(Messages.get(lang, Messages.DATE_PICK_YEAR, name),
                DatePickerKeyboard.yearKeyboard(offset, lang));
    }

    CallbackResult handleBackToMonth(long userId, Lang lang) {
        String name = userStateService.getPendingName(userId);
        if (name == null) return new CallbackResult(Messages.get(lang, Messages.SESSION_EXPIRED), null);
        userStateService.setPendingMonth(userId, null);
        return new CallbackResult(Messages.get(lang, Messages.DATE_PICK_MONTH, name),
                DatePickerKeyboard.monthKeyboard(lang, userStateService.getYearPageOffset(userId)));
    }

    CallbackResult handleRelationship(String data, long userId, Lang lang) {
        String name = userStateService.getPendingName(userId);
        Integer year = userStateService.getPendingYear(userId);
        Integer month = userStateService.getPendingMonth(userId);
        Integer day = userStateService.getPendingDay(userId);
        if (year == null || month == null || day == null || name == null)
            return new CallbackResult(Messages.get(lang, Messages.SESSION_EXPIRED), null);
        LocalDate birthDate = LocalDate.of(year, month, day);
        Relationship rel = "SKIP".equals(data.substring(RELATIONSHIP_PREFIX.length())) ? null
                : Relationship.valueOf(data.substring(RELATIONSHIP_PREFIX.length()));
        if (friendService.getFriends(userId).size() >= FriendService.FRIEND_CAP) {
            userStateService.clearState(userId);
            return new CallbackResult(Messages.get(lang, Messages.FRIEND_CAP, FriendService.FRIEND_CAP), null);
        }
        friendService.addFriend(userId, new Friend(name, birthDate, rel));
        userStateService.clearState(userId);
        log.debug("friend.added: userId={}, name={}, relationship={}", userId, name, rel);
        return new CallbackResult(Messages.get(lang, Messages.FRIEND_ADDED, name),
                InlineKeyboardMarkup.builder().keyboard(List.of(new InlineKeyboardRow(
                        InlineKeyboardButton.builder().text(Messages.get(lang, Messages.QUICK_LIST)).callbackData(LIST_SORT_DATE + "_0").build(),
                        InlineKeyboardButton.builder().text(Messages.get(lang, Messages.QUICK_ADD_ANOTHER)).callbackData(CallbackQueryHandler.ACTION_ADD).build()
                ))).build());
    }

    CallbackResult handleEditFieldRel(String data, long userId, Lang lang) {
        String id = data.substring(EditCallbackHandler.EDIT_FIELD_REL.length());
        Friend friend = friendService.findFriendById(id).orElse(null);
        if (friend == null) return new CallbackResult(Messages.get(lang, Messages.SESSION_EXPIRED), null);
        userStateService.setPendingName(userId, friend.getName());
        userStateService.setPendingId(userId, id);
        userStateService.setState(userId, BotState.WAITING_FOR_EDIT_RELATIONSHIP);
        return new CallbackResult(Messages.get(lang, Messages.RELATIONSHIP_PICK, friend.getName()), editRelKeyboard(lang));
    }

    CallbackResult handleEditRelationship(String data, long userId, Lang lang) {
        String id = userStateService.getPendingId(userId);
        String name = userStateService.getPendingName(userId);
        if (name == null) return new CallbackResult(Messages.get(lang, Messages.SESSION_EXPIRED), null);
        String value = data.substring(EDIT_REL_PREFIX.length());
        Relationship rel = "SKIP".equals(value) ? null : Relationship.valueOf(value);
        if (id != null) {
            Friend friend = friendService.findFriendById(id).orElse(null);
            if (friend == null) return new CallbackResult(Messages.get(lang, Messages.SESSION_EXPIRED), null);
            friendService.updateFriendRelationship(userId, friend.getName(), rel);
            userStateService.clearState(userId);
            log.debug("friend.relationship.updated: userId={}, id={}, rel={}", userId, id, rel);
            return new CallbackResult(Messages.get(lang, Messages.EDIT_REL_DONE, friend.getName()), null);
        }
        friendService.updateFriendRelationship(userId, name, rel);
        userStateService.clearState(userId);
        log.debug("friend.relationship.updated: userId={}, name={}, rel={}", userId, name, rel);
        return new CallbackResult(Messages.get(lang, Messages.EDIT_REL_DONE, name), null);
    }

    private InlineKeyboardMarkup relationshipKeyboard(Lang lang) {
        return relKeyboard(lang, RELATIONSHIP_PREFIX);
    }

    private InlineKeyboardMarkup editRelKeyboard(Lang lang) {
        return relKeyboard(lang, EDIT_REL_PREFIX);
    }

    private InlineKeyboardMarkup relKeyboard(Lang lang, String prefix) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        InlineKeyboardRow row = new InlineKeyboardRow();
        for (Relationship r : Relationship.values()) {
            row.add(InlineKeyboardButton.builder().text(r.label(lang)).callbackData(prefix + r.name()).build());
            if (row.size() == 3) { rows.add(row); row = new InlineKeyboardRow(); }
        }
        if (!row.isEmpty()) rows.add(row);
        rows.add(new InlineKeyboardRow(
                InlineKeyboardButton.builder().text(Messages.get(lang, Messages.RELATIONSHIP_SKIP)).callbackData(prefix + "SKIP").build()));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }
}
