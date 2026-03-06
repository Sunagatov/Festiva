package com.festiva.command.handler;

import com.festiva.command.CommandHandler;
import com.festiva.command.MessageBuilder;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.state.UserStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ListCommandHandler implements CommandHandler {

    public static final String LIST_PAGE_PREFIX = "LIST_PAGE_";
    public static final int PAGE_SIZE = 10;

    private final FriendService friendService;
    private final UserStateService userStateService;

    @Override
    public String command() { return "/list"; }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        Lang lang = userStateService.getLanguage(userId);
        List<Friend> friends = friendService.getFriendsSortedByDayMonth(userId);
        if (friends.isEmpty()) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.FRIENDS_EMPTY),
                    InlineKeyboardMarkup.builder().keyboard(List.of(new InlineKeyboardRow(
                            InlineKeyboardButton.builder().text(Messages.get(lang, Messages.REMOVE_EMPTY_ADD))
                                    .callbackData("ACTION_ADD").build()))).build());
        }
        return MessageBuilder.html(chatId, buildText(friends, lang, true, 0), keyboard(lang, true, 0, friends.size()));
    }

    public String buildText(List<Friend> friends, Lang lang, boolean byDate, int page) {
        LocalDate today = LocalDate.now();
        List<Friend> sorted = byDate ? friends
                : friends.stream().sorted(Comparator.comparing(f -> f.getName().toLowerCase(java.util.Locale.ROOT))).toList();

        List<Friend> pageFriends = paginate(sorted, page);
        StringBuilder sb = new StringBuilder(Messages.get(lang, Messages.LIST_HEADER));

        if (byDate) {
            List<Friend> upcoming = pageFriends.stream()
                    .filter(f -> f.nextBirthday(today).getYear() == today.getYear()).toList();
            List<Friend> celebrated = pageFriends.stream()
                    .filter(f -> f.nextBirthday(today).getYear() > today.getYear()).toList();
            if (!upcoming.isEmpty()) {
                sb.append(Messages.get(lang, Messages.LIST_UPCOMING_HEADER));
                upcoming.forEach(f -> appendFriend(sb, f, today, lang));
            }
            if (!celebrated.isEmpty()) {
                sb.append(Messages.get(lang, Messages.LIST_CELEBRATED_HEADER));
                celebrated.forEach(f -> appendFriend(sb, f, today, lang));
            }
        } else {
            pageFriends.forEach(f -> appendFriend(sb, f, today, lang));
        }

        int totalPages = totalPages(sorted.size());
        if (totalPages > 1) {
            sb.append("\n").append(Messages.get(lang, Messages.LIST_PAGE, page + 1, totalPages));
        }
        return sb.toString();
    }

    public InlineKeyboardMarkup keyboard(Lang lang, boolean byDate, int page, int total) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(new InlineKeyboardRow(
                InlineKeyboardButton.builder()
                        .text((byDate ? "✅ " : "") + Messages.get(lang, Messages.LIST_SORT_DATE))
                        .callbackData("LIST_SORT_DATE_" + page).build(),
                InlineKeyboardButton.builder()
                        .text((!byDate ? "✅ " : "") + Messages.get(lang, Messages.LIST_SORT_NAME))
                        .callbackData("LIST_SORT_NAME_" + page).build()
        ));
        int totalPages = totalPages(total);
        if (totalPages > 1) {
            InlineKeyboardRow nav = new InlineKeyboardRow();
            String mode = byDate ? "DATE" : "NAME";
            if (page > 0)
                nav.add(InlineKeyboardButton.builder().text("◀").callbackData(LIST_PAGE_PREFIX + mode + "_" + (page - 1)).build());
            if (page < totalPages - 1)
                nav.add(InlineKeyboardButton.builder().text("▶").callbackData(LIST_PAGE_PREFIX + mode + "_" + (page + 1)).build());
            if (!nav.isEmpty()) rows.add(nav);
        }
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private List<Friend> paginate(List<Friend> all, int page) {
        int from = page * PAGE_SIZE;
        if (from >= all.size()) from = 0;
        int to = Math.min(from + PAGE_SIZE, all.size());
        return all.subList(from, to);
    }

    private int totalPages(int total) {
        return (int) Math.ceil((double) total / PAGE_SIZE);
    }

    private void appendFriend(StringBuilder sb, Friend f, LocalDate today, Lang lang) {
        long daysUntil = ChronoUnit.DAYS.between(today, f.nextBirthday(today));
        String daysLabel = daysUntil == 0
                ? " " + Messages.get(lang, Messages.LIST_DAYS_TODAY)
                : " " + Messages.get(lang, Messages.LIST_DAYS_LEFT, daysUntil);
        String relLabel = f.getRelationship() != null ? " <i>" + f.getRelationship().label(lang) + "</i>" : "";
        sb.append("– <b>").append(f.getBirthDate().format(MessageBuilder.DATE_FORMATTER))
                .append("</b> ").append(f.getZodiac()).append(" <i>").append(f.getName()).append("</i>")
                .append(relLabel).append(" ");
        boolean alreadyHadBirthday = f.nextBirthday(today).getYear() > today.getYear();
        if (alreadyHadBirthday) {
            sb.append(Messages.get(lang, Messages.LIST_TURNED, f.getAge()));
        } else {
            sb.append(Messages.get(lang, Messages.LIST_WILL_TURN, f.getAge(), f.getNextAge()));
        }
        sb.append(daysLabel).append("\n");
    }
}
