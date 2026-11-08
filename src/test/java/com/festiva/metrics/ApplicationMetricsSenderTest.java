package com.festiva.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ApplicationMetricsSenderTest {
    @Test
    void recordsUpdatesWhenKafkaIsDisabled() {
        var registry = new SimpleMeterRegistry();
        var beans = new StaticListableBeanFactory();
        var sender = new ApplicationMetricsSender(registry, beans.getBeanProvider(FestivaMetricsSender.class));
        Update update = mock(Update.class);
        when(update.hasMessage()).thenReturn(true);

        sender.sendMetrics(update, "SUCCESS", 125);

        var timer = registry.get("festiva.telegram.updates").tags("type", "message", "status", "success").timer();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isEqualTo(125);
        verify(update, never()).getMessage(); // No user identity or message text enters metric labels.
    }

    @Test
    void keepsLabelsBoundedAndRejectsNegativeDurations() {
        var registry = new SimpleMeterRegistry();
        var beans = new StaticListableBeanFactory();
        var sender = new ApplicationMetricsSender(registry, beans.getBeanProvider(FestivaMetricsSender.class));
        sender.sendMetrics(mock(Update.class), "untrusted arbitrary status", -5);
        var timer = registry.get("festiva.telegram.updates").tags("type", "other", "status", "other").timer();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isZero();
    }

    @Test
    void preservesOptionalKafkaDeliveryAndRecordsCallbackFailures() {
        var registry = new SimpleMeterRegistry();
        var kafka = mock(FestivaMetricsSender.class);
        var beans = new StaticListableBeanFactory();
        beans.addBean("kafka", kafka);
        var sender = new ApplicationMetricsSender(registry, beans.getBeanProvider(FestivaMetricsSender.class));
        Update update = mock(Update.class);
        when(update.hasCallbackQuery()).thenReturn(true);

        sender.sendMetrics(update, "ERROR", 40);

        assertThat(registry.get("festiva.telegram.updates").tags("type", "callback", "status", "error").timer().count()).isEqualTo(1);
        verify(kafka).sendMetrics(update, "ERROR", 40);
    }
}
