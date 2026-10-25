package com.festiva.bot;

import com.festiva.i18n.Lang;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeChat;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class BotCommandsService {

    private final TelegramClient telegramClient;
    private final BotCommandsProvider commandsProvider;

    public void registerGlobalCommands() {
        registerGlobalCommands(Lang.EN, null);
        registerGlobalCommands(Lang.EN, Lang.EN.code());
        registerGlobalCommands(Lang.RU, Lang.RU.code());
    }

    private void registerGlobalCommands(Lang commandsLang, String languageCode) {
        try {
            SetMyCommands.SetMyCommandsBuilder builder = SetMyCommands.builder()
                    .commands(commandsProvider.getCommandsForLanguage(commandsLang));
            if (languageCode != null) {
                builder.languageCode(languageCode);
            }
            telegramClient.execute(builder.build());
        } catch (TelegramApiException e) {
            log.atWarn()
                    .setMessage("bot_commands_register_failed")
                    .addKeyValue("language", languageCode == null ? "default" : languageCode)
                    .setCause(e)
                    .log();
        }
    }

    public void updateCommandsForUser(long chatId, Lang lang) {
        try {
            telegramClient.execute(SetMyCommands.builder()
                    .commands(commandsProvider.getCommandsForLanguage(lang))
                    .scope(new BotCommandScopeChat(String.valueOf(chatId)))
                    .build());
        } catch (TelegramApiException e) {
            log.atWarn()
                    .setMessage("bot_commands_update_failed")
                    .addKeyValue("chatId", chatId)
                    .addKeyValue("language", lang.name())
                    .setCause(e)
                    .log();
        }
    }
}
