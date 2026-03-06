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
public class StatsCommandHandler implements CommandHandler {

    private final FriendService friendService;
    private final UserStateService userStateService;

    @Override
    public String command() { return "/stats"; }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        Lang lang = userStateService.getLanguage(userId);
        List<Friend> friends = friendService.getFriends(userId);
        LocalDate today = LocalDate.now();

        int total = friends.size();
        int thisMonth = (int) friends.stream()
                .filter(f -> f.getBirthDate().getMonthValue() == today.getMonthValue())
                .count();
        int jubilees = (int) friends.stream()
                .filter(f -> f.getNextAge() > 0 && f.getNextAge() % FriendService.JUBILEE_INTERVAL == 0)
                .count();
        String nextBirthday = friends.stream()
                .min(Comparator.comparing(f -> ChronoUnit.DAYS.between(today, f.nextBirthday(today))))
                .map(f -> {
                    long days = ChronoUnit.DAYS.between(today, f.nextBirthday(today));
                    return days == 0
                            ? f.getName() + " 🎂"
                            : f.getName() + " (" + days + Messages.get(lang, Messages.UPCOMING_DAYS_SUFFIX) + ")";
                })
                .orElse("—");

        return MessageBuilder.html(chatId, Messages.get(lang, Messages.STATS_HEADER, total, nextBirthday, thisMonth, jubilees));
    }
}
