# Multi-stage Dockerfile for nhxserver with dependency caching
FROM maven:3.9-eclipse-temurin-21 AS dependencies
WORKDIR /app

# Copy only pom.xml first to cache dependencies
COPY pom.xml .

# Download all dependencies with retry logic
RUN mvn dependency:go-offline -B || \
    mvn dependency:resolve -B || \
    mvn dependency:resolve-plugins -B

# Stage 2: Build the application
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy the cached dependencies from previous stage
COPY --from=dependencies /root/.m2 /root/.m2

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# Stage 3: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create a non-root user
RUN addgroup --system javauser && adduser -S -s /bin/false -G javauser javauser

# Copy the jar from the build stage
COPY --from=builder /app/target/*.jar app.jar

# Set ownership
RUN chown -R javauser:javauser /app

# Use non-root user
USER javauser

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=prod
ENV PORT=8084

# Expose the application port
EXPOSE ${PORT}

# Health check (optional but recommended)
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT}/actuator/health || exit 1

# Start the application with optimized JVM settings
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]