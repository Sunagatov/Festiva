package com.festiva.command;

import com.festiva.state.BotState;
import com.festiva.state.UserStateService;
import com.festiva.i18n.Messages;
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
        if (!update.hasMessage()) return null;
        if (!update.getMessage().hasText() && !update.getMessage().hasDocument()) return null;
        if (update.getMessage().getFrom() == null) return null;

        long userId = update.getMessage().getFrom().getId();
        BotState state = userStateService.getState(userId);

        if (update.getMessage().hasDocument()) return routeDocument(update, userId, state);
        return routeText(update, userId, state);
    }

    private SendMessage routeDocument(Update update, long userId, BotState state) {
        StatefulCommandHandler h = statefulHandlers.get(state);
        if (h == null) {
            if (state != BotState.IDLE) {
                return MessageBuilder.html(update.getMessage().getChatId(),
                        Messages.get(userStateService.getLanguage(userId), Messages.USE_BUTTONS));
            }
            return null;
        }
        log.debug("router.document: userId={}, state={}", userId, state);
        return h.handleState(update);
    }

    private SendMessage routeText(Update update, long userId, BotState state) {
        String text = update.getMessage().getText().trim();
        String command = text.split("[\\s@]")[0];

        if ("/cancel".equals(command) || handlers.containsKey(command)) {
            log.debug("router.command: userId={}, command={}", userId, command);
            return handlers.getOrDefault(command, defaultHandler).handle(update);
        }

        String mappedCommand = MessageBuilder.LABEL_TO_COMMAND.get(text);
        if (mappedCommand != null) {
            log.debug("router.label: userId={}, label={}, command={}", userId, text, mappedCommand);
            userStateService.clearState(userId);
            return handlers.getOrDefault(mappedCommand, defaultHandler).handle(update);
        }

        StatefulCommandHandler h = statefulHandlers.get(state);
        if (h != null) {
            log.debug("router.state: userId={}, state={}, input={}", userId, state, text);
            return h.handleState(update);
        }

        if (state != BotState.IDLE) {
            log.debug("router.state.unhandled: userId={}, state={}, input={}", userId, state, text);
            return MessageBuilder.html(update.getMessage().getChatId(),
                    Messages.get(userStateService.getLanguage(userId), Messages.USE_BUTTONS));
        }

        log.debug("router.command: userId={}, command={}", userId, command);
        return handlers.getOrDefault(command, defaultHandler).handle(update);
    }
}
