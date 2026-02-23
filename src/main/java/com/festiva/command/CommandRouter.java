package com.festiva.command;

import com.festiva.command.handler.AddFriendCommandHandler;
import com.festiva.command.handler.CancelCommandHandler;
import com.festiva.command.handler.RemoveCommandHandler;
import com.festiva.state.BotState;
import com.festiva.state.UserStateService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CommandRouter {

    private final UserStateService userStateService;
    private final Map<String, CommandHandler> handlers;
    private final AddFriendCommandHandler addFriendHandler;
    private final RemoveCommandHandler removeHandler;
    private final CancelCommandHandler cancelHandler;
    private final CommandHandler defaultHandler;

    public CommandRouter(UserStateService userStateService,
                         List<CommandHandler> handlers,
                         AddFriendCommandHandler addFriendHandler,
                         RemoveCommandHandler removeHandler,
                         CancelCommandHandler cancelHandler) {
        this.userStateService = userStateService;
        this.handlers = handlers.stream()
                .filter(h -> h.command() != null)
                .collect(Collectors.toMap(CommandHandler::command, Function.identity()));
        this.addFriendHandler = addFriendHandler;
        this.removeHandler = removeHandler;
        this.cancelHandler = cancelHandler;
        this.defaultHandler = handlers.stream()
                .filter(h -> h.command() == null)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No default command handler registered"));
    }

    public SendMessage route(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return null;
        }

        Long userId = update.getMessage().getFrom().getId();
        String text = update.getMessage().getText().trim();
        String command = text.split(" ")[0];
        BotState state = userStateService.getState(userId);

        if ("/cancel".equals(command)) {
            return cancelHandler.handle(update);
        }

        return switch (state) {
            case WAITING_FOR_ADD_FRIEND_NAME -> addFriendHandler.handleAwaitingName(update);
            case WAITING_FOR_ADD_FRIEND_DATE -> addFriendHandler.handleAwaitingDate(update);
            case WAITING_FOR_REMOVE_FRIEND_INPUT -> removeHandler.handleAwaitingInput(update);
            default -> handlers.getOrDefault(command, defaultHandler).handle(update);
        };
    }
}
