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

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;

@Component
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class StatsCommandHandler implements CommandHandler {

    private final FriendService friendService;
    private final UserStateService userStateService;
    private final UserPreferenceService userPreferenceService;
    private final UserDateService userDateService;

    @Override
    public String command() { return "/stats"; }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        Lang lang = userPreferenceService.getLanguage(userId);
        List<Friend> friends = friendService.getFriends(userId);
        LocalDate today = userDateService.todayFor(userId);

        int total = friends.size();
        int thisMonth = (int) friends.stream()
                .filter(f -> f.getBirthMonthDay().getMonthValue() == today.getMonthValue())
                .count();
        int jubilees = (int) friends.stream()
                .filter(Friend::hasYear)  // Only count friends with known year
                .filter(f -> f.getNextAge(today) > 0 && f.getNextAge(today) % FriendService.JUBILEE_INTERVAL == 0)
                .count();
        String nextBirthday = formatNextBirthday(friends, today, lang);

        StringJoiner text = new StringJoiner("\n");
        text.add(Messages.get(lang, Messages.STATS_HEADER));
        text.add("");
        text.add(Messages.get(lang, Messages.STATS_FRIENDS, total));
        text.add(nextBirthday);
        text.add(Messages.get(lang, Messages.STATS_THIS_MONTH, thisMonth, progressBar(thisMonth)));
        text.add(Messages.get(lang, Messages.STATS_JUBILEES, jubilees));
        return MessageBuilder.html(chatId, text.toString());
    }

    private String formatNextBirthday(List<Friend> friends, LocalDate today, Lang lang) {
        record Entry(Friend friend, long days) {}
        return friends.stream()
                .map(f -> new Entry(f, ChronoUnit.DAYS.between(today, f.nextBirthday(today))))
                .min(Comparator.comparingLong(Entry::days))
                .map(e -> {
                    String name = HtmlEscaper.escape(e.friend().getName());
                    return e.days() == 0
                            ? Messages.get(lang, Messages.STATS_NEXT_TODAY, name)
                            : Messages.get(lang, Messages.STATS_NEXT_IN_DAYS, name, e.days());
                })
                .orElse(Messages.get(lang, Messages.STATS_NEXT_NONE));
    }

    private String progressBar(int count) {
        int filled = Math.clamp(count, 0, 10);
        return "█".repeat(filled) + "░".repeat(10 - filled);
    }
}
