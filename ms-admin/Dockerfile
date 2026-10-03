FROM eclipse-temurin:24-jdk AS builder
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew :ms-admin:bootJar --no-daemon

FROM eclipse-temurin:24-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=builder /app/ms-admin/build/libs/*.jar app.jar
EXPOSE 8082
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:${ADMIN_PORT:-8082}/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
