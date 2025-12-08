# ==========================================
# STAGE 1: Build the Application
# ==========================================
# FIX: Use the JDK image directly since 'maven:3.9-eclipse-temurin-25' doesn't exist yet
FROM eclipse-temurin:25-jdk-alpine AS build

# Install Maven manually (Alpine Linux)
RUN apk add --no-cache maven

WORKDIR /app

# Copy config files first to leverage Docker cache
COPY pom.xml .
# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
# Skip tests to speed up the container build (tests run in CI anyway)
RUN mvn clean package -DskipTests

# ==========================================
# STAGE 2: The Final Image
# ==========================================
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Create a non-root user for security
RUN addgroup -S ai2mq && adduser -S ai2mq -G ai2mq
USER ai2mq

# Copy only the JAR from the build stage
COPY --from=build /app/target/ai2mqtt-1.0.0-SNAPSHOT.jar app.jar

# Create volume for database persistence
VOLUME /app/data

# Default Environment Variables
ENV MESSAGING_TYPE=mqtt
ENV MQTT_BROKER_URL=tcp://core-mosquitto:1883
ENV JAVA_OPTS=""

# Expose the dashboard port
EXPOSE 8080

# Run it
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
