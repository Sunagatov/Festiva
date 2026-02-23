package com.festiva.metrics;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface MetricsSender {

    void sendMetrics(Update update, String status, long processingTimeMillis);
}
