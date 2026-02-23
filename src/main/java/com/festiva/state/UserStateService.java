package com.festiva.state;

import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserStateService {

    private final ConcurrentHashMap<Long, BotState> userStates = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, String> pendingNames = new ConcurrentHashMap<>();

    public BotState getState(Long userId) {
        return userStates.getOrDefault(userId, BotState.IDLE);
    }

    public void setState(Long userId, BotState state) {
        userStates.put(userId, state);
    }

    public void clearState(Long userId) {
        userStates.put(userId, BotState.IDLE);
        pendingNames.remove(userId);
    }

    public void setPendingName(Long userId, String name) {
        pendingNames.put(userId, name);
    }

    public String getPendingName(Long userId) {
        return pendingNames.get(userId);
    }
}
