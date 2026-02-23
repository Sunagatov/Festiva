package com.festiva.metrics;

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
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "kafka", name = "enabled", havingValue = "true")
public class FestivaMetricsSender implements MetricsSender {

    private static final Duration SEND_TIMEOUT = Duration.ofSeconds(5);

    private final KafkaProducer<String, String> producer;
    private final String topic;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FestivaMetricsSender(
            @Value("${kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${kafka.api-key}") String apiKey,
            @Value("${kafka.api-secret}") String apiSecret,
            @Value("${kafka.topic}") String topic) {

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

        this.producer = new KafkaProducer<>(props);
        this.topic = topic;
        log.info("Kafka Metrics Producer initialized with topic: {}", topic);
    }

    @Override
    public void sendMetrics(Update update, String status, long processingTimeMillis) {
        try {
            String json = buildJson(update, status, processingTimeMillis);
            producer.send(new ProducerRecord<>(topic, json))
                    .get(SEND_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("Failed to send metrics to Kafka", e);
        }
    }

    private String buildJson(Update update, String status, long processingTimeMillis) {
        String command = "unknown";
        String userName = "unknown";
        long userId = 0L;

        if (update.hasMessage() && update.getMessage().getFrom() != null) {
            User user = update.getMessage().getFrom();
            String text = update.getMessage().getText();
            command = (text != null && text.startsWith("/")) ? text.split("\\s+")[0] : "text_message";
            userId = user.getId();
            userName = extractUserName(user);
        } else if (update.hasCallbackQuery() && update.getCallbackQuery().getFrom() != null) {
            User user = update.getCallbackQuery().getFrom();
            userId = user.getId();
            command = update.getCallbackQuery().getData();
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
            return objectMapper.writeValueAsString(metrics);
        } catch (Exception e) {
            log.error("Failed to serialize metrics", e);
            return "{}";
        }
    }

    private String extractUserName(User user) {
        if (user.getUserName() != null && !user.getUserName().isEmpty()) {
            return user.getUserName();
        }
        String first = user.getFirstName() != null ? user.getFirstName() : "";
        String last = user.getLastName() != null ? " " + user.getLastName() : "";
        String full = (first + last).trim();
        return full.isEmpty() ? "unknown" : full;
    }

    private String sanitize(String value) {
        return value != null ? value.replaceAll("[\"\\\\r\n]", "") : "unknown";
    }

    @PreDestroy
    public void close() {
        producer.close(Duration.ofSeconds(10));
        log.info("Kafka producer closed");
    }
}
