# Stage 1: Build the application
# 표준 OpenJDK 이미지를 사용하여 빌드 환경의 변수를 최소화합니다.
FROM openjdk:21-jdk-slim AS builder
WORKDIR /app

# 요청하신 대로, backend 폴더의 모든 내용을 한 번에 복사합니다.
COPY . .

# 복사된 파일 목록을 출력하여 src 폴더와 index.html이 포함되었는지 확인합니다.
RUN ls -laR

# Gradle 래퍼에 실행 권한을 부여하고, 모든 소스 코드를 사용하여 빌드합니다.
RUN chmod +x ./gradlew
RUN ./gradlew clean build --no-daemon -x test

# Stage 2: Create the final image
FROM eclipse-temurin:21-jre
WORKDIR /app

# 빌드 단계에서 생성된 최종 JAR 파일만 복사합니다.
COPY --from=builder /app/build/libs/*-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
