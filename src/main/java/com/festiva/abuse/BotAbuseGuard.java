package com.festiva.abuse;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/** Admission happens before database access, Telegram replies and queued work. */
@Slf4j
@Component
public class BotAbuseGuard {
    private final Clock clock;
    private final int userUpdates;
    private final int globalUpdates;
    private final int userImports;
    private final int globalImports;
    private final Cache<Long, Window> users = Caffeine.newBuilder().maximumSize(10_000)
            .expireAfterWrite(Duration.ofMinutes(2)).build();
    private final Cache<Long, Window> importers = Caffeine.newBuilder().maximumSize(10_000)
            .expireAfterWrite(Duration.ofHours(2)).build();
    private final Window updates = new Window();
    private final Window imports = new Window();
    private final AtomicLong rejected = new AtomicLong();

    @Autowired
    public BotAbuseGuard(@Value("${festiva.abuse.updates-per-user-minute:30}") int userUpdates,
                         @Value("${festiva.abuse.updates-per-minute:300}") int globalUpdates,
                         @Value("${festiva.abuse.imports-per-user-hour:3}") int userImports,
                         @Value("${festiva.abuse.imports-per-hour:20}") int globalImports) {
        this(userUpdates, globalUpdates, userImports, globalImports, Clock.systemUTC());
    }

    BotAbuseGuard(int userUpdates, int globalUpdates, int userImports, int globalImports, Clock clock) {
        if (userUpdates < 1 || globalUpdates < 1 || userImports < 1 || globalImports < 1) {
            throw new IllegalArgumentException("Abuse allowances must be positive");
        }
        this.userUpdates = userUpdates;
        this.globalUpdates = globalUpdates;
        this.userImports = userImports;
        this.globalImports = globalImports;
        this.clock = clock;
    }

    public synchronized boolean allowUpdate(long userId) {
        boolean allowed = admit(userId, updates, globalUpdates, users, userUpdates, 60);
        if (!allowed) rejected.incrementAndGet();
        return allowed;
    }

    public synchronized boolean allowImport(long userId) {
        // Attempts, including failed files, consume allowance. /cancel cannot reset it.
        boolean allowed = admit(userId, imports, globalImports, importers, userImports, 3600);
        if (!allowed) rejected.incrementAndGet();
        return allowed;
    }

    public void queueFull() { rejected.incrementAndGet(); }

    private boolean admit(long userId, Window global, int globalLimit, Cache<Long, Window> cache, int userLimit, int seconds) {
        if (userId <= 0 || !hasRoom(global, globalLimit, seconds)) return false;
        Window user = cache.get(userId, id -> new Window());
        if (!hasRoom(user, userLimit, seconds)) return false;
        global.used++;
        user.used++;
        return true;
    }

    private boolean hasRoom(Window window, int allowance, int seconds) {
        long now = clock.millis();
        if (now >= window.resetAt) {
            window.resetAt = now + seconds * 1000L;
            window.used = 0;
        }
        return window.used < allowance;
    }

    @Scheduled(fixedDelay = 60_000)
    public void summarize() {
        long count = rejected.getAndSet(0);
        if (count > 0) log.warn("bot_abuse_rejected count={} intervalSeconds=60", count);
    }

    private static class Window { long resetAt; int used; }
}
