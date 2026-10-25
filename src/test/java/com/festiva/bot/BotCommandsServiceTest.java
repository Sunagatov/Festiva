package com.festiva.bot;

import com.festiva.i18n.Lang;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class BotCommandsServiceTest {

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
    }
}
