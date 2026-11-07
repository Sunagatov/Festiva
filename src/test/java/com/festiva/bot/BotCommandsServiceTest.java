package com.festiva.bot;

import com.festiva.i18n.Lang;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class BotCommandsServiceTest extends com.festiva.i18n.MessagesTestSupport {

    @Test
    void registersDefaultEnglishAndRussianCommandMenus() throws Exception {
        TelegramClient client = mock(TelegramClient.class);
        BotCommandsProvider provider = mock(BotCommandsProvider.class);
        when(provider.getCommandsForLanguage(any())).thenReturn(List.of());
        BotCommandsService service = new BotCommandsService(client, provider);

        service.registerGlobalCommands();

        var captor = org.mockito.ArgumentCaptor.forClass(SetMyCommands.class);
        verify(client, times(3)).execute(captor.capture());
        assertThat(captor.getAllValues()).extracting(SetMyCommands::getLanguageCode)
                .containsExactly(null, Lang.EN.code(), Lang.RU.code());
        var descriptions = org.mockito.ArgumentCaptor.forClass(
                org.telegram.telegrambots.meta.api.methods.description.SetMyShortDescription.class);
        verify(client, times(3)).execute(descriptions.capture());
        assertThat(descriptions.getAllValues()).allSatisfy(description -> {
            assertThat(description.getShortDescription()).contains(com.festiva.command.MessageBuilder.COFFEE_URL);
            assertThat(description.getShortDescription().length()).isLessThanOrEqualTo(120);
        });
    }

    @Test
    void refreshesExistingChatMenusOnceAndAfterLanguageChanges() throws Exception {
        TelegramClient client = mock(TelegramClient.class);
        BotCommandsService service = new BotCommandsService(client, new BotCommandsProvider());
        service.ensureCommandsForUser(42L, Lang.EN);
        service.ensureCommandsForUser(42L, Lang.EN);
        service.ensureCommandsForUser(42L, Lang.RU);
        var captor = org.mockito.ArgumentCaptor.forClass(SetMyCommands.class);
        verify(client, times(2)).execute(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(menu ->
                assertThat(menu.getCommands()).extracting(command -> command.getCommand())
                        .contains("supportproject"));
        assertThat(captor.getValue().getCommands()).anySatisfy(command -> {
            assertThat(command.getCommand()).isEqualTo("supportproject");
            assertThat(command.getDescription()).contains("Поддержать проект");
        });
    }
}
