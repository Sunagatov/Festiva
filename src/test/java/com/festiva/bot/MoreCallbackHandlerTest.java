package com.festiva.bot;

import com.festiva.command.CommandHandler;
import com.festiva.command.handler.MoreCommandHandler;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.i18n.MessagesTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("MoreCallbackHandler")
@SuppressWarnings("unused")
class MoreCallbackHandlerTest extends MessagesTestSupport {

    @Test
    @DisplayName("more callback → dispatches to existing command handler")
    void handle_dispatchesToExistingHandler() {
        CommandHandler searchHandler = mock(CommandHandler.class);
        when(searchHandler.command()).thenReturn("/search");
        when(searchHandler.handle(any())).thenReturn(SendMessage.builder().chatId(1L).text("search").build());

        MoreCallbackHandler handler = new MoreCallbackHandler(List.of(searchHandler));

        CallbackResult result = handler.handle(MoreCommandHandler.CALLBACK_SEARCH, 1L, 1L, Lang.EN);

        verify(searchHandler).handle(any());
        assertThat(result.sendMessage).isNotNull();
        assertThat(result.sendMessage.getText()).isEqualTo("search");
    }

    @Test
    @DisplayName("export callback with side-effect-only handler → keeps more menu visible")
    void handle_exportKeepsMoreMenuVisible() {
        CommandHandler exportHandler = mock(CommandHandler.class);
        when(exportHandler.command()).thenReturn("/export");
        when(exportHandler.handle(any())).thenReturn(null);

        MoreCallbackHandler handler = new MoreCallbackHandler(List.of(exportHandler));

        CallbackResult result = handler.handle(MoreCommandHandler.CALLBACK_EXPORT, 1L, 1L, Lang.EN);

        verify(exportHandler).handle(any());
        assertThat(result.text).contains(Messages.get(Lang.EN, Messages.MORE_HEADER));
        assertThat(result.markup).isEqualTo(MoreCommandHandler.keyboard(Lang.EN));
    }

    @Test
    @DisplayName("back callback → returns more hub without dispatching to a handler")
    void handle_backReturnsMoreHub() {
        CommandHandler searchHandler = mock(CommandHandler.class);
        when(searchHandler.command()).thenReturn("/search");

        MoreCallbackHandler handler = new MoreCallbackHandler(List.of(searchHandler));

        CallbackResult result = handler.handle(MoreCommandHandler.CALLBACK_BACK, 1L, 1L, Lang.EN);

        verify(searchHandler, never()).handle(any());
        assertThat(result.text).contains(Messages.get(Lang.EN, Messages.MORE_HEADER));
        assertThat(result.markup).isEqualTo(MoreCommandHandler.keyboard(Lang.EN));
    }
}
