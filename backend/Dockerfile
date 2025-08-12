# Dockerfile

FROM openjdk:21-jdk-slim AS builder

WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew build

FROM eclipse-temurin:21-jre-focal

WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]