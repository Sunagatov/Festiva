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
import java.util.List;

@Component
@RequiredArgsConstructor
public class ListCommandHandler implements CommandHandler {

    private final FriendService friendService;
    private final UserStateService userStateService;

    @Override
    public String command() {
        return "/list";
    }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        Lang lang = userStateService.getLanguage(userId);
        List<Friend> friends = friendService.getFriendsSortedByDayMonth(userId);
        String text = friends.isEmpty()
                ? Messages.get(lang, Messages.FRIENDS_EMPTY)
                : buildText(friends, lang);
        return MessageBuilder.html(chatId, text);
    }

    private String buildText(List<Friend> friends, Lang lang) {
        StringBuilder sb = new StringBuilder(Messages.get(lang, Messages.LIST_HEADER));
        LocalDate today = LocalDate.now();
        for (Friend f : friends) {
            sb.append("– <b>").append(f.getBirthDate().format(MessageBuilder.DATE_FORMATTER))
                    .append("</b> <i>").append(f.getName()).append("</i> ");
            boolean alreadyHadBirthday = f.getBirthDate().withYear(today.getYear()).isBefore(today);
            if (alreadyHadBirthday) {
                sb.append(Messages.get(lang, Messages.LIST_TURNED, f.getAge()));
            } else {
                sb.append(Messages.get(lang, Messages.LIST_WILL_TURN, f.getAge(), f.getNextAge()));
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
