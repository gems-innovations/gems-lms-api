FROM eclipse-temurin:24-jdk AS builder
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew :ms-auth:bootJar --no-daemon

FROM eclipse-temurin:24-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=builder /app/ms-auth/build/libs/*.jar app.jar
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:${AUTH_PORT:-8081}/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
