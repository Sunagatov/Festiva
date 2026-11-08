package com.festiva.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsCommandListener;
import org.springframework.boot.mongodb.autoconfigure.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MongoCommandMetricsConfiguration {
    @Bean
    MongoClientSettingsBuilderCustomizer mongoCommandMetrics(MeterRegistry registry) {
        // Pool metrics alone cannot reveal a slow or failing database command.
        return builder -> builder.addCommandListener(new MongoMetricsCommandListener(registry));
    }
}
