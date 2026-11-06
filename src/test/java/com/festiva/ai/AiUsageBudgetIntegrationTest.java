package com.festiva.ai;

import com.festiva.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import static org.assertj.core.api.Assertions.assertThat;

class AiUsageBudgetIntegrationTest extends IntegrationTestBase {
    @Autowired MongoTemplate mongo;
    private final Clock today = Clock.fixed(Instant.parse("2026-11-05T23:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void clean() { mongo.dropCollection(AiUsageBudget.COLLECTION); }

    @Test
    void concurrentCallsAndNewInstancesCannotRefillPersistentBudget() throws Exception {
        var budget = new AiUsageBudget(mongo, 5, today);
        assertThat(budget.reserve()).isTrue();
        try (var workers = Executors.newFixedThreadPool(8)) {
            var results = workers.invokeAll(IntStream.range(0, 30)
                    .<java.util.concurrent.Callable<Boolean>>mapToObj(i -> budget::reserve).toList());
            int allowed = 1;
            for (var result : results) if (result.get()) allowed++;
            assertThat(allowed).isEqualTo(5);
        }
        assertThat(new AiUsageBudget(mongo, 5, today).reserve()).isFalse();
        Clock tomorrow = Clock.fixed(today.instant().plusSeconds(86400), ZoneOffset.UTC);
        assertThat(new AiUsageBudget(mongo, 5, tomorrow).reserve()).isTrue();
        assertThat(mongo.getCollection(AiUsageBudget.COLLECTION).countDocuments()).isEqualTo(1);
    }

    @Test
    void zeroPausesPaidWorkWithoutCreatingUsageDocument() {
        assertThat(new AiUsageBudget(mongo, 0, today).reserve()).isFalse();
        assertThat(mongo.getCollection(AiUsageBudget.COLLECTION).countDocuments()).isZero();
    }
}
