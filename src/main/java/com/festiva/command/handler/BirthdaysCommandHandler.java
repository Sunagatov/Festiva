package com.festiva.command.handler;

import com.festiva.command.CommandHandler;
import com.festiva.command.MessageBuilder;
import com.festiva.friend.api.FriendService;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.state.UserStateService;
import com.festiva.util.UserDateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BirthdaysCommandHandler implements CommandHandler {

    private static final int COLUMNS_PER_ROW = 4;
    private static final int TOTAL_MONTHS = 12;

    private final FriendService friendService;
    private final UserStateService userStateService;
    private final UserDateService userDateService;

    @Override
    public String command() {
        return "/birthdays";
    }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        var lang = userStateService.getLanguage(userId);
        int currentMonth = userDateService.todayFor(userId).getMonthValue();

        Map<Integer, Long> countByMonth = friendService.getFriends(userId).stream()
                .collect(Collectors.groupingBy(f -> f.getBirthMonthDay().getMonthValue(), Collectors.counting()));

        return MessageBuilder.html(chatId,
                Messages.get(lang, Messages.BIRTHDAYS_PICK),
                InlineKeyboardMarkup.builder().keyboard(buildKeyboard(lang, currentMonth, countByMonth)).build());
    }

    private List<InlineKeyboardRow> buildKeyboard(Lang lang, int currentMonth, Map<Integer, Long> countByMonth) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(new InlineKeyboardRow(button(Messages.get(lang, Messages.CURRENT_MONTH), "MONTH_CURRENT")));

        for (int m = 1; m <= TOTAL_MONTHS; m += COLUMNS_PER_ROW) {
            InlineKeyboardRow row = new InlineKeyboardRow();
            for (int col = 0; col < COLUMNS_PER_ROW && m + col <= TOTAL_MONTHS; col++) {
                int month = m + col;
                String raw = Month.of(month).getDisplayName(TextStyle.SHORT, lang.locale());
                String name = Character.toUpperCase(raw.charAt(0)) + raw.substring(1).replace(".", "");
                long count = countByMonth.getOrDefault(month, 0L);
                String label = (month == currentMonth ? "📍 " : "") + name + (count > 0 ? " (" + count + ")" : "");
                row.add(button(label, "MONTH_" + month));
            }
            rows.add(row);
        }
        return rows;
    }

    private InlineKeyboardButton button(String text, String callbackData) {
        return InlineKeyboardButton.builder().text(text).callbackData(callbackData).build();
    }
}
