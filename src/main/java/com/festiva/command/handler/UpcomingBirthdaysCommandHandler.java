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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UpcomingBirthdaysCommandHandler implements CommandHandler {

    public static final String UPCOMING_DAYS_PREFIX = "UPCOMING_DAYS_";
    private static final int DEFAULT_DAYS = 30;

    private final FriendService friendService;
    private final UserStateService userStateService;

    @Override
    public String command() { return "/upcomingbirthdays"; }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        Lang lang = userStateService.getLanguage(userId);
        List<Friend> friends = friendService.getFriends(userId);
        return MessageBuilder.html(chatId, buildText(friends, lang, DEFAULT_DAYS), filterKeyboard(lang, DEFAULT_DAYS));
    }

    public String buildText(List<Friend> friends, Lang lang, int daysLimit) {
        LocalDate today = LocalDate.now();
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

        StringBuilder sb = new StringBuilder(Messages.get(lang, Messages.UPCOMING_HEADER));
        upcoming.forEach(e -> {
            String suffix = e.days() == 0
                    ? Messages.get(lang, Messages.UPCOMING_TODAY, Messages.yearsRu(lang, e.friend().getNextAge(today)))
                    : Messages.get(lang, Messages.UPCOMING_TURNS, Messages.yearsRu(lang, e.friend().getNextAge(today)), e.days());
            sb.append("– <b>")
                    .append(String.format("%02d.%02d", e.next().getDayOfMonth(), e.next().getMonthValue()))
                    .append("</b> <i>").append(e.friend().getName()).append("</i> ")
                    .append(suffix).append("\n");
        });
        return sb.toString();
    }

    public InlineKeyboardMarkup filterKeyboard(Lang lang, int activeDays) {
        InlineKeyboardRow row = new InlineKeyboardRow();
        for (int d : new int[]{7, 14, 30}) {
            String label = (d == activeDays ? "✅ " : "") + d + Messages.get(lang, Messages.UPCOMING_DAYS_SUFFIX);
            row.add(InlineKeyboardButton.builder().text(label).callbackData(UPCOMING_DAYS_PREFIX + d).build());
        }
        return InlineKeyboardMarkup.builder().keyboard(List.of(row)).build();
    }
}
