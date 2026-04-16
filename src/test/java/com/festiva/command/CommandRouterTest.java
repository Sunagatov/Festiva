package com.festiva.command;

import com.festiva.state.BotState;
import com.festiva.state.UserStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("CommandRouter")
class CommandRouterTest {

    UserStateService stateService;
    CommandRouter router;

    CommandHandler cancelHandler;
    StatefulCommandHandler statefulHandler;
    CommandHandler startHandler;
    CommandHandler defaultHandler;

    @BeforeEach
    void setUp() {
        stateService = new UserStateService(
            mock(com.festiva.state.UserSessionRepository.class),
            mock(com.festiva.user.UserPreferenceRepository.class),
            mock(com.festiva.state.PendingImportRepository.class)
        );

        cancelHandler   = handler("/cancel",  "cancelled");
        startHandler    = handler("/start",   "started");
        defaultHandler  = handler(null,       "default");
        statefulHandler = statefulHandler(Set.of(BotState.WAITING_FOR_ADD_FRIEND_NAME));

        router = new CommandRouter(stateService,
                List.of(cancelHandler, startHandler, statefulHandler, defaultHandler));
    }

    @Test
    @DisplayName("/cancel is always routed first, even when user is in a stateful state")
    void cancel_takesHighestPriority() {
        stateService.setState(1L, BotState.WAITING_FOR_ADD_FRIEND_NAME);
        SendMessage result = router.route(update("/cancel"));
        assertThat(result.getText()).isEqualTo("cancelled");
    }

    @Test
    @DisplayName("stateful handler is invoked when user has a matching active state")
    void statefulHandler_invokedForMatchingState() {
        stateService.setState(1L, BotState.WAITING_FOR_ADD_FRIEND_NAME);
        SendMessage result = router.route(update("some text"));
        assertThat(result.getText()).isEqualTo("stateful");
    }

    @Test
    @DisplayName("command handler is invoked by exact command match when state is IDLE")
    void commandHandler_invokedByExactMatch() {
        SendMessage result = router.route(update("/start"));
        assertThat(result.getText()).isEqualTo("started");
    }

    @Test
    @DisplayName("@botname suffix is stripped before routing — /start@mybot routes to /start")
    void command_botnameSuffixIsStripped() {
        SendMessage result = router.route(update("/start@mybot"));
        assertThat(result.getText()).isEqualTo("started");
    }

    @Test
    @DisplayName("unknown command falls through to default handler")
    void unknownCommand_routesToDefault() {
        SendMessage result = router.route(update("/unknown"));
        assertThat(result.getText()).isEqualTo("default");
    }

    @Test
    @DisplayName("returns null for updates without a text message")
    void noTextMessage_returnsNull() {
        Update update = mock(Update.class);
        when(update.hasMessage()).thenReturn(false);
        assertThat(router.route(update)).isNull();
    }

    @Test
    @DisplayName("returns null for channel posts where getFrom() is null")
    void channelPost_nullFrom_returnsNull() {
        Message message = mock(Message.class);
        when(message.getFrom()).thenReturn(null);
        when(message.hasText()).thenReturn(true);
        Update update = mock(Update.class);
        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        assertThat(router.route(update)).isNull();
    }

    // --- helpers ---

    private Update update(String text) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        Message message = mock(Message.class);
        when(message.getFrom()).thenReturn(user);
        when(message.getChatId()).thenReturn(1L);
        when(message.getText()).thenReturn(text);
        when(message.hasText()).thenReturn(true);
        Update update = mock(Update.class);
        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        return update;
    }

    private CommandHandler handler(String command, String responseText) {
        CommandHandler h = mock(CommandHandler.class);
        when(h.command()).thenReturn(command);
        when(h.handle(any())).thenReturn(SendMessage.builder().chatId(1L).text(responseText).build());
        return h;
    }

    private StatefulCommandHandler statefulHandler(Set<BotState> states) {
        StatefulCommandHandler h = mock(StatefulCommandHandler.class);
        when(h.command()).thenReturn("/add");
        when(h.handledStates()).thenReturn(states);
        when(h.handleState(any())).thenReturn(SendMessage.builder().chatId(1L).text("stateful").build());
        return h;
    }
}
