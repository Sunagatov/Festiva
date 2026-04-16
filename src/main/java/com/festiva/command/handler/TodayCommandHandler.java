package com.festiva.command.handler;

import com.festiva.command.CommandHandler;
import com.festiva.command.MessageBuilder;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.state.UserStateService;
import com.festiva.util.HtmlEscaper;
import com.festiva.util.UserDateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TodayCommandHandler implements CommandHandler {

    private final FriendService friendService;
    private final UserStateService userStateService;
    private final UserDateService userDateService;

    @Override
    public String command() { return "/today"; }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        Lang lang = userStateService.getLanguage(userId);
        LocalDate today = userDateService.todayFor(userId);

        List<Friend> todayFriends = friendService.getFriends(userId).stream()
                .filter(f -> f.nextBirthday(today).equals(today))
                .toList();

        if (todayFriends.isEmpty()) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.TODAY_NONE));
        }

        StringBuilder sb = new StringBuilder(Messages.get(lang, Messages.TODAY_HEADER));
        todayFriends.forEach(f -> {
            sb.append("🎂 <b>").append(HtmlEscaper.escape(f.getName())).append("</b>");
            if (f.hasYear()) {
                sb.append(" — ")
                  .append(Messages.get(lang, Messages.JUBILEE_TURNS, Messages.yearsRu(lang, f.getNextAge(today))));
            }
            sb.append("\n");
        });
        sb.append("\n").append(Messages.get(lang, Messages.TODAY_HINT));
        return MessageBuilder.html(chatId, sb.toString());
    }
}
