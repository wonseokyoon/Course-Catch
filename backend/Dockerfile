# Stage 1: Build the application
FROM openjdk:21-jdk-slim AS builder
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew clean build --no-daemon -x test

# Stage 2: Final image for diagnostics
FROM eclipse-temurin:21-jre
WORKDIR /app

# Install tools needed for inspection
RUN apt-get update && apt-get install -y unzip

# Copy the built JAR file from the builder stage
COPY --from=builder /app/build/libs/*-SNAPSHOT.jar app.jar

# This entrypoint will NOT start the application.
# Instead, it will list all files inside the JAR file and then exit.
ENTRYPOINT ["unzip", "-l", "app.jar"]
