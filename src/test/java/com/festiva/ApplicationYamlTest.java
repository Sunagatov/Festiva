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
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> sources = loader.load("applicationConfig", new ClassPathResource("application.yml"));

        Object value = sources.stream()
                .map(source -> source.getProperty("spring.servlet.multipart.enabled"))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        assertThat(value).isEqualTo(false);
    }
}
