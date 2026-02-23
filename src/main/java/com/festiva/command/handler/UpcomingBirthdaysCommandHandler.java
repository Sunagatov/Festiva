package com.festiva.command.handler;

import com.festiva.command.CommandHandler;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UpcomingBirthdaysCommandHandler implements CommandHandler {

    private static final int DAYS_LIMIT = 30;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final FriendService friendService;

    @Override
    public String command() {
        return "/upcomingbirthdays";
    }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.getMessage().getChatId();
        List<Friend> friends = friendService.getFriendsSortedByDayMonth(update.getMessage().getFrom().getId());
        return SendMessage.builder().chatId(chatId).parseMode("HTML").text(buildText(friends)).build();
    }

    private String buildText(List<Friend> friends) {
        LocalDate today = LocalDate.now();
        List<Friend> upcoming = friends.stream()
                .filter(f -> {
                    long days = ChronoUnit.DAYS.between(today, FriendService.nextBirthday(f.getBirthDate(), today));
                    return days >= 0 && days <= DAYS_LIMIT;
                })
                .sorted(Comparator.comparing(f -> FriendService.nextBirthday(f.getBirthDate(), today)))
                .toList();

        if (upcoming.isEmpty()) {
            return "<b>В ближайшие " + DAYS_LIMIT + " дней нет дней рождения.</b>";
        }

        StringBuilder sb = new StringBuilder("<b>Ближайшие дни рождения:</b>\n\n");
        upcoming.forEach(f -> {
            long days = ChronoUnit.DAYS.between(today, FriendService.nextBirthday(f.getBirthDate(), today));
            sb.append("– <b>").append(FriendService.nextBirthday(f.getBirthDate(), today).format(DATE_FORMATTER))
                    .append("</b> <i>").append(f.getName())
                    .append("</i> (исполнится <b>").append(f.getNextAge())
                    .append("</b>, дней до дня рождения — <b>").append(days).append("</b>)\n");
        });
        return sb.toString();
    }
}
