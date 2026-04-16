# =============================================================================
# BUILD STAGE — Modern 2026 approach with BuildKit cache mounts
# =============================================================================
FROM maven:3.9-eclipse-temurin-25-alpine AS build

# Build arguments
# BUILD_PROFILE: optional Maven profile for build-time optimizations.
# Leave empty by default unless Festiva gets a dedicated Maven profile later.
ARG BUILD_PROFILE=dev

WORKDIR /app

# --- Copy POM first for dependency caching ---
COPY pom.xml ./

# --- Warm Maven cache ---
# Best-effort only: some plugins can make go-offline fail even though
# the actual package build succeeds. Do not fail the image build here.
RUN --mount=type=cache,target=/root/.m2 \
    (mvn -U dependency:go-offline -B --no-transfer-progress || \
     echo "⚠️ Maven go-offline failed; continuing with package step.")

# --- Copy source code ---
COPY src ./src

# --- Build application with cached dependencies ---
RUN --mount=type=cache,target=/root/.m2 \
    mvn -U package ${BUILD_PROFILE:+-P${BUILD_PROFILE}} -DskipTests -B --no-transfer-progress

# =============================================================================
# EXTRACT STAGE — split fat JAR into layers for Docker cache efficiency
# =============================================================================
FROM eclipse-temurin:25-jre-alpine AS extract
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# =============================================================================
# CDS TRAINING STAGE — generate class-data sharing archive
# =============================================================================
# Best-effort for Festiva:
# if CDS generation fails because app startup needs external config/services,
# continue without failing the whole Docker build.
FROM eclipse-temurin:25-jre-alpine AS cds-train
WORKDIR /app

COPY --from=extract /app/dependencies/ ./
COPY --from=extract /app/spring-boot-loader/ ./
COPY --from=extract /app/snapshot-dependencies/ ./
COPY --from=extract /app/application/ ./

RUN java -XX:ArchiveClassesAtExit=app-cds.jsa \
        -Dspring.context.exit=onRefresh \
        org.springframework.boot.loader.launch.JarLauncher 2>/dev/null || true

# =============================================================================
# RUNTIME STAGE
# =============================================================================
FROM eclipse-temurin:25-jre-alpine

# Build arguments for runtime metadata
ARG IMAGE_VERSION=1.0.0

# OCI-compliant metadata labels
LABEL org.opencontainers.image.title="Festiva" \
      org.opencontainers.image.version="${IMAGE_VERSION}" \
      org.opencontainers.image.description="Telegram birthday reminder bot" \
      org.opencontainers.image.vendor="Zufar Sunagatov"

# --- Create non-root user for security hardening ---
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# --- Layered copy: dependencies change rarely, application changes often ---
COPY --from=extract --chown=appuser:appgroup /app/dependencies/ ./
COPY --from=extract --chown=appuser:appgroup /app/spring-boot-loader/ ./
COPY --from=extract --chown=appuser:appgroup /app/snapshot-dependencies/ ./
COPY --from=extract --chown=appuser:appgroup /app/application/ ./
COPY --from=cds-train --chown=appuser:appgroup /app/app-cds.jsa ./app-cds.jsa

# --- Switch to non-root user ---
USER appuser

# --- Runtime configuration ---
EXPOSE 8080

# --- Application startup ---
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=60.0", \
    "-XX:MaxMetaspaceSize=128m", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-XX:+UseG1GC", \
    "-XX:G1HeapRegionSize=4m", \
    "-XX:+UseStringDeduplication", \
    "-XX:SharedArchiveFile=app-cds.jsa", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "org.springframework.boot.loader.launch.JarLauncher"]