package com.festiva.command;

import com.festiva.state.BotState;
import com.festiva.state.UserStateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CommandRouter {

    private final UserStateService userStateService;
    private final Map<String, CommandHandler> handlers;
    private final Map<BotState, StatefulCommandHandler> statefulHandlers;
    private final CommandHandler defaultHandler;

    public CommandRouter(UserStateService userStateService, List<CommandHandler> allHandlers) {
        this.userStateService = userStateService;
        this.handlers = allHandlers.stream()
                .filter(h -> h.command() != null)
                .collect(Collectors.toMap(CommandHandler::command, Function.identity()));
        this.statefulHandlers = allHandlers.stream()
                .filter(h -> h instanceof StatefulCommandHandler)
                .map(h -> (StatefulCommandHandler) h)
                .flatMap(h -> h.handledStates().stream().map(state -> Map.entry(state, h)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        this.defaultHandler = allHandlers.stream()
                .filter(h -> h.command() == null)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No default command handler registered"));
    }

    public SendMessage route(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return null;

        long userId = update.getMessage().getFrom().getId();
        String text = update.getMessage().getText().trim();
        String command = text.split("[\\s@]")[0];
        BotState state = userStateService.getState(userId);

        if ("/cancel".equals(command)) {
            log.debug("router.command: userId={}, command={}", userId, command);
            return handlers.get("/cancel").handle(update);
        }

        StatefulCommandHandler statefulHandler = statefulHandlers.get(state);
        if (statefulHandler != null) {
            log.debug("router.state: userId={}, state={}, input={}", userId, state, text);
            return statefulHandler.handleState(update);
        }

        log.debug("router.command: userId={}, command={}", userId, command);
        return handlers.getOrDefault(command, defaultHandler).handle(update);
    }
}
