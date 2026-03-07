package com.festiva.command.handler;

import com.festiva.command.MessageBuilder;
import com.festiva.command.StatefulCommandHandler;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.state.BotState;
import com.festiva.state.UserStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class EditFriendCommandHandler implements StatefulCommandHandler {

    public static final String EDIT_PAGE_PREFIX = "EDIT_PAGE_";
    public static final int PAGE_SIZE = 10;

    private final FriendService friendService;
    private final UserStateService userStateService;

    @Override
    public String command() { return "/edit"; }

    @Override
    public Set<BotState> handledStates() { return Set.of(BotState.WAITING_FOR_EDIT_NAME); }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        Lang lang = userStateService.getLanguage(userId);
        List<Friend> friends = friendService.getFriendsSortedByDayMonth(userId);

        if (friends.isEmpty()) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.FRIENDS_EMPTY));
        }
        return MessageBuilder.html(chatId, Messages.get(lang, Messages.EDIT_SELECT), keyboard(friends, lang, 0));
    }

    public InlineKeyboardMarkup keyboard(List<Friend> friends, Lang lang, int page) {
        int from = page * PAGE_SIZE;
        if (from >= friends.size()) from = 0;
        int to = Math.min(from + PAGE_SIZE, friends.size());

        List<InlineKeyboardRow> rows = new ArrayList<>();
        friends.subList(from, to).forEach(f -> rows.add(new InlineKeyboardRow(
                InlineKeyboardButton.builder()
                        .text(f.getName() + " (" + f.getBirthDate().format(MessageBuilder.DATE_FORMATTER) + ")")
                        .callbackData("EDIT_" + f.getId()).build())));

        int totalPages = (int) Math.ceil((double) friends.size() / PAGE_SIZE);
        if (totalPages > 1) {
            InlineKeyboardRow nav = new InlineKeyboardRow();
            if (page > 0)
                nav.add(InlineKeyboardButton.builder().text("◀").callbackData(EDIT_PAGE_PREFIX + (page - 1)).build());
            if (page < totalPages - 1)
                nav.add(InlineKeyboardButton.builder().text("▶").callbackData(EDIT_PAGE_PREFIX + (page + 1)).build());
            if (!nav.isEmpty()) rows.add(nav);
        }
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    @Override
    public SendMessage handleState(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        Lang lang = userStateService.getLanguage(userId);
        String newName = update.getMessage().getText().trim();
        String oldName = userStateService.getPendingName(userId);

        if (newName.isBlank()) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.NAME_EMPTY));
        }
        if (newName.length() > 100) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.NAME_TOO_LONG));
        }
        if (friendService.friendExists(userId, newName)) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.NAME_EXISTS, newName));
        }

        friendService.updateFriendName(userId, oldName, newName);
        userStateService.clearState(userId);
        return MessageBuilder.html(chatId, Messages.get(lang, Messages.EDIT_NAME_DONE, newName));
    }
}
