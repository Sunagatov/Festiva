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

    private Object property(String key) throws IOException {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> sources = loader.load("applicationConfig", new ClassPathResource("application.yml"));

        return sources.stream()
                .map(source -> source.getProperty(key))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
