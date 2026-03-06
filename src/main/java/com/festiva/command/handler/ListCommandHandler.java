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
        if (friends.isEmpty()) {
            InlineKeyboardMarkup addButton = InlineKeyboardMarkup.builder()
                    .keyboard(List.of(new InlineKeyboardRow(
                            InlineKeyboardButton.builder()
                                    .text(Messages.get(lang, Messages.REMOVE_EMPTY_ADD))
                                    .callbackData("ACTION_ADD")
                                    .build())))
                    .build();
            return MessageBuilder.html(chatId, text, addButton);
        }
        return MessageBuilder.html(chatId, text);
    }

    private String buildText(List<Friend> friends, Lang lang) {
        LocalDate today = LocalDate.now();
        StringBuilder sb = new StringBuilder(Messages.get(lang, Messages.LIST_HEADER));

        List<Friend> upcoming = friends.stream()
                .filter(f -> !f.getBirthDate().withYear(today.getYear()).isBefore(today))
                .toList();
        List<Friend> celebrated = friends.stream()
                .filter(f -> f.getBirthDate().withYear(today.getYear()).isBefore(today))
                .toList();

        if (!upcoming.isEmpty()) {
            sb.append(Messages.get(lang, Messages.LIST_UPCOMING_HEADER));
            upcoming.forEach(f -> appendFriend(sb, f, today, lang));
        }
        if (!celebrated.isEmpty()) {
            sb.append(Messages.get(lang, Messages.LIST_CELEBRATED_HEADER));
            celebrated.forEach(f -> appendFriend(sb, f, today, lang));
        }
        return sb.toString();
    }

    private void appendFriend(StringBuilder sb, Friend f, LocalDate today, Lang lang) {
        long daysUntil = ChronoUnit.DAYS.between(today, f.nextBirthday(today));
        String daysLabel = daysUntil == 0
                ? " " + Messages.get(lang, Messages.LIST_DAYS_TODAY)
                : " " + Messages.get(lang, Messages.LIST_DAYS_LEFT, daysUntil);
        sb.append("– <b>").append(f.getBirthDate().format(MessageBuilder.DATE_FORMATTER))
                .append("</b> ").append(f.getZodiac()).append(" <i>").append(f.getName()).append("</i> ");
        boolean alreadyHadBirthday = f.getBirthDate().withYear(today.getYear()).isBefore(today);
        if (alreadyHadBirthday) {
            sb.append(Messages.get(lang, Messages.LIST_TURNED, f.getAge()));
        } else {
            sb.append(Messages.get(lang, Messages.LIST_WILL_TURN, f.getAge(), f.getNextAge()));
        }
        sb.append(daysLabel).append("\n");
    }
}
