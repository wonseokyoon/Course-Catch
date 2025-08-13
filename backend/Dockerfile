# Stage 1: Build the application using JDK
FROM openjdk:21-jdk-slim AS builder

WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
# CI 단계에서 이미 테스트를 수행했으므로, Docker 빌드 시에는 -x test 옵션으로 테스트를 건너뜁니다.
RUN ./gradlew build --no-daemon -x test

# Stage 2: Create the final image using JRE
FROM eclipse-temurin:21-jre

WORKDIR /app
# 빌드된 jar 파일만 복사합니다.
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
