package com.festiva.abuse;

import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import static org.assertj.core.api.Assertions.assertThat;

class BotAbuseGuardTest {
    @Test
    void throttledUserCannotDrainOtherUsersGlobalAllowance() {
        var guard = new BotAbuseGuard(2, 4, 1, 2);
        assertThat(guard.allowUpdate(1)).isTrue();
        assertThat(guard.allowUpdate(1)).isTrue();
        for (int i = 0; i < 1000; i++) assertThat(guard.allowUpdate(1)).isFalse();
        assertThat(guard.allowUpdate(2)).isTrue();
        assertThat(guard.allowUpdate(3)).isTrue();
        assertThat(guard.allowUpdate(4)).isFalse();
        assertThat(guard.allowUpdate(0)).isFalse();
    }

    @Test
    void importsShareAllowanceAcrossAliasesAndRecoverAfterHour() {
        var clock = new MutableClock();
        var guard = new BotAbuseGuard(30, 300, 1, 3, clock);
        assertThat(guard.allowImport(1)).isTrue();
        for (int i = 0; i < 1000; i++) assertThat(guard.allowImport(1)).isFalse();
        assertThat(guard.allowImport(2)).isTrue();
        assertThat(guard.allowImport(3)).isTrue();
        assertThat(guard.allowImport(4)).isFalse();
        clock.now = clock.now.plusSeconds(3600);
        assertThat(guard.allowImport(1)).isTrue();
    }

    @Test
    void concurrentAdmissionCannotOverspendGlobalBudget() throws Exception {
        var guard = new BotAbuseGuard(100, 10, 1, 20);
        try (var workers = Executors.newFixedThreadPool(8)) {
            var results = workers.invokeAll(IntStream.range(1, 101)
                    .<java.util.concurrent.Callable<Boolean>>mapToObj(id -> () -> guard.allowUpdate(id)).toList());
            int allowed = 0;
            for (var result : results) if (result.get()) allowed++;
            assertThat(allowed).isEqualTo(10);
        }
    }

    private static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-11-05T12:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return now; }
    }
}
