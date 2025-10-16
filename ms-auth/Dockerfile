# Multi-stage build for Auth Microservice
FROM eclipse-temurin:24-jdk as builder

# Set working directory
WORKDIR /app

# Copy gradle files
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Copy shared module
COPY shared shared

# Copy ms-auth source
COPY ms-auth ms-auth

# Make gradlew executable
RUN chmod +x gradlew

# Build the Auth microservice
RUN ./gradlew :ms-auth:build -x test

# Runtime stage
FROM eclipse-temurin:24-jre

# Set working directory
WORKDIR /app

# Copy the built jar
COPY --from=builder /app/ms-auth/build/libs/*.jar app.jar

# Expose port
EXPOSE 8082

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8082/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
