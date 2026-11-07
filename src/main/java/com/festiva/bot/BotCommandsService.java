package com.festiva.bot;

import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.command.MessageBuilder;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.DependsOn;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.description.SetMyDescription;
import org.telegram.telegrambots.meta.api.methods.description.SetMyShortDescription;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeChat;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import java.time.Duration;

@Slf4j
@Service
@DependsOn("messages")
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class BotCommandsService {

    private final TelegramClient telegramClient;
    private final BotCommandsProvider commandsProvider;
    private final Cache<Long, Lang> registeredLanguages = Caffeine.newBuilder()
            .maximumSize(1000).expireAfterWrite(Duration.ofHours(12)).build();

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
        registerProfile(commandsLang, languageCode);
    }

    private void registerProfile(Lang lang, String languageCode) {
        try {
            telegramClient.execute(SetMyDescription.builder()
                    .description(Messages.get(lang, Messages.SUPPORT_PROFILE, MessageBuilder.COFFEE_URL))
                    .languageCode(languageCode).build());
            telegramClient.execute(SetMyShortDescription.builder()
                    .shortDescription(Messages.get(lang, Messages.SUPPORT_PROFILE_SHORT, MessageBuilder.COFFEE_URL))
                    .languageCode(languageCode).build());
        } catch (TelegramApiException e) {
            log.atWarn().setMessage("bot_profile_register_failed")
                    .addKeyValue("language", languageCode == null ? "default" : languageCode)
                    .setCause(e).log();
        }
    }

    public void ensureCommandsForUser(long chatId, Lang lang) {
        // Old chat-scoped menus override the global menu after a deployment.
        if (lang != null && registeredLanguages.getIfPresent(chatId) != lang) {
            updateCommandsForUser(chatId, lang);
        }
    }

    public void updateCommandsForUser(long chatId, Lang lang) {
        try {
            telegramClient.execute(SetMyCommands.builder()
                    .commands(commandsProvider.getCommandsForLanguage(lang))
                    .scope(new BotCommandScopeChat(String.valueOf(chatId)))
                    .build());
            registeredLanguages.put(chatId, lang);
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
