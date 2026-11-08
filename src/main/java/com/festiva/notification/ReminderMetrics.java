package com.festiva.notification;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/** Aggregate operational outcomes only: no chat IDs, names or message contents. */
@Component
public class ReminderMetrics {
    private final MeterRegistry registry;
    private final AtomicLong lastCompleted = new AtomicLong();
    private final AtomicLong lastSuccessful = new AtomicLong();

    public ReminderMetrics(MeterRegistry registry) {
        this.registry = registry;
        registry.gauge("festiva.reminders.last.completed.seconds", lastCompleted);
        registry.gauge("festiva.reminders.last.success.seconds", lastSuccessful);
        for (String outcome : new String[]{"success", "failure"}) {
            registry.counter("festiva.reminders.deliveries", "outcome", outcome);
            registry.counter("festiva.reminders.runs", "outcome", outcome);
        }
    }

    public Timer.Sample start() { return Timer.start(registry); }

    public void complete(Timer.Sample sample, boolean succeeded, int sent, int failed) {
        String outcome = succeeded ? "success" : "failure";
        long now = Instant.now().getEpochSecond();
        lastCompleted.set(now);
        if (succeeded) lastSuccessful.set(now);
        registry.counter("festiva.reminders.runs", "outcome", outcome).increment();
        registry.counter("festiva.reminders.deliveries", "outcome", "success").increment(sent);
        registry.counter("festiva.reminders.deliveries", "outcome", "failure").increment(failed);
        sample.stop(Timer.builder("festiva.reminders.duration").tag("outcome", outcome).register(registry));
    }
}
