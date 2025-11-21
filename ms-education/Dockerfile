# Multi-stage build for Education Microservice
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

# Copy ms-education source
COPY ms-education ms-education

# Make gradlew executable
RUN chmod +x gradlew

# Build the Education microservice
RUN ./gradlew :ms-education:build -x test

# Runtime stage
FROM eclipse-temurin:24-jre

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy the built jar
COPY --from=builder /app/ms-education/build/libs/*.jar app.jar

# Expose port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
