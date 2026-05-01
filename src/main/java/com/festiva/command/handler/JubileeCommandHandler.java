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

@Component
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class JubileeCommandHandler implements CommandHandler {

    private final FriendService friendService;
    private final UserStateService userStateService;
    private final UserPreferenceService userPreferenceService;
    private final UserDateService userDateService;

    @Override
    public String command() {
        return "/jubilee";
    }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        Lang lang = userPreferenceService.getLanguage(userId);
        LocalDate today = userDateService.todayFor(userId);
        List<Friend> friends = friendService.getFriends(userId).stream()
                .sorted(Comparator.comparing(f -> f.nextBirthday(today)))
                .toList();
        if (friends.isEmpty()) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.FRIENDS_EMPTY),
                    MessageBuilder.withBackToMore(lang, MessageBuilder.emptyStateAddMarkup(lang)));
        }
        return MessageBuilder.html(chatId, buildText(friends, lang, today), MessageBuilder.backToMoreMarkup(lang));
    }

    private static String milestoneEmoji(int age) {
        if (age >= 75) return "💎";
        if (age >= 60) return "🏆";
        if (age >= 50) return "🥇";
        return "🎉";
    }

    private String buildText(List<Friend> friends, Lang lang, LocalDate today) {
        List<Friend> jubilee = friends.stream()
                .filter(Friend::hasYear)  // Only friends with known year
                .filter(f -> f.getNextAge(today) > 0 && f.getNextAge(today) % FriendService.JUBILEE_INTERVAL == 0)
                .toList();

        if (jubilee.isEmpty()) {
            return Messages.get(lang, Messages.JUBILEE_NONE);
        }

        StringBuilder sb = new StringBuilder(Messages.get(lang, Messages.JUBILEE_HEADER) + "\n\n");
        jubilee.forEach(f -> {
            LocalDate next = f.nextBirthday(today);
            long days = ChronoUnit.DAYS.between(today, next);
            int age = f.getNextAge(today);

            sb.append(milestoneEmoji(age)).append(" <b>")
              .append(String.format("%02d.%02d", next.getDayOfMonth(), next.getMonthValue()))
              .append("</b>  →  <i>").append(HtmlEscaper.escape(f.getName())).append("</i>  —  ")
              .append(Messages.get(lang, Messages.JUBILEE_TURNS, Messages.yearsRu(lang, age)));

            if (days == 0) {
                sb.append("  ").append(Messages.get(lang, Messages.JUBILEE_DAYS_TODAY));
            } else {
                sb.append("  ").append(Messages.get(lang, Messages.JUBILEE_DAYS_LEFT, days));
            }
            sb.append("\n");
        });
        return sb.toString();
    }
}
