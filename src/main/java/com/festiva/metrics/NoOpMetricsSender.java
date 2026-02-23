package com.festiva.metrics;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@ConditionalOnProperty(prefix = "kafka", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpMetricsSender implements MetricsSender {

    @Override
    public void sendMetrics(Update update, String status, long processingTimeMillis) {
    }
}
