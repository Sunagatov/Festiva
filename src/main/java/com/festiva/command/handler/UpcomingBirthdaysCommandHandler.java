package com.festiva.command.handler;

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

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class UpcomingBirthdaysCommandHandler implements CommandHandler {

    public static final String UPCOMING_DAYS_PREFIX = "UPCOMING_DAYS_";
    private static final int DEFAULT_DAYS = 30;

    private final FriendService friendService;
    private final UserStateService userStateService;
    private final UserPreferenceService userPreferenceService;
    private final UserDateService userDateService;

    @Override
    public String command() { return "/upcomingbirthdays"; }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        Lang lang = userPreferenceService.getLanguage(userId);
        List<Friend> friends = friendService.getFriends(userId);
        return MessageBuilder.html(chatId, buildText(friends, lang, DEFAULT_DAYS, userId), MessageBuilder.upcomingMarkup(lang, DEFAULT_DAYS));
    }

    public String buildText(List<Friend> friends, Lang lang, int daysLimit, long userId) {
        LocalDate today = userDateService.todayFor(userId);
        record Entry(Friend friend, LocalDate next, long days) {}

        List<Entry> upcoming = friends.stream()
                .map(f -> {
                    LocalDate next = f.nextBirthday(today);
                    return new Entry(f, next, ChronoUnit.DAYS.between(today, next));
                })
                .filter(e -> e.days() >= 0 && e.days() <= daysLimit)
                .sorted(Comparator.comparing(Entry::next))
                .toList();

        if (upcoming.isEmpty()) {
            return Messages.get(lang, Messages.UPCOMING_NONE, daysLimit);
        }

        StringBuilder sb = new StringBuilder(Messages.get(lang, Messages.UPCOMING_HEADER) + "\n\n");
        upcoming.forEach(e -> {
            boolean isToday = e.days() == 0;
            sb.append(isToday ? "🎂 " : "").append("<b>").append(HtmlEscaper.escape(e.friend().getName())).append("</b>")
                    .append(relationshipLabel(lang, e.friend().getRelationship())).append("\n");
            sb.append("↳ ").append(formatDateLabel(e.friend(), lang)).append(" ").append(e.friend().getZodiac()).append("\n");
            if (isToday) {
                sb.append("  ").append(Messages.get(lang, Messages.LIST_DAYS_TODAY));
                if (e.friend().hasYear()) {
                    sb.append(" ").append(Messages.yearsRu(lang, e.friend().getNextAge(today)));
                }
            } else {
                sb.append("  ").append(Messages.get(lang, Messages.LIST_DAYS_LEFT, e.days()));
                if (e.friend().hasYear()) {
                    sb.append(" ").append(Messages.get(lang, Messages.LIST_WILL_TURN, Messages.yearsRu(lang, e.friend().getNextAge(today))));
                }
            }
            sb.append("\n\n");
        });
        return sb.toString();
    }

    public InlineKeyboardMarkup filterKeyboard(Lang lang, int activeDays) {
        return MessageBuilder.upcomingMarkup(lang, activeDays);
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
