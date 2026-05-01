package com.festiva.state;

import com.festiva.friend.entity.Friend;
import com.festiva.friend.workflow.FriendWorkflowSessionRepository;
import com.festiva.friend.workflow.FriendWorkflowSessionService;
import com.festiva.importing.PendingIcsImportRepository;
import com.festiva.importing.PendingIcsImportService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@SuppressWarnings("unused")
public class UserStateService {

    private final Cache<Long, UserSession> cache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofHours(1))
            .build();
    private final UserSessionRepository sessionRepository;
    private final FriendWorkflowSessionService friendWorkflowSessionService;
    private final PendingIcsImportService pendingIcsImportService;

    @Autowired
    public UserStateService(UserSessionRepository sessionRepository,
                            FriendWorkflowSessionService friendWorkflowSessionService,
                            PendingIcsImportService pendingIcsImportService) {
        this.sessionRepository = sessionRepository;
        this.friendWorkflowSessionService = friendWorkflowSessionService;
        this.pendingIcsImportService = pendingIcsImportService;
    }

    @Deprecated
    public UserStateService(UserSessionRepository sessionRepository,
                            FriendWorkflowSessionRepository friendWorkflowSessionRepository,
                            PendingIcsImportRepository pendingIcsImportRepository) {
        this(sessionRepository,
                new FriendWorkflowSessionService(friendWorkflowSessionRepository),
                new PendingIcsImportService(pendingIcsImportRepository));
    }

    private UserSession session(long userId) {
        return cache.get(userId, id -> {
            UserSession session = sessionRepository.findById(id).orElse(new UserSession());
            session.setUserId(id);
            session.touch();
            
            // Reject stale sessions from DB
            if (session.getLastActivity() != null && 
                session.getLastActivity().isBefore(LocalDateTime.now().minusHours(1))) {
                session = new UserSession();
                session.setUserId(id);
                session.touch();
            }
            
            return session;
        });
    }
    
    private void saveSession(long userId) {
        UserSession session = cache.getIfPresent(userId);
        if (session != null) {
            session.touch();
            sessionRepository.save(session);
        }
    }

    public BotState getState(long userId) { return session(userId).getState(); }
    public void setState(long userId, BotState state) { 
        session(userId).setState(state);
        saveSession(userId);
    }

    public void clearState(long userId) {
        UserSession s = session(userId);
        s.setState(BotState.IDLE);
        friendWorkflowSessionService.clear(userId);
        pendingIcsImportService.delete(userId);
        saveSession(userId);
    }

    public void removeSession(long userId) {
        cache.invalidate(userId);
        sessionRepository.deleteById(userId);
        friendWorkflowSessionService.remove(userId);
    }

    public void setPendingIcsImport(long userId, java.util.List<Friend> friends) {
        pendingIcsImportService.save(userId, friends);
    }
    
    public java.util.List<Friend> getPendingIcsImport(long userId) {
        return pendingIcsImportService.get(userId);
    }
}
