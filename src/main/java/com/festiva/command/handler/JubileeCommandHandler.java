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
public class JubileeCommandHandler implements CommandHandler {

    private final FriendService friendService;
    private final UserStateService userStateService;

    @Override
    public String command() {
        return "/jubilee";
    }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        Lang lang = userStateService.getLanguage(userId);
        LocalDate today = LocalDate.now();
        List<Friend> friends = friendService.getFriends(userId).stream()
                .sorted(Comparator.comparing(f -> f.nextBirthday(today)))
                .toList();
        String text = friends.isEmpty()
                ? Messages.get(lang, Messages.FRIENDS_EMPTY)
                : buildText(friends, lang);
        return MessageBuilder.html(chatId, text);
    }

    private String buildText(List<Friend> friends, Lang lang) {
        List<Friend> jubilee = friends.stream()
                .filter(f -> f.getNextAge() > 0 && f.getNextAge() % FriendService.JUBILEE_INTERVAL == 0)
                .toList();

        if (jubilee.isEmpty()) {
            return Messages.get(lang, Messages.JUBILEE_NONE);
        }

        LocalDate today = LocalDate.now();
        StringBuilder sb = new StringBuilder(Messages.get(lang, Messages.JUBILEE_HEADER));
        jubilee.forEach(f -> {
            LocalDate next = f.nextBirthday(today);
            long days = ChronoUnit.DAYS.between(today, next);
            String daysLabel = days == 0
                    ? " " + Messages.get(lang, Messages.JUBILEE_DAYS_TODAY)
                    : " " + Messages.get(lang, Messages.JUBILEE_DAYS_LEFT, days);
            sb.append("– <b>").append(next.format(MessageBuilder.DATE_FORMATTER))
                    .append("</b> <i>").append(f.getName()).append("</i> ")
                    .append(Messages.get(lang, Messages.JUBILEE_TURNS, f.getNextAge()))
                    .append(daysLabel).append("\n");
        });
        return sb.toString();
    }
}
