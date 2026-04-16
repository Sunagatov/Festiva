package com.festiva.command.handler;

import com.festiva.command.MessageBuilder;
import com.festiva.command.StatefulCommandHandler;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.state.BotState;
import com.festiva.state.UserStateService;
import com.festiva.util.HtmlEscaper;
import com.festiva.util.UserDateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class SearchCommandHandler implements StatefulCommandHandler {

    private final FriendService friendService;
    private final UserStateService userStateService;
    private final UserDateService userDateService;

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
        String rawQuery = update.getMessage().getText().trim();
        String query = rawQuery.toLowerCase(Locale.ROOT);

        if (query.isBlank()) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.SEARCH_PROMPT));
        }
        if (rawQuery.length() > 100) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.SEARCH_TOO_LONG));
        }
        userStateService.clearState(userId);

        List<Friend> matches = friendService.getFriendsSortedByDayMonth(userId).stream()
                .filter(f -> f.getName().toLowerCase(Locale.ROOT).contains(query))
                .toList();

        if (matches.isEmpty()) {
            userStateService.setState(userId, BotState.WAITING_FOR_SEARCH);
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.SEARCH_NONE, rawQuery));
        }

        LocalDate today = userDateService.todayFor(userId);
        StringBuilder sb = new StringBuilder(Messages.get(lang, Messages.SEARCH_RESULTS, rawQuery));
        matches.forEach(f -> {
            long days = ChronoUnit.DAYS.between(today, f.nextBirthday(today));
            String daysLabel = days == 0
                    ? " 🎂"
                    : " (" + days + Messages.get(lang, Messages.UPCOMING_DAYS_SUFFIX) + ")";
            String dateStr = f.hasYear()
                    ? f.getBirthDate().format(MessageBuilder.DATE_FORMATTER)
                    : String.format("%02d.%02d", f.getBirthMonthDay().getDayOfMonth(), f.getBirthMonthDay().getMonthValue());
            sb.append("– <b>").append(dateStr)
                    .append("</b> ").append(f.getZodiac())
                    .append(" <i>").append(HtmlEscaper.escape(f.getName())).append("</i>")
                    .append(daysLabel).append("\n");
        });
        sb.append("\n").append(Messages.get(lang, Messages.SEARCH_RESULTS_HINT));
        return MessageBuilder.html(chatId, sb.toString());
    }
}
