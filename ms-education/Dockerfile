FROM eclipse-temurin:24-jdk AS builder
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew :ms-education:bootJar --no-daemon

FROM eclipse-temurin:24-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=builder /app/ms-education/build/libs/*.jar app.jar
EXPOSE 8083
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:${EDUCATION_PORT:-8083}/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
