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

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UpcomingBirthdaysCommandHandler implements CommandHandler {

    private static final int DAYS_LIMIT = 30;

    private final FriendService friendService;
    private final UserStateService userStateService;

    @Override
    public String command() {
        return "/upcomingbirthdays";
    }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        Lang lang = userStateService.getLanguage(userId);
        List<Friend> friends = friendService.getFriends(userId);
        return MessageBuilder.html(chatId, buildText(friends, lang));
    }

    private String buildText(List<Friend> friends, Lang lang) {
        LocalDate today = LocalDate.now();
        record Entry(Friend friend, LocalDate next, long days) {}

        List<Entry> upcoming = friends.stream()
                .map(f -> {
                    LocalDate next = f.nextBirthday(today);
                    return new Entry(f, next, ChronoUnit.DAYS.between(today, next));
                })
                .filter(e -> e.days() >= 0 && e.days() <= DAYS_LIMIT)
                .sorted(Comparator.comparing(Entry::next))
                .toList();

        if (upcoming.isEmpty()) {
            return Messages.get(lang, Messages.UPCOMING_NONE, DAYS_LIMIT);
        }

        StringBuilder sb = new StringBuilder(Messages.get(lang, Messages.UPCOMING_HEADER));
        upcoming.forEach(e -> sb.append("– <b>").append(e.next().format(MessageBuilder.DATE_FORMATTER))
                .append("</b> <i>").append(e.friend().getName()).append("</i> ")
                .append(Messages.get(lang, Messages.UPCOMING_TURNS, e.friend().getNextAge(), e.days()))
                .append("\n"));
        return sb.toString();
    }
}
