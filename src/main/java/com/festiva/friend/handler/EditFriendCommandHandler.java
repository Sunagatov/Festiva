package com.festiva.friend.handler;

import com.festiva.command.MessageBuilder;
import com.festiva.command.StatefulCommandHandler;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.friend.entity.Relationship;
import com.festiva.friend.workflow.FriendWorkflowSessionService;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.state.BotState;
import com.festiva.state.UserStateService;
import com.festiva.user.api.UserPreferenceService;
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
@SuppressWarnings("unused")
public class EditFriendCommandHandler implements StatefulCommandHandler {

    public static final String EDIT_PAGE_PREFIX = "EDIT_PAGE_";
    public static final int PAGE_SIZE = 10;

    private final FriendService friendService;
    private final UserStateService userStateService;
    private final FriendWorkflowSessionService friendWorkflowSessionService;
    private final UserPreferenceService userPreferenceService;

    @Override
    public String command() { return "/edit"; }

    @Override
    public Set<BotState> handledStates() { return Set.of(BotState.WAITING_FOR_EDIT_NAME); }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        Lang lang = userPreferenceService.getLanguage(userId);
        List<Friend> friends = friendService.getFriendsSortedByDayMonth(userId);

        if (friends.isEmpty()) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.FRIENDS_EMPTY),
                    MessageBuilder.withBackToMore(lang, MessageBuilder.emptyStateAddMarkup(lang)));
        }
        return MessageBuilder.html(chatId, Messages.get(lang, Messages.EDIT_SELECT), keyboard(friends, 0, lang));
    }

    public InlineKeyboardMarkup keyboard(List<Friend> friends, int page) {
        return keyboard(friends, page, Lang.EN);
    }

    public InlineKeyboardMarkup keyboard(List<Friend> friends, int page, Lang lang) {
        int from = page * PAGE_SIZE;
        if (from >= friends.size()) from = 0;
        int to = Math.min(from + PAGE_SIZE, friends.size());

        List<InlineKeyboardRow> rows = new ArrayList<>();
        friends.subList(from, to).forEach(f -> {
            String dateStr = compactDate(f);
            String relationship = relationshipLabel(lang, f.getRelationship());
            rows.add(new InlineKeyboardRow(
                    InlineKeyboardButton.builder()
                            .text(f.getName() + relationship + " — " + dateStr)
                            .callbackData("EDIT_" + f.getId()).build()));
        });

        int totalPages = (int) Math.ceil((double) friends.size() / PAGE_SIZE);
        if (totalPages > 1) {
            InlineKeyboardRow nav = new InlineKeyboardRow();
            if (page > 0)
                nav.add(InlineKeyboardButton.builder().text("◀").callbackData(EDIT_PAGE_PREFIX + (page - 1)).build());
            if (page < totalPages - 1)
                nav.add(InlineKeyboardButton.builder().text("▶").callbackData(EDIT_PAGE_PREFIX + (page + 1)).build());
            rows.add(nav);
        }
        rows.add(MessageBuilder.backToMoreRow(lang));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    @Override
    public SendMessage handleState(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        Lang lang = userPreferenceService.getLanguage(userId);
        String newName = update.getMessage().getText().trim();
        String id = friendWorkflowSessionService.getPendingId(userId);
        String oldName = friendWorkflowSessionService.getPendingName(userId);

        if (id == null || oldName == null || oldName.isBlank()) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.SESSION_EXPIRED));
        }
        if (newName.isBlank()) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.NAME_EMPTY));
        }
        if (newName.length() > 100) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.NAME_TOO_LONG));
        }

        Friend currentFriend = friendService.findOwnedFriend(id, userId).orElse(null);
        if (currentFriend == null) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.SESSION_EXPIRED));
        }

        if (friendService.friendExists(userId, newName) && !newName.equalsIgnoreCase(currentFriend.getName())) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.NAME_EXISTS, newName));
        }

        friendService.updateFriendNameById(id, userId, newName);
        userStateService.clearState(userId);
        return MessageBuilder.html(chatId, Messages.get(lang, Messages.EDIT_NAME_DONE, newName), MessageBuilder.editAndListMarkup(lang));
    }

    private String compactDate(Friend friend) {
        return friend.hasYear()
                ? friend.getBirthDate().format(MessageBuilder.DATE_FORMATTER)
                : String.format("%02d.%02d", friend.getBirthMonthDay().getDayOfMonth(), friend.getBirthMonthDay().getMonthValue());
    }

    private String relationshipLabel(Lang lang, Relationship relationship) {
        if (relationship == null) {
            return "";
        }
        String label = relationship.label(lang);
        int firstSpace = label.indexOf(' ');
        if (firstSpace < 0 || firstSpace == label.length() - 1) {
            return " " + label;
        }
        return " " + label.substring(0, firstSpace);
    }
}
