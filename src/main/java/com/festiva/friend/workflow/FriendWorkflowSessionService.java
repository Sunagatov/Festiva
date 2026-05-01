package com.festiva.friend.workflow;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class FriendWorkflowSessionService {

    private final Cache<Long, FriendWorkflowSession> cache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofHours(1))
            .build();
    private final FriendWorkflowSessionRepository repository;

    private FriendWorkflowSession session(long userId) {
        return cache.get(userId, id -> {
            FriendWorkflowSession session = repository.findById(id).orElse(new FriendWorkflowSession());
            session.setUserId(id);
            session.touch();

            if (session.getLastActivity() != null
                    && session.getLastActivity().isBefore(LocalDateTime.now().minusHours(1))) {
                session = new FriendWorkflowSession();
                session.setUserId(id);
                session.touch();
            }

            return session;
        });
    }

    private void save(long userId) {
        FriendWorkflowSession session = cache.getIfPresent(userId);
        if (session != null) {
            session.touch();
            repository.save(session);
        }
    }

    public void clear(long userId) {
        FriendWorkflowSession session = session(userId);
        session.setPendingName(null);
        session.setPendingId(null);
        session.setPendingYear(null);
        session.setPendingMonth(null);
        session.setPendingDay(null);
        session.setYearPageOffset(0);
        save(userId);
    }

    public void remove(long userId) {
        cache.invalidate(userId);
        repository.deleteById(userId);
    }

    public void setPendingName(long userId, String name) {
        session(userId).setPendingName(name);
        save(userId);
    }

    public String getPendingName(long userId) {
        return session(userId).getPendingName();
    }

    public void setPendingId(long userId, String id) {
        session(userId).setPendingId(id);
        save(userId);
    }

    public String getPendingId(long userId) {
        return session(userId).getPendingId();
    }

    public void setPendingYear(long userId, Integer year) {
        session(userId).setPendingYear(year);
        save(userId);
    }

    public Integer getPendingYear(long userId) {
        return session(userId).getPendingYear();
    }

    public void setPendingMonth(long userId, Integer month) {
        session(userId).setPendingMonth(month);
        save(userId);
    }

    public Integer getPendingMonth(long userId) {
        return session(userId).getPendingMonth();
    }

    public void setPendingDay(long userId, Integer day) {
        session(userId).setPendingDay(day);
        save(userId);
    }

    public Integer getPendingDay(long userId) {
        return session(userId).getPendingDay();
    }

    public void setYearPageOffset(long userId, int offset) {
        session(userId).setYearPageOffset(offset);
        save(userId);
    }

    public int getYearPageOffset(long userId) {
        return session(userId).getYearPageOffset();
    }
}
