package com.festiva.command;

import com.festiva.command.handler.ImportIcsCommandHandler;
import com.festiva.friend.api.FriendService;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.i18n.MessagesTestSupport;
import com.festiva.state.UserStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ImportIcsCommandHandler — state handling")
@ExtendWith(MockitoExtension.class)
class ImportIcsCommandHandlerStateTest extends MessagesTestSupport {

    @Mock UserStateService userStateService;
    private ImportIcsCommandHandler handler;

    @BeforeEach
    void defaults() {
        lenient().when(userStateService.getLanguage(anyLong())).thenReturn(Lang.EN);
        handler = new ImportIcsCommandHandler(
                mock(FriendService.class),
                userStateService,
                mock(TelegramClient.class)
        );
    }

    @Test
    @DisplayName("oversized ICS file is rejected at 512 KB")
    void oversizedFile_rejected() {
        Update update = oversizedDocumentUpdate();

        var result = handler.handleState(update);

        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.ICS_TOO_LARGE));
        verify(userStateService, never()).clearState(anyLong());
    }

    private Update oversizedDocumentUpdate() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);

        Document document = mock(Document.class);
        when(document.getMimeType()).thenReturn("text/calendar");
        when(document.getFileSize()).thenReturn(512L * 1024 + 1);

        Message message = mock(Message.class);
        when(message.getFrom()).thenReturn(user);
        when(message.getChatId()).thenReturn(1L);
        when(message.hasDocument()).thenReturn(true);
        when(message.getDocument()).thenReturn(document);

        Update update = mock(Update.class);
        when(update.getMessage()).thenReturn(message);
        return update;
    }
}
