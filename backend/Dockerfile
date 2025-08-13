# Stage 1: Build the application
FROM openjdk:21-jdk-slim AS builder
WORKDIR /app

# 1. 모든 파일을 복사한 직후, 파일 목록을 출력하여 src 폴더가 있는지 확인합니다.
COPY . .
RUN echo "--- 1. 'COPY . .' 실행 후 /app 디렉터리 내용물 ---" && ls -la

RUN chmod +x ./gradlew
RUN ./gradlew clean build --no-daemon -x test

# 2. 빌드가 완료된 후, 생성된 JAR 파일이 있는지 확인합니다.
RUN echo "--- 2. 빌드 완료 후 /app/build/libs 디렉터리 내용물 ---" && ls -la /app/build/libs

# Stage 2: Create the final image
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=builder /app/build/libs/*-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
