# Multi-stage build for API Gateway
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

# Copy api-gateway source
COPY api-gateway api-gateway

# Make gradlew executable
RUN chmod +x gradlew

# Build the API Gateway
RUN ./gradlew :api-gateway:build -x test

# Runtime stage
FROM eclipse-temurin:24-jre

# Set working directory
WORKDIR /app

# Copy the built jar
COPY --from=builder /app/api-gateway/build/libs/*.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
