package com.festiva.command.handler;

import com.festiva.command.MessageBuilder;
import com.festiva.command.StatefulCommandHandler;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.friend.entity.Relationship;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.state.BotState;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class SearchCommandHandler implements StatefulCommandHandler {

    public static final String SEARCH_PAGE_PREFIX = "SEARCH_PAGE_";
    private static final int PAGE_SIZE = 10;

    private final FriendService friendService;
    private final UserStateService userStateService;
    private final UserPreferenceService userPreferenceService;
    private final UserDateService userDateService;

    @Override
    public String command() { return "/search"; }

    @Override
    public Set<BotState> handledStates() { return Set.of(BotState.WAITING_FOR_SEARCH); }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        userStateService.setState(userId, BotState.WAITING_FOR_SEARCH);
        Lang lang = userPreferenceService.getLanguage(userId);
        return MessageBuilder.html(chatId, Messages.get(lang, Messages.SEARCH_PROMPT), MessageBuilder.backToMoreMarkup(lang));
    }

    @Override
    public SendMessage handleState(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        Lang lang = userPreferenceService.getLanguage(userId);
        String rawQuery = update.getMessage().getText().trim();
        String query = rawQuery.toLowerCase(Locale.ROOT);

        if (query.isBlank()) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.SEARCH_PROMPT), MessageBuilder.backToMoreMarkup(lang));
        }
        if (rawQuery.length() > 100) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.SEARCH_TOO_LONG), MessageBuilder.backToMoreMarkup(lang));
        }
        List<Friend> matches = findMatches(userId, query);

        if (matches.isEmpty()) {
            userStateService.setState(userId, BotState.WAITING_FOR_SEARCH);
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.SEARCH_NONE, HtmlEscaper.escape(rawQuery)), MessageBuilder.backToMoreMarkup(lang));
        }

        userStateService.clearState(userId);
        userStateService.setLastSearchQuery(userId, rawQuery);
        View view = view(userId, lang, rawQuery, 0);
        return MessageBuilder.html(chatId, view.text(), view.markup());
    }

    public View view(long userId, Lang lang, String rawQuery, int requestedPage) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return null;
        }
        List<Friend> matches = findMatches(userId, rawQuery.toLowerCase(Locale.ROOT));
        if (matches.isEmpty()) {
            return new View(Messages.get(lang, Messages.SEARCH_NONE, HtmlEscaper.escape(rawQuery)),
                    MessageBuilder.backToMoreMarkup(lang));
        }

        LocalDate today = userDateService.todayFor(userId);
        int totalPages = (int) Math.ceil((double) matches.size() / PAGE_SIZE);
        int page = Math.max(0, Math.min(requestedPage, totalPages - 1));
        int from = page * PAGE_SIZE;
        List<Friend> pageMatches = matches.subList(from, Math.min(from + PAGE_SIZE, matches.size()));
        StringBuilder sb = new StringBuilder(Messages.get(lang, Messages.SEARCH_RESULTS, HtmlEscaper.escape(rawQuery)) + "\n\n");
        pageMatches.forEach(f -> {
            long days = ChronoUnit.DAYS.between(today, f.nextBirthday(today));
            LocalDate next = f.nextBirthday(today);
            boolean alreadyHadBirthday = next.equals(today) || next.getYear() > today.getYear();

            sb.append(days == 0 ? "🎂 " : "")
                    .append("<b>").append(HtmlEscaper.escape(f.getName())).append("</b>")
                    .append(relationshipLabel(lang, f.getRelationship()))
                    .append("\n");

            List<String> detail = new ArrayList<>();
            detail.add(formatDateLabel(f, lang));
            detail.add(f.getZodiac());
            if (f.hasYear() && alreadyHadBirthday) {
                detail.add(Messages.yearsRu(lang, f.getAge(today)));
            }
            sb.append("↳ ").append(String.join(" ", detail)).append("\n");

            List<String> status = new ArrayList<>();
            if (!alreadyHadBirthday) {
                status.add(Messages.get(lang, Messages.LIST_DAYS_LEFT, days));
                if (f.hasYear()) {
                    status.add(Messages.get(lang, Messages.LIST_WILL_TURN, Messages.yearsRu(lang, f.getNextAge(today))));
                }
            } else if (days == 0) {
                status.add(Messages.get(lang, Messages.LIST_DAYS_TODAY));
            }
            if (!status.isEmpty()) {
                sb.append("  ").append(String.join(" ", status)).append("\n");
            }
            sb.append("\n");
        });
        if (totalPages > 1) {
            sb.append(Messages.get(lang, Messages.LIST_PAGE, page + 1, totalPages)).append("\n");
        }
        sb.append("\n").append(Messages.get(lang, Messages.SEARCH_RESULTS_HINT));
        InlineKeyboardMarkup markup = MessageBuilder.withPagination(
                MessageBuilder.searchResultsMarkup(lang), SEARCH_PAGE_PREFIX, page, totalPages);
        return new View(sb.toString(), markup);
    }

    private List<Friend> findMatches(long userId, String query) {
        return friendService.getFriendsSortedByDayMonth(userId).stream()
                .filter(friend -> friend.getName().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)))
                .toList();
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

    public record View(String text, InlineKeyboardMarkup markup) {}
}
