# Stage 1: Build the application
FROM openjdk:21-jdk-slim AS builder
WORKDIR /app

# Build context를 복사합니다.
COPY . .

# Gradle 실행 권한을 부여하고, 모든 소스 코드를 사용하여 빌드합니다.
RUN chmod +x ./gradlew
RUN ./gradlew clean build --no-daemon -x test

# Stage 2: Create the final runnable image
FROM eclipse-temurin:21-jre
WORKDIR /app

# 빌드 단계에서 생성된 최종 JAR 파일만 복사합니다.
COPY --from=builder /app/build/libs/*-SNAPSHOT.jar app.jar

# Spring Boot의 기본 포트인 8080을 노출합니다.
EXPOSE 8080

# 애플리케이션을 실행합니다.
ENTRYPOINT ["java", "-jar", "app.jar"]
