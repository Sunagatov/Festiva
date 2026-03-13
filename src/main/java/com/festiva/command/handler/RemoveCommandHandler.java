package com.festiva.command.handler;

import com.festiva.bot.CallbackQueryHandler;
import com.festiva.command.CommandHandler;
import com.festiva.command.MessageBuilder;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.state.UserStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RemoveCommandHandler implements CommandHandler {

    public static final String REMOVE_PAGE_PREFIX = "REMOVE_PAGE_";
    public static final int PAGE_SIZE = 10;

    private final FriendService friendService;
    private final UserStateService userStateService;

    @Override
    public String command() { return "/remove"; }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        Lang lang = userStateService.getLanguage(userId);
        List<Friend> friends = friendService.getFriendsSortedByDayMonth(userId);

        if (friends.isEmpty()) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.FRIENDS_EMPTY),
                    InlineKeyboardMarkup.builder().keyboard(List.of(new InlineKeyboardRow(
                            InlineKeyboardButton.builder()
                                    .text(Messages.get(lang, Messages.REMOVE_EMPTY_ADD))
                                    .callbackData(CallbackQueryHandler.ACTION_ADD).build()))).build());
        }
        return MessageBuilder.html(chatId, Messages.get(lang, Messages.SELECT_REMOVE),
                keyboard(friends, 0));
    }

    public InlineKeyboardMarkup keyboard(List<Friend> friends, int page) {
        int from = page * PAGE_SIZE;
        if (from >= friends.size()) from = 0;
        int to = Math.min(from + PAGE_SIZE, friends.size());

        List<InlineKeyboardRow> rows = new ArrayList<>();
        friends.subList(from, to).forEach(f -> {
            String dateStr = f.hasYear()
                    ? f.getBirthDate().format(MessageBuilder.DATE_FORMATTER)
                    : String.format("%02d.%02d", f.getBirthMonthDay().getDayOfMonth(), f.getBirthMonthDay().getMonthValue());
            rows.add(new InlineKeyboardRow(
                    InlineKeyboardButton.builder()
                            .text(f.getName() + " (" + dateStr + ")")
                            .callbackData("REMOVE_" + f.getId()).build()));
        });

        int totalPages = (int) Math.ceil((double) friends.size() / PAGE_SIZE);
        if (totalPages > 1) {
            InlineKeyboardRow nav = new InlineKeyboardRow();
            if (page > 0)
                nav.add(InlineKeyboardButton.builder().text("◀").callbackData(REMOVE_PAGE_PREFIX + (page - 1)).build());
            if (page < totalPages - 1)
                nav.add(InlineKeyboardButton.builder().text("▶").callbackData(REMOVE_PAGE_PREFIX + (page + 1)).build());
            rows.add(nav);
        }
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }
}
