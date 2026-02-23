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
import java.util.List;

@Component
@RequiredArgsConstructor
public class ListCommandHandler implements CommandHandler {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final FriendService friendService;

    @Override
    public String command() {
        return "/list";
    }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.getMessage().getChatId();
        List<Friend> friends = friendService.getFriendsSortedByDayMonth(update.getMessage().getFrom().getId());
        String text = friends.isEmpty() ? "<b>Список пользователей пуст.</b>" : buildText(friends);
        return SendMessage.builder().chatId(chatId).parseMode("HTML").text(text).build();
    }

    private String buildText(List<Friend> friends) {
        StringBuilder sb = new StringBuilder("<b>Список пользователей (текущий календарный год):</b>\n\n");
        LocalDate today = LocalDate.now();

        for (Friend f : friends) {
            LocalDate nextBirthday = f.getBirthDate().withYear(today.getYear());
            sb.append("– <b>").append(f.getBirthDate().format(DATE_FORMATTER))
                    .append("</b> <i>").append(f.getName()).append("</i> ");

            if (nextBirthday.isBefore(today) || nextBirthday.isEqual(today)) {
                sb.append("(в этом году исполнилось <b>").append(f.getAge()).append("</b>)");
            } else {
                sb.append("(сейчас пользователю <b>").append(f.getAge())
                        .append("</b>, в этом году исполнится <b>").append(f.getNextAge()).append("</b>)");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
