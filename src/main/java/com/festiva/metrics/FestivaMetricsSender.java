package com.festiva.metrics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Properties;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "kafka", name = "enabled", havingValue = "true")
public class FestivaMetricsSender implements MetricsSender {

    private final KafkaProducer<String, String> producer;
    private final String topic;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @SuppressWarnings("unused")
    public FestivaMetricsSender(
            @Value("${kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${kafka.api-key}") String apiKey,
            @Value("${kafka.api-secret}") String apiSecret,
            @Value("${kafka.topic}") String topic) {

        this.producer = new KafkaProducer<>(buildProperties(bootstrapServers, apiKey, apiSecret));
        this.topic = topic;
    }

    private static Properties buildProperties(String bootstrapServers, String apiKey, String apiSecret) {
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("security.protocol", "SASL_SSL");
        props.put("sasl.mechanism", "PLAIN");
        props.put("sasl.jaas.config",
                "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                        "username=\"" + apiKey + "\" password=\"" + apiSecret + "\";");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("acks", "1");
        props.put("retries", 3);
        props.put("request.timeout.ms", 30000);
        props.put("delivery.timeout.ms", 60000);
        return props;
    }

    @Override
    public void sendMetrics(Update update, String status, long processingTimeMillis) {
        try {
            String json = buildJson(update, status, processingTimeMillis);
            producer.send(new ProducerRecord<>(topic, json), (_, ex) -> {
                if (ex != null) log.error("metrics.kafka.send.failed: message={}", ex.getMessage(), ex);
            });
        } catch (RuntimeException e) {
            log.error("metrics.payload.build.failed: message={}", e.getMessage(), e);
        }
    }

    private String buildJson(Update update, String status, long processingTimeMillis) {
        String command = "unknown";
        String userName = "unknown";
        long userId = 0L;

        User user = null;
        if (update.hasMessage() && update.getMessage().getFrom() != null) {
            user = update.getMessage().getFrom();
            String text = update.getMessage().getText();
            command = (text != null && text.startsWith("/")) ? text.split("\\s+")[0] : "text_message";
        } else if (update.hasCallbackQuery() && update.getCallbackQuery().getFrom() != null) {
            user = update.getCallbackQuery().getFrom();
            String data = update.getCallbackQuery().getData();
            command = data != null ? data.replaceAll("_.*", "_*") : "callback";
        }
        if (user != null) {
            userId = user.getId();
            userName = extractUserName(user);
        }

        Map<String, Object> metrics = Map.of(
                "timestamp", Instant.now().toString(),
                "userId", userId,
                "userName", sanitize(userName),
                "command", sanitize(command),
                "status", status,
                "processingTimeMillis", processingTimeMillis
        );

        try {
            return OBJECT_MAPPER.writeValueAsString(metrics);
        } catch (JsonProcessingException e) {
            log.error("metrics.serialize.failed: message={}", e.getMessage(), e);
            return "{}";
        }
    }

    private String extractUserName(User user) {
        String username = user.getUserName();
        if (username != null && !username.isBlank()) return username;
        String full = (user.getFirstName() +
                (user.getLastName() != null ? " " + user.getLastName() : "")).trim();
        return full.isBlank() ? "unknown" : full;
    }

    private String sanitize(String value) {
        return value != null ? value.replaceAll("[\"\\\\\r\n]", "") : "unknown";
    }

    @PreDestroy
    public void close() {
        producer.close(Duration.ofSeconds(10));
    }
}