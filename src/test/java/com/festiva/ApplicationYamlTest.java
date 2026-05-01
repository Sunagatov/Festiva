package com.festiva;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("application.yml")
@SuppressWarnings("unused")
class ApplicationYamlTest {

    @Test
    @DisplayName("multipart is disabled because Festiva does not serve HTTP file uploads")
    void multipartDisabled() throws IOException {
        assertThat(property("spring.servlet.multipart.enabled")).isEqualTo(false);
    }

    @Test
    @DisplayName("MongoDB uses Spring Boot 4 MongoDB property namespace")
    void mongoUsesSpringBootMongoNamespace() throws IOException {
        assertThat(property("spring.mongodb.uri"))
                .isEqualTo("${MONGO_URI:mongodb://localhost:27017}");
        assertThat(property("spring.mongodb.database"))
                .isEqualTo("${MONGO_DATABASE_NAME:FestivaDatabase}");

        assertThat(property("spring.data.mongodb.uri")).isNull();
        assertThat(property("spring.data.mongodb.database")).isNull();
    }

    @Test
    @DisplayName("console log pattern includes structured key value pairs")
    void consolePatternIncludesKeyValuePairs() throws IOException {
        assertThat(property("logging.pattern.console"))
                .isEqualTo("%d{yyyy-MM-dd'T'HH:mm:ss.SSSXXX} %5p --- [%15.15t] %-40.40logger{39} : %m %kvp%n%wEx");
    }

    @Test
    @DisplayName("test profile keeps app logs but suppresses framework startup noise")
    void testProfileLoggingLevels() throws IOException {
        assertThat(testProperty("logging.level.root")).isEqualTo("WARN");
        assertThat(testProperty("logging.level.com.festiva")).isEqualTo("INFO");
        assertThat(testProperty("logging.level.org.mongodb.driver")).isEqualTo("WARN");
        assertThat(testProperty("logging.level.org.springframework.boot.test")).isEqualTo("WARN");
        assertThat(testProperty("logging.level.org.springframework.test")).isEqualTo("WARN");
        assertThat(testProperty("logging.level.org.springframework.data.mongodb")).isEqualTo("WARN");
        assertThat(testProperty("logging.level.org.testcontainers")).isEqualTo("WARN");
    }

    private Object property(String key) throws IOException {
        return property("application.yml", key);
    }

    private Object testProperty(String key) throws IOException {
        return property("application-test.yml", key);
    }

    private Object property(String resourceName, String key) throws IOException {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> sources = loader.load("applicationConfig", new ClassPathResource(resourceName));

        return sources.stream()
                .map(source -> source.getProperty(key))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
