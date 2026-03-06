# =============================================================================
# BUILD STAGE
# =============================================================================
FROM maven:3.9-eclipse-temurin-25-alpine AS build

ARG MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1"

WORKDIR /app
COPY pom.xml ./
COPY src ./src
RUN mvn package -DskipTests -B --no-transfer-progress

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

LABEL maintainer="Zufar Sunagatov" \
      description="Festiva — Telegram birthday reminder bot"

WORKDIR /app

COPY --from=extract /app/dependencies/ ./
COPY --from=extract /app/spring-boot-loader/ ./
COPY --from=extract /app/snapshot-dependencies/ ./
COPY --from=extract /app/application/ ./
COPY --from=cds-train /app/app-cds.jsa ./

EXPOSE 8080

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
