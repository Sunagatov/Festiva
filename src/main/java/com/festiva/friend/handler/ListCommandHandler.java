package com.festiva.friend.handler;

import com.festiva.command.CommandHandler;
import com.festiva.command.MessageBuilder;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.friend.entity.Relationship;
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class ListCommandHandler implements CommandHandler {

    public static final String LIST_PAGE_PREFIX = "LIST_PAGE_";
    public static final int PAGE_SIZE = 10;

    private final FriendService friendService;
    private final UserStateService userStateService;
    private final UserPreferenceService userPreferenceService;
    private final UserDateService userDateService;

    @Override
    public String command() { return "/list"; }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        Lang lang = userPreferenceService.getLanguage(userId);
        List<Friend> friends = friendService.getFriendsSortedByDayMonth(userId);
        if (friends.isEmpty()) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.FRIENDS_EMPTY), MessageBuilder.emptyStateAddMarkup(lang));
        }
        return MessageBuilder.html(chatId, buildText(friends, lang, true, 0, userId), keyboard(lang, true, 0, friends.size()));
    }

    public String buildText(List<Friend> friends, Lang lang, boolean byDate, int page, long userId) {
        LocalDate today = userDateService.todayFor(userId);
        List<Friend> sorted = byDate ? friends
                : friends.stream().sorted(Comparator.comparing(f -> f.getName().toLowerCase(java.util.Locale.ROOT))).toList();

        List<Friend> pageFriends = paginate(sorted, page);
        StringBuilder sb = new StringBuilder(Messages.get(lang, Messages.LIST_HEADER) + "\n\n");

        if (byDate) {
            List<Friend> upcoming = pageFriends.stream()
                    .filter(f -> f.nextBirthday(today).getYear() == today.getYear()).toList();
            List<Friend> celebrated = pageFriends.stream()
                    .filter(f -> f.nextBirthday(today).getYear() > today.getYear()).toList();
            if (!upcoming.isEmpty()) {
                sb.append(Messages.get(lang, Messages.LIST_UPCOMING_HEADER)).append("\n");
                upcoming.forEach(f -> appendFriend(sb, f, today, lang));
            }
            if (!celebrated.isEmpty()) {
                sb.append(Messages.get(lang, Messages.LIST_CELEBRATED_HEADER)).append("\n");
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
            rows.add(nav);
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
        String relLabel = relationshipLabel(lang, f.getRelationship());
        String namePrefix = daysUntil == 0 ? "🎂 " : "";
        LocalDate next = f.nextBirthday(today);
        boolean alreadyHadBirthday = next.equals(today) || next.getYear() > today.getYear();

        sb.append(namePrefix).append("<b>").append(HtmlEscaper.escape(f.getName())).append("</b>")
                .append(relLabel).append("\n");

        List<String> firstLine = new ArrayList<>();
        firstLine.add(formatDateLabel(f, lang));
        firstLine.add(f.getZodiac());
        if (f.hasYear() && alreadyHadBirthday) {
            firstLine.add(Messages.yearsRu(lang, f.getAge(today)));
        }
        sb.append("↳ ").append(String.join(" ", firstLine)).append("\n");

        List<String> details = new ArrayList<>();
        if (!alreadyHadBirthday) {
            details.add(Messages.get(lang, Messages.LIST_DAYS_LEFT, daysUntil));
        } else if (daysUntil == 0) {
            details.add(Messages.get(lang, Messages.LIST_DAYS_TODAY));
        }
        if (f.hasYear()) {
            if (!alreadyHadBirthday) {
                details.add(Messages.get(lang, Messages.LIST_WILL_TURN, Messages.yearsRu(lang, f.getNextAge(today))));
            }
        }
        if (!details.isEmpty()) {
            sb.append("  ").append(String.join(" ", details)).append("\n");
        }
        sb.append("\n");
    }

    private String formatDateLabel(Friend friend, Lang lang) {
        int month = friend.getBirthMonthDay().getMonthValue();
        String rawMonth = Month.of(month).getDisplayName(TextStyle.SHORT, lang.locale());
        String normalizedMonth = rawMonth.replace(".", "");
        String trimmedMonth = normalizedMonth.length() > 3 ? normalizedMonth.substring(0, 3) : normalizedMonth;
        String shortMonth = Character.toUpperCase(trimmedMonth.charAt(0)) + trimmedMonth.substring(1);
        if (friend.hasYear()) {
            return friend.getBirthMonthDay().getDayOfMonth() + " " + shortMonth + " " + friend.getBirthYear();
        }
        return friend.getBirthMonthDay().getDayOfMonth() + " " + shortMonth;
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
        return " " + label.substring(0, firstSpace) + " " + label.substring(firstSpace + 1);
    }
}
