#!/usr/bin/env bash
set -euo pipefail

REPO_DIR="${1:-.}"
cd "$REPO_DIR"

for f in pom.xml src/test/java/com/festiva/IntegrationTestBase.java; do
  if [[ ! -f "$f" ]]; then
    echo "Required file not found: $f" >&2
    exit 1
  fi
done

STAMP="$(date +%Y%m%d_%H%M%S)"
BACKUP_DIR="festiva_testcontainers_fix_backup_${STAMP}"
mkdir -p "$BACKUP_DIR"

cp pom.xml "$BACKUP_DIR/pom.xml.bak"
cp src/test/java/com/festiva/IntegrationTestBase.java "$BACKUP_DIR/IntegrationTestBase.java.bak"

python3 <<'PY'
from pathlib import Path
import re

pom_path = Path("pom.xml")
pom = pom_path.read_text(encoding="utf-8")

new_surefire = '''            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.1.2</version>
                <configuration>
                    <argLine>-Dnet.bytebuddy.experimental=true -XX:+EnableDynamicAgentLoading</argLine>
                </configuration>
            </plugin>'''

pattern = re.compile(
    r'<plugin>\s*<groupId>org\.apache\.maven\.plugins</groupId>\s*<artifactId>maven-surefire-plugin</artifactId>.*?</plugin>',
    re.DOTALL
)

pom_new, count = pattern.subn(new_surefire, pom, count=1)
if count != 1:
    raise SystemExit("Could not locate maven-surefire-plugin block in pom.xml")

pom_path.write_text(pom_new, encoding="utf-8")

integration_test_base = '''package com.festiva;

import com.festiva.bot.BirthdayBot;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
public abstract class IntegrationTestBase {

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    @MockitoBean
    @SuppressWarnings("unused")
    BirthdayBot birthdayBot;

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", () -> MONGO.getReplicaSetUrl("festiva-test"));
    }
}
'''
Path("src/test/java/com/festiva/IntegrationTestBase.java").write_text(integration_test_base, encoding="utf-8")
PY

echo "Testcontainers/macOS test fix applied successfully."
echo "Backup saved under: ${BACKUP_DIR}"
echo
echo "Next steps:"
echo "  1) Review diff: git diff --stat && git diff"
echo "  2) Run tests again: bash festiva_testcontainers_macos_verify.sh"
