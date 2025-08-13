# Stage 1: Build the application
FROM openjdk:21-jdk-slim AS builder
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew clean build --no-daemon -x test

# Stage 2: Create the final image
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=builder /app/build/libs/*-SNAPSHOT.jar app.jar

EXPOSE 8080

# server.address=0.0.0.0 를 추가하여 컨테이너 외부의 모든 연결을 허용하도록 강제합니다.
# 이것이 ERR_CONNECTION_REFUSED 문제를 해결할 핵심적인 수정입니다.
ENTRYPOINT ["java", "-Dserver.address=0.0.0.0", "-jar", "app.jar"]
