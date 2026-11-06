package com.festiva.command;

import com.festiva.command.handler.ImportIcsCommandHandler;
import com.festiva.abuse.BotAbuseGuard;
import com.festiva.importing.ImportFileReader;
import com.festiva.friend.api.FriendService;
import com.festiva.importing.PendingIcsImportService;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.i18n.MessagesTestSupport;
import com.festiva.state.UserStateService;
import com.festiva.user.api.UserPreferenceService;
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
@SuppressWarnings("unused")
class ImportIcsCommandHandlerStateTest extends MessagesTestSupport {

    @Mock UserStateService userStateService;
    @Mock UserPreferenceService userPreferenceService;
    @Mock PendingIcsImportService pendingIcsImportService;
    @Mock ImportFileReader importFileReader;
    @Mock BotAbuseGuard abuseGuard;
    @Mock FriendService friendService;
    private ImportIcsCommandHandler handler;

    @BeforeEach
    void defaults() {
        lenient().when(userPreferenceService.getLanguage(anyLong())).thenReturn(Lang.EN);
        lenient().when(abuseGuard.allowImport(anyLong())).thenReturn(true);
        handler = new ImportIcsCommandHandler(
                friendService,
                userStateService,
                userPreferenceService,
                pendingIcsImportService,
                importFileReader,
                abuseGuard
        );
    }

    @Test
    @DisplayName("oversized ICS file is rejected at 512 KB")
    void oversizedFile_rejected() {
        Update update = documentUpdate(512L * 1024 + 1);

        var result = handler.handleState(update);

        assertThat(result.getText()).contains(Messages.get(Lang.EN, Messages.ICS_TOO_LARGE));
        verify(userStateService, never()).clearState(anyLong());
    }

    @Test
    void oversizedCalendarIsRejectedBeforeAiAndFriendWrites() throws Exception {
        var extractor = mock(com.festiva.ai.IcsNameExtractorService.class);
        org.springframework.test.util.ReflectionTestUtils.setField(handler, "icsNameExtractorService", extractor);
        var lines = new java.util.ArrayList<String>();
        for (int i = 0; i < 1001; i++) lines.addAll(java.util.List.of("BEGIN:VEVENT", "END:VEVENT"));
        when(importFileReader.download("file-id")).thenReturn(lines);
        assertThat(handler.handleState(documentUpdate(100L)).getText()).contains("Calendar is too large");
        org.mockito.Mockito.verifyNoInteractions(extractor, friendService, pendingIcsImportService);
        verify(userStateService, never()).clearState(anyLong());
    }

    @Test
    void validCalendarLimitsAiAndRetainsRemainingOriginalNames() throws Exception {
        var extractor = mock(com.festiva.ai.IcsNameExtractorService.class);
        org.springframework.test.util.ReflectionTestUtils.setField(handler, "icsNameExtractorService", extractor);
        when(extractor.extractName(org.mockito.ArgumentMatchers.anyString())).thenAnswer(i -> i.getArgument(0));
        var lines = new java.util.ArrayList<String>();
        for (int i = 0; i < 15; i++) lines.addAll(java.util.List.of("BEGIN:VEVENT", "SUMMARY:Birthday of Name" + i,
                "DTSTART;VALUE=DATE:19900101", "RRULE:FREQ=YEARLY", "END:VEVENT"));
        when(importFileReader.download("file-id")).thenReturn(lines);
        assertThat(handler.handleState(documentUpdate(100L)).getText()).contains("15");
        verify(extractor, org.mockito.Mockito.times(10)).extractName(org.mockito.ArgumentMatchers.anyString());
        org.mockito.ArgumentCaptor<java.util.List<com.festiva.friend.entity.Friend>> captured = org.mockito.ArgumentCaptor.captor();
        verify(pendingIcsImportService).save(org.mockito.ArgumentMatchers.eq(1L), captured.capture());
        assertThat(captured.getValue()).hasSize(15);
        assertThat(captured.getValue().getLast().getName()).isEqualTo("Birthday of Name14");
    }

    private Update documentUpdate(Long size) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);

        Document document = mock(Document.class);
        when(document.getMimeType()).thenReturn("text/calendar");
        when(document.getFileSize()).thenReturn(size);
        lenient().when(document.getFileId()).thenReturn("file-id");

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
