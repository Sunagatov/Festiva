package com.festiva.command;

import com.festiva.state.BotState;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Set;

public interface StatefulCommandHandler extends CommandHandler {

    Set<BotState> handledStates();

    SendMessage handleState(Update update);
}
