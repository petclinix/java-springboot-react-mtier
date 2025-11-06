# ---------- Builder ----------
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Helpful tools for wrappers
RUN apk add --no-cache bash

# ---- Maven (cache deps) ----
# Copy only Maven files first to leverage layer caching
COPY pom.xml mvnw ./
COPY .mvn .mvn
# Go offline if Maven wrapper exists
RUN chmod +x mvnw && ./mvnw -q -DskipTests dependency:go-offline

# Now add sources and do the real build
COPY src ./src

# Build using whichever wrapper exists (Maven preferred if both present)
RUN set -eux; \
    ./mvnw -DskipTests -P mariadb package; \
    # pick first non-plain jar (handles Gradle's *-plain.jar)
    JAR="$(find target -name 'backend*.jar' | head -n 1)"; \
    test -n "$JAR"; \
    cp "$JAR" /workspace/petclinix.jar

# ---------- Production ----------
FROM eclipse-temurin:21-jre-alpine AS production
ENV APP_HOME=/opt/app
WORKDIR $APP_HOME

# Copy runnable jar
COPY --from=builder /workspace/petclinix.jar ./petclinix.jar

# Non-root user
RUN addgroup -S petclinix && adduser -S petclinix -G petclinix
USER petclinix:petclinix

# Tune JVM for containers (override via JAVA_OPTS)
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75 -XX:InitialRAMPercentage=50 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

# Healthcheck (expects Spring Actuator at /actuator/health)
# Adjust or remove if you don't use actuator
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-lc", "/opt/java/openjdk/bin/java $JAVA_OPTS -Dspring.profiles.active=prod -jar petclinix.jar"]
