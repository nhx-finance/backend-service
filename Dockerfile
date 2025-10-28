# Multi-stage Dockerfile for nhxserver
# Build stage: uses Maven wrapper if present to build the fat jar
FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /workspace

# Cache Maven wrapper and settings if present
COPY .mvn .mvn
COPY mvnw mvnw
COPY pom.xml pom.xml
COPY settings.xml settings.xml

# Copy source and build
COPY src ./src
RUN if [ -f mvnw ]; then chmod +x mvnw && ./mvnw -B -DskipTests package; else mvn -B -DskipTests package; fi

# Runtime stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy jar produced by the build stage
COPY --from=build /workspace/target/*-*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
# Build stage
FROM maven:3.9.5-eclipse-temurin-17 AS builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-alpine
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
ENV PORT=8080

# Expose the application port
EXPOSE ${PORT}

# Start the application
CMD ["java", "-jar", "app.jar"]