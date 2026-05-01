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
        userStateService.clearState(userId);

        List<Friend> matches = friendService.getFriendsSortedByDayMonth(userId).stream()
                .filter(f -> f.getName().toLowerCase(Locale.ROOT).contains(query))
                .toList();

        if (matches.isEmpty()) {
            userStateService.setState(userId, BotState.WAITING_FOR_SEARCH);
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.SEARCH_NONE, rawQuery), MessageBuilder.backToMoreMarkup(lang));
        }

        LocalDate today = userDateService.todayFor(userId);
        StringBuilder sb = new StringBuilder(Messages.get(lang, Messages.SEARCH_RESULTS, rawQuery) + "\n\n");
        matches.forEach(f -> {
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
        sb.append("\n").append(Messages.get(lang, Messages.SEARCH_RESULTS_HINT));
        return MessageBuilder.html(chatId, sb.toString(), MessageBuilder.searchResultsMarkup(lang));
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
