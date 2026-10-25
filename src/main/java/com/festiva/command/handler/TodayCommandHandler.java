package com.festiva.command.handler;

import com.festiva.command.CommandHandler;
import com.festiva.command.MessageBuilder;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.state.UserStateService;
import com.festiva.util.HtmlEscaper;
import com.festiva.user.api.UserDateService;
import com.festiva.user.api.UserPreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class TodayCommandHandler implements CommandHandler {

    public static final String TODAY_PAGE_PREFIX = "TODAY_PAGE_";
    private static final int PAGE_SIZE = 10;

    private final FriendService friendService;
    private final UserStateService userStateService;
    private final UserPreferenceService userPreferenceService;
    private final UserDateService userDateService;

    @Override
    public String command() { return "/today"; }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        Lang lang = userPreferenceService.getLanguage(userId);
        View view = view(userId, lang, 0);
        return MessageBuilder.html(chatId, view.text(), view.markup());
    }

    public View view(long userId, Lang lang, int requestedPage) {
        LocalDate today = userDateService.todayFor(userId);
        List<Friend> todayFriends = friendService.getFriends(userId).stream()
                .filter(friend -> friend.nextBirthday(today).equals(today))
                .toList();

        if (todayFriends.isEmpty()) {
            return new View(Messages.get(lang, Messages.TODAY_NONE), MessageBuilder.todayEmptyMarkup(lang));
        }

        int totalPages = (int) Math.ceil((double) todayFriends.size() / PAGE_SIZE);
        int page = Math.max(0, Math.min(requestedPage, totalPages - 1));
        int from = page * PAGE_SIZE;
        List<Friend> pageFriends = todayFriends.subList(from, Math.min(from + PAGE_SIZE, todayFriends.size()));
        StringBuilder sb = new StringBuilder(Messages.get(lang, Messages.TODAY_HEADER) + "\n\n");
        pageFriends.forEach(f -> {
            sb.append("🎂 <b>").append(HtmlEscaper.escape(f.getName())).append("</b>");
            if (f.hasYear()) {
                sb.append(" — ")
                  .append(Messages.get(lang, Messages.JUBILEE_TURNS, Messages.yearsRu(lang, f.getNextAge(today))));
            }
            sb.append("\n");
        });
        if (totalPages > 1) {
            sb.append(Messages.get(lang, Messages.LIST_PAGE, page + 1, totalPages)).append("\n");
        }
        sb.append("\n").append(Messages.get(lang, Messages.TODAY_CELEBRATE))
                .append("\n")
                .append(Messages.get(lang, Messages.TODAY_HINT));
        InlineKeyboardMarkup markup = MessageBuilder.withPagination(
                MessageBuilder.todayCelebrationMarkup(lang), TODAY_PAGE_PREFIX, page, totalPages);
        return new View(sb.toString(), markup);
    }

    public record View(String text, InlineKeyboardMarkup markup) {}
}
