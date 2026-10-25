package com.festiva.command.handler;

import com.festiva.bot.CallbackResult;
import com.festiva.command.MessageBuilder;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.friend.entity.Relationship;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.user.api.UserDateService;
import com.festiva.util.HtmlEscaper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class BirthdayPeriodListService {

    public static final String CALLBACK_PREFIX = "BPL_";
    public static final int MISSED_DAYS = 60;
    private static final int PAGE_SIZE = 10;

    private final FriendService friendService;
    private final UserDateService userDateService;

    public View view(long userId, Lang lang, Period period, boolean byDate, int requestedPage) {
        LocalDate today = userDateService.todayFor(userId);
        List<Entry> entries = entriesFor(userId, period, today);
        if (entries.isEmpty()) {
            return new View(
                    period == Period.THIS_MONTH
                            ? Messages.get(lang, Messages.THIS_MONTH_NONE)
                            : Messages.get(lang, Messages.MISSED_BIRTHDAYS_NONE, MISSED_DAYS),
                    MessageBuilder.browseMarkup(lang));
        }

        List<Entry> sorted = sort(entries, period, byDate);
        int totalPages = totalPages(sorted.size());
        int page = Math.min(Math.max(requestedPage, 0), totalPages - 1);
        int from = page * PAGE_SIZE;
        List<Entry> pageEntries = sorted.subList(from, Math.min(from + PAGE_SIZE, sorted.size()));

        StringBuilder text = new StringBuilder(header(lang, period, today)).append("\n\n");
        pageEntries.forEach(entry -> appendEntry(text, entry, today, lang));
        if (totalPages > 1) {
            text.append("\n").append(Messages.get(lang, Messages.LIST_PAGE, page + 1, totalPages));
        }

        return new View(text.toString(), keyboard(lang, period, byDate, page, totalPages));
    }

    public CallbackResult handleCallback(String data, long userId, Lang lang) {
        if (data == null || !data.startsWith(CALLBACK_PREFIX)) {
            return null;
        }

        String payload = data.substring(CALLBACK_PREFIX.length());
        Period period = Period.fromCallback(payload);
        if (period == null) {
            return null;
        }
        String[] parts = payload.substring(period.callbackKey.length() + 1).split("_");
        if (parts.length != 2) {
            return null;
        }

        try {
            boolean byDate = switch (parts[0]) {
                case "DATE" -> true;
                case "NAME" -> false;
                default -> throw new IllegalArgumentException("Unknown sort");
            };
            int page = Integer.parseInt(parts[1]);
            if (page < 0) {
                return null;
            }
            View view = view(userId, lang, period, byDate, page);
            return new CallbackResult(view.text(), view.markup());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private List<Entry> entriesFor(long userId, Period period, LocalDate today) {
        List<Friend> friends = friendService.getFriends(userId);
        if (period == Period.THIS_MONTH) {
            return friends.stream()
                    .filter(friend -> friend.getBirthMonth() == today.getMonthValue())
                    .map(friend -> new Entry(friend, occurrenceInYear(friend, today.getYear())))
                    .toList();
        }

        LocalDate start = today.minusDays(MISSED_DAYS);
        return friends.stream()
                .map(friend -> new Entry(friend, friend.nextBirthday(start)))
                .filter(entry -> !entry.occurrence().isBefore(start) && entry.occurrence().isBefore(today))
                .toList();
    }

    private LocalDate occurrenceInYear(Friend friend, int year) {
        try {
            return LocalDate.of(year, friend.getBirthMonth(), friend.getBirthDay());
        } catch (DateTimeException e) {
            return LocalDate.of(year, 2, 28);
        }
    }

    private List<Entry> sort(List<Entry> entries, Period period, boolean byDate) {
        Comparator<Entry> comparator;
        if (byDate) {
            comparator = Comparator.comparing(Entry::occurrence);
            if (period == Period.MISSED) {
                comparator = comparator.reversed();
            }
        } else {
            comparator = Comparator.comparing(
                    entry -> entry.friend().getName().toLowerCase(Locale.ROOT));
        }
        return entries.stream()
                .sorted(comparator.thenComparing(entry -> entry.friend().getName()))
                .toList();
    }

    private String header(Lang lang, Period period, LocalDate today) {
        if (period == Period.MISSED) {
            return Messages.get(lang, Messages.MISSED_BIRTHDAYS_HEADER, MISSED_DAYS);
        }
        String raw = today.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, lang.locale());
        String month = Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
        return Messages.get(lang, Messages.THIS_MONTH_HEADER, month);
    }

    private void appendEntry(StringBuilder text, Entry entry, LocalDate today, Lang lang) {
        Friend friend = entry.friend();
        long days = ChronoUnit.DAYS.between(today, entry.occurrence());

        text.append(days == 0 ? "🎂 " : "")
                .append("<b>").append(HtmlEscaper.escape(friend.getName())).append("</b>")
                .append(relationshipLabel(lang, friend.getRelationship()))
                .append("\n↳ ").append(formatDate(friend, lang)).append(" ").append(friend.getZodiac());

        if (friend.hasYear() && !entry.occurrence().isAfter(today)) {
            text.append(" ").append(Messages.yearsRu(lang, friend.getAge(today)));
        }
        text.append("\n  ");

        if (days == 0) {
            text.append(Messages.get(lang, Messages.LIST_DAYS_TODAY));
        } else if (days > 0) {
            text.append(Messages.get(lang, Messages.LIST_DAYS_LEFT, days));
            if (friend.hasYear()) {
                text.append(" ").append(Messages.get(lang, Messages.LIST_WILL_TURN,
                        Messages.yearsRu(lang, friend.getNextAge(today))));
            }
        } else {
            long daysAgo = -days;
            text.append(Messages.get(lang, daysAgo == 1
                    ? Messages.MISSED_DAY_AGO
                    : Messages.MISSED_DAYS_AGO, daysAgo));
        }
        text.append("\n\n");
    }

    private String formatDate(Friend friend, Lang lang) {
        String rawMonth = Month.of(friend.getBirthMonth()).getDisplayName(TextStyle.SHORT, lang.locale());
        String normalized = rawMonth.replace(".", "");
        String shortMonth = normalized.length() > 3 ? normalized.substring(0, 3) : normalized;
        shortMonth = Character.toUpperCase(shortMonth.charAt(0)) + shortMonth.substring(1);
        String date = friend.getBirthDay() + " " + shortMonth;
        return friend.hasYear() ? date + " " + friend.getBirthYear() : date;
    }

    private String relationshipLabel(Lang lang, Relationship relationship) {
        return relationship == null ? "" : " " + relationship.label(lang);
    }

    private InlineKeyboardMarkup keyboard(Lang lang, Period period, boolean byDate, int page, int totalPages) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(new InlineKeyboardRow(
                button((byDate ? "✅ " : "") + Messages.get(lang, Messages.LIST_SORT_DATE),
                        callback(period, true, page)),
                button((!byDate ? "✅ " : "") + Messages.get(lang, Messages.LIST_SORT_NAME),
                        callback(period, false, page))));

        if (totalPages > 1) {
            InlineKeyboardRow navigation = new InlineKeyboardRow();
            if (page > 0) {
                navigation.add(button("◀", callback(period, byDate, page - 1)));
            }
            if (page < totalPages - 1) {
                navigation.add(button("▶", callback(period, byDate, page + 1)));
            }
            rows.add(navigation);
        }
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private String callback(Period period, boolean byDate, int page) {
        return CALLBACK_PREFIX + period.callbackKey + "_" + (byDate ? "DATE" : "NAME") + "_" + page;
    }

    private InlineKeyboardButton button(String text, String callback) {
        return InlineKeyboardButton.builder().text(text).callbackData(callback).build();
    }

    private int totalPages(int total) {
        return (int) Math.ceil((double) total / PAGE_SIZE);
    }

    public enum Period {
        THIS_MONTH("THIS_MONTH"),
        MISSED("MISSED");

        private final String callbackKey;

        Period(String callbackKey) {
            this.callbackKey = callbackKey;
        }

        private static Period fromCallback(String payload) {
            for (Period period : values()) {
                if (payload.startsWith(period.callbackKey + "_")) {
                    return period;
                }
            }
            return null;
        }
    }

    private record Entry(Friend friend, LocalDate occurrence) {}

    public record View(String text, InlineKeyboardMarkup markup) {}
}
