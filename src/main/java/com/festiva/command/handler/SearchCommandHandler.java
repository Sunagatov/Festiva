package com.festiva.command.handler;

import com.festiva.command.MessageBuilder;
import com.festiva.command.StatefulCommandHandler;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.state.BotState;
import com.festiva.state.UserStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class SearchCommandHandler implements StatefulCommandHandler {

    private final FriendService friendService;
    private final UserStateService userStateService;

    @Override
    public String command() { return "/search"; }

    @Override
    public Set<BotState> handledStates() { return Set.of(BotState.WAITING_FOR_SEARCH); }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        userStateService.setState(userId, BotState.WAITING_FOR_SEARCH);
        return MessageBuilder.html(chatId, Messages.get(userStateService.getLanguage(userId), Messages.SEARCH_PROMPT));
    }

    @Override
    public SendMessage handleState(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        Lang lang = userStateService.getLanguage(userId);
        String query = update.getMessage().getText().trim().toLowerCase(java.util.Locale.ROOT);
        userStateService.clearState(userId);

        List<Friend> matches = friendService.getFriendsSortedByDayMonth(userId).stream()
                .filter(f -> f.getName().toLowerCase(java.util.Locale.ROOT).contains(query))
                .toList();

        if (matches.isEmpty()) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.SEARCH_NONE, query));
        }

        LocalDate today = LocalDate.now();
        StringBuilder sb = new StringBuilder(Messages.get(lang, Messages.SEARCH_RESULTS, query));
        matches.forEach(f -> {
            long days = ChronoUnit.DAYS.between(today, f.nextBirthday(today));
            String daysLabel = days == 0
                    ? " 🎂"
                    : " (" + days + Messages.get(lang, Messages.UPCOMING_DAYS_SUFFIX) + ")";
            sb.append("– <b>").append(f.getBirthDate().format(MessageBuilder.DATE_FORMATTER))
                    .append("</b> ").append(f.getZodiac())
                    .append(" <i>").append(f.getName()).append("</i>")
                    .append(daysLabel).append("\n");
        });
        return MessageBuilder.html(chatId, sb.toString());
    }
}
