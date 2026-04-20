#!/usr/bin/env bash
set -euo pipefail

REPO_DIR="${1:-.}"
cd "$REPO_DIR"

for f in src/test/java/com/festiva/IntegrationTestBase.java; do
  if [[ ! -f "$f" ]]; then
    echo "Required file not found: $f" >&2
    exit 1
  fi
done

STAMP="$(date +%Y%m%d_%H%M%S)"
BACKUP_DIR="festiva_test_singleton_mongo_fix_backup_${STAMP}"
mkdir -p "$BACKUP_DIR"

cp src/test/java/com/festiva/IntegrationTestBase.java "$BACKUP_DIR/IntegrationTestBase.java.bak"

cat > src/test/java/com/festiva/IntegrationTestBase.java <<'EOF'
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
        boolean dockerAvailable = false;

        try {
            dockerAvailable = DockerClientFactory.instance().isDockerAvailable();
            if (dockerAvailable) {
                mongo = new MongoDBContainer("mongo:7.0");
                mongo.start();
            }
        } catch (Throwable ignored) {
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
EOF

echo "Singleton Mongo Testcontainers fix applied successfully."
echo "Backup saved under: ${BACKUP_DIR}"
echo
echo "Next steps:"
echo "  1) Review diff: git diff --stat && git diff"
echo "  2) Run verification: bash festiva_test_singleton_mongo_verify.sh"
