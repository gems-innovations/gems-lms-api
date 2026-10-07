FROM eclipse-temurin:24-jdk AS builder
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew :api-gateway:bootJar --no-daemon

FROM eclipse-temurin:24-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=builder /app/api-gateway/build/libs/*.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:${GATEWAY_PORT:-8080}/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
