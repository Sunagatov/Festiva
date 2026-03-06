package com.festiva.state;

import com.festiva.i18n.Lang;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserStateService {

    private static class UserSession {
        BotState state = BotState.IDLE;
        String pendingName = null;
        Lang lang = Lang.RU;
    }

    private final ConcurrentHashMap<Long, UserSession> sessions = new ConcurrentHashMap<>();

    private UserSession session(long userId) {
        return sessions.computeIfAbsent(userId, k -> new UserSession());
    }

    public BotState getState(long userId) { return session(userId).state; }
    public void setState(long userId, BotState state) { session(userId).state = state; }
    public void clearState(long userId) { UserSession s = session(userId); s.state = BotState.IDLE; s.pendingName = null; }

    public void setPendingName(long userId, String name) { session(userId).pendingName = name; }
    public String getPendingName(long userId) { return session(userId).pendingName; }

    public Lang getLanguage(long userId) { return session(userId).lang; }
    public void setLanguage(long userId, Lang lang) { session(userId).lang = lang; }
}
