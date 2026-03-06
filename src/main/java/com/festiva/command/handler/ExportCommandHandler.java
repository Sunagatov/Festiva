package com.festiva.command.handler;

import com.festiva.command.CommandHandler;
import com.festiva.command.MessageBuilder;
import com.festiva.friend.api.FriendService;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.state.UserStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExportCommandHandler implements CommandHandler {

    private final FriendService friendService;
    private final UserStateService userStateService;
    private final TelegramClient telegramClient;

    @Override
    public String command() { return "/export"; }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        Lang lang = userStateService.getLanguage(userId);

        var friends = friendService.getFriendsSortedByDayMonth(userId);
        if (friends.isEmpty()) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.EXPORT_EMPTY));
        }

        StringBuilder csv = new StringBuilder("name,birthday\n");
        friends.forEach(f -> {
            String name = f.getName().contains(",") ? "\"" + f.getName() + "\"" : f.getName();
            csv.append(name).append(",")
                    .append(f.getBirthDate().format(MessageBuilder.DATE_FORMATTER)).append("\n");
        });

        try {
            telegramClient.execute(SendDocument.builder()
                    .chatId(chatId)
                    .document(new InputFile(
                            new ByteArrayInputStream(csv.toString().getBytes(StandardCharsets.UTF_8)),
                            "friends.csv"))
                    .caption(Messages.get(lang, Messages.EXPORT_CAPTION))
                    .build());
        } catch (TelegramApiException e) {
            log.error("export.failed: userId={}, message={}", userId, e.getMessage(), e);
        }
        return null;
    }
}
