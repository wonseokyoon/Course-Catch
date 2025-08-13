# Stage 1: Build the application using JDK
FROM openjdk:21-jdk-slim AS builder

WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
# gradlew 빌드 시 발생하는 불필요한 로그를 줄이고 빌드 속도를 개선하기 위해 --no-daemon 옵션을 추가할 수 있습니다.
RUN ./gradlew build --no-daemon

# Stage 2: Create the final image using JRE
FROM eclipse-temurin:21-jre

WORKDIR /app
# 빌드된 jar 파일만 복사합니다.
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]