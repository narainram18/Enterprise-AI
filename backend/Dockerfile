# Build stage
FROM maven:3.9.6-eclipse-temurin-21-jammy AS build
WORKDIR /app
COPY pom.xml .
# Download dependencies
RUN mvn dependency:go-offline -B
COPY src ./src
# Build the application
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Create non-root user
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

# Create directories for data and logs
RUN mkdir -p /app/data/documents /app/logs && \
    chown -R appuser:appgroup /app/data /app/logs

# Copy the jar file
COPY --from=build --chown=appuser:appgroup /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

# Run as non-root user
USER appuser

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
