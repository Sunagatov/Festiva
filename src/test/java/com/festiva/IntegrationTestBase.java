package com.festiva;

import com.festiva.bot.BirthdayBot;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Assumptions;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MongoDBContainer;

@SpringBootTest
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    private static final boolean DOCKER_AVAILABLE;
    private static final MongoDBContainer MONGO;

    static {
        MongoDBContainer mongo = null;
        boolean dockerAvailable;

        try {
            dockerAvailable = DockerClientFactory.instance().isDockerAvailable();
            if (dockerAvailable) {
                mongo = new MongoDBContainer("mongo:7.0");
                mongo.start();
            }
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(IntegrationTestBase.class)
                    .warn("IntegrationTestBase: Docker check or container start failed — skipping integration tests", e);
            dockerAvailable = false;
            mongo = null;
        }

        DOCKER_AVAILABLE = dockerAvailable;
        MONGO = mongo;
    }

    @MockitoBean
    @SuppressWarnings("unused")
    BirthdayBot birthdayBot;

    @BeforeAll
    static void requireDockerForIntegrationTests() {
        Assumptions.assumeTrue(DOCKER_AVAILABLE,
                "Docker is required for integration tests in this test base.");
    }

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        if (DOCKER_AVAILABLE && MONGO != null) {
            registry.add("spring.mongodb.uri", () -> MONGO.getReplicaSetUrl("festiva-test"));
        }
    }
}
