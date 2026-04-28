# syntax=docker/dockerfile:1.7

# =============================================================================
# Festiva Dockerfile
# =============================================================================
# Goals:
# - fast enough for normal local development
# - easy to read in one pass
# - stable, boring defaults
# - no fragile "platform engineering" tricks
#
# Notes:
# - This file assumes BuildKit, which is the practical default in modern Docker.
# - We keep Spring Boot layer extraction because it gives real rebuild wins.
# - We intentionally do NOT generate CDS archives here.
#   That optimization adds build-time complexity and a more fragile image build
#   for a small local-dev payoff.


# =============================================================================
# Build Stage
# =============================================================================
FROM maven:3.9-eclipse-temurin-25-alpine AS build

# Optional Maven profile.
# Keep this empty by default so local builds stay predictable.
ARG BUILD_PROFILE=""

WORKDIR /workspace

# Copy the build descriptor first so dependency download can be cached
# separately from application source changes.
COPY pom.xml ./

# Warm the Maven cache.
# The cache mount avoids redownloading dependencies on every local rebuild.
# This step is best-effort because `go-offline` can be noisy with some plugins.
RUN --mount=type=cache,target=/root/.m2 \
    (mvn -U -B --no-transfer-progress dependency:go-offline || \
     echo "Maven go-offline was incomplete; continuing to the real build.")

# Copy only what the application image actually needs.
COPY src ./src

# Build the Spring Boot jar.
# Tests stay out of the image build because they belong in the normal dev/test
# loop, not in every Docker build.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -U -B --no-transfer-progress \
    package ${BUILD_PROFILE:+-P${BUILD_PROFILE}} -DskipTests


# =============================================================================
# Layer Extraction Stage
# =============================================================================
# Spring Boot's tools jarmode splits the fat jar into practical cacheable
# layers. This is a good middle ground:
# - dependencies stay reusable
# - application classes can change frequently
# - the file remains easy to understand
FROM eclipse-temurin:25-jre-alpine AS extract

WORKDIR /work

COPY --from=build /workspace/target/*.jar app.jar

# Use an explicit destination so the extracted layer layout is unambiguous.
RUN java -Djarmode=tools -jar app.jar extract \
    --layers \
    --launcher \
    --destination /opt/layers


# =============================================================================
# Runtime Stage
# =============================================================================
FROM eclipse-temurin:25-jre-alpine AS runtime

ARG IMAGE_VERSION=1.0.0

LABEL org.opencontainers.image.title="Festiva" \
      org.opencontainers.image.description="Telegram birthday reminder bot" \
      org.opencontainers.image.version="${IMAGE_VERSION}" \
      org.opencontainers.image.vendor="Zufar Sunagatov"

WORKDIR /app

# Create a non-root runtime user.
# Numeric IDs make ownership more predictable across environments.
RUN addgroup -S festiva -g 10001 && \
    adduser -S festiva -u 10001 -G festiva

# Copy layers from least-frequently changed to most-frequently changed.
COPY --from=extract --chown=festiva:festiva /opt/layers/dependencies/ ./
COPY --from=extract --chown=festiva:festiva /opt/layers/spring-boot-loader/ ./
COPY --from=extract --chown=festiva:festiva /opt/layers/snapshot-dependencies/ ./
COPY --from=extract --chown=festiva:festiva /opt/layers/application/ ./

USER festiva

# Festiva exposes Spring Boot on port 8080.
EXPOSE 8080

# Keep runtime defaults simple and overrideable.
# We avoid over-tuning the JVM here:
# - container-awareness is already standard
# - G1 is already the practical default
# - JAVA_OPTS gives contributors an escape hatch without rebuilding the image
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

# Use a small shell wrapper so JAVA_OPTS can be adjusted at runtime.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
