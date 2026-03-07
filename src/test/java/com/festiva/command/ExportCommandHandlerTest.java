package com.festiva.command;

import com.festiva.command.handler.ExportCommandHandler;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.friend.entity.Relationship;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.i18n.MessagesTestSupport;
import com.festiva.state.UserStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("ExportCommandHandler")
@ExtendWith(MockitoExtension.class)
class ExportCommandHandlerTest extends MessagesTestSupport {

    @Mock FriendService friendService;
    @Mock UserStateService userStateService;
    @Mock TelegramClient telegramClient;
    @InjectMocks ExportCommandHandler handler;

    @BeforeEach
    void defaultLang() {
        lenient().when(userStateService.getLanguage(anyLong())).thenReturn(Lang.EN);
    }

    @Test
    @DisplayName("no friends → returns export-empty message")
    void noFriends_returnsEmptyMessage() {
        when(friendService.getFriendsSortedByDayMonth(1L)).thenReturn(List.of());

        SendMessage result = handler.handle(update());

        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.EXPORT_EMPTY));
    }

    @Test
    @DisplayName("with friends → sends document and returns null")
    void withFriends_sendsDocumentAndReturnsNull() throws Exception {
        when(friendService.getFriendsSortedByDayMonth(1L))
                .thenReturn(List.of(new Friend("Alice", LocalDate.of(1990, 3, 15))));

        SendMessage result = handler.handle(update());

        verify(telegramClient).execute(any(SendDocument.class));
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("with friends → CSV contains relationship column header and value")
    void withFriends_csvContainsRelationshipColumn() throws Exception {
        Friend alice = new Friend("Alice", LocalDate.of(1990, 3, 15), Relationship.FRIEND);
        when(friendService.getFriendsSortedByDayMonth(1L)).thenReturn(List.of(alice));

        handler.handle(update());

        ArgumentCaptor<SendDocument> captor = ArgumentCaptor.forClass(SendDocument.class);
        verify(telegramClient).execute(captor.capture());
        byte[] bytes = captor.getValue().getDocument().getNewMediaStream().readAllBytes();
        String csv = new String(bytes, StandardCharsets.UTF_8);
        assertThat(csv).contains("name,birthday,relationship");
        assertThat(csv).contains("friend");
    }

    @Test
    @DisplayName("name with comma → quoted in CSV")
    void nameWithComma_quotedInCsv() throws Exception {
        when(friendService.getFriendsSortedByDayMonth(1L))
                .thenReturn(List.of(new Friend("Smith, John", LocalDate.of(1985, 7, 22))));

        handler.handle(update());

        ArgumentCaptor<SendDocument> captor = ArgumentCaptor.forClass(SendDocument.class);
        verify(telegramClient).execute(captor.capture());
        assertThat(captor.getValue()).isNotNull();
    }

    private Update update() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        Message message = mock(Message.class);
        when(message.getFrom()).thenReturn(user);
        when(message.getChatId()).thenReturn(1L);
        Update update = mock(Update.class);
        when(update.getMessage()).thenReturn(message);
        return update;
    }
}
