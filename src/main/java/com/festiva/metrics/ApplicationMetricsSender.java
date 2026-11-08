package com.festiva.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.concurrent.TimeUnit;

@Component
public class ApplicationMetricsSender implements MetricsSender {
    private final MeterRegistry registry;
    private final ObjectProvider<FestivaMetricsSender> kafka;

    public ApplicationMetricsSender(MeterRegistry registry, ObjectProvider<FestivaMetricsSender> kafka) {
        this.registry = registry;
        this.kafka = kafka;
    }

    @Override
    public void sendMetrics(Update update, String status, long processingTimeMillis) {
        String type = update.hasCallbackQuery() ? "callback" : update.hasMessage() ? "message" : "other";
        String outcome = "SUCCESS".equals(status) ? "success" : "ERROR".equals(status) ? "error" : "other";
        Timer.builder("festiva.telegram.updates")
                .description("Admitted Telegram updates and their processing duration")
                .tags("type", type, "status", outcome)
                .publishPercentileHistogram()
                .register(registry)
                .record(Math.max(0, processingTimeMillis), TimeUnit.MILLISECONDS);
        kafka.ifAvailable(sender -> sender.sendMetrics(update, status, processingTimeMillis));
    }
}
