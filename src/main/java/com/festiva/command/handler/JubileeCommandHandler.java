package com.festiva.command.handler;

import com.festiva.command.CommandHandler;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JubileeCommandHandler implements CommandHandler {

    private static final int JUBILEE_INTERVAL = 5;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final FriendService friendService;

    @Override
    public String command() {
        return "/jubilee";
    }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.getMessage().getChatId();
        List<Friend> friends = friendService.getFriendsSortedByUpcomingBirthday(update.getMessage().getFrom().getId());
        String text = friends.isEmpty() ? "<b>Список пользователей пуст.</b>" : buildText(friends);
        return SendMessage.builder().chatId(chatId).parseMode("HTML").text(text).build();
    }

    private String buildText(List<Friend> friends) {
        List<Friend> jubilee = friends.stream()
                .filter(f -> f.getNextAge() % JUBILEE_INTERVAL == 0)
                .toList();

        if (jubilee.isEmpty()) {
            return "<b>В ближайшее время нет юбилейных дней рождения.</b>";
        }

        StringBuilder sb = new StringBuilder("<b>Юбилейные дни рождения</b>\n\n");
        jubilee.forEach(f -> sb.append("– <b>").append(f.getBirthDate().format(DATE_FORMATTER))
                .append("</b> <i>").append(f.getName())
                .append("</i> (исполнится <b>").append(f.getNextAge()).append("</b> лет)\n"));
        return sb.toString();
    }
}
