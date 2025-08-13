# Stage 1: Build the application with Gradle in a clean environment
FROM openjdk:21-jdk-slim AS builder

WORKDIR /app

# 1. 빌드에 필요한 최소한의 파일만 복사하여 의존성 레이어를 분리합니다.
# 이렇게 하면 소스 코드가 변경되어도 매번 의존성을 새로 받지 않아 빌드 속도가 향상됩니다.
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# 2. 애플리케이션 의존성을 먼저 다운로드합니다.
RUN ./gradlew dependencies

# 3. 나머지 소스 코드를 복사합니다.
COPY src ./src

# 4. CI 단계에서 이미 테스트를 수행했으므로, 테스트를 제외하고 애플리케이션을 빌드합니다.
RUN ./gradlew build --no-daemon -x test

# Stage 2: Create the final, minimal image for deployment
FROM eclipse-temurin:21-jre

WORKDIR /app

# 애플리케이션이 80번 포트에서 실행되도록 환경 변수를 설정합니다.
ENV SERVER_PORT=80
# 변경된 포트를 외부에 노출합니다.
EXPOSE 80

# 빌드 단계에서 생성된 실행 가능한 JAR 파일만 복사합니다.
COPY --from=builder /app/build/libs/*.jar app.jar

# 컨테이너 실행 시 Spring Profile을 'dev'로 지정하여 환경을 통일합니다.
# 만약 다른 프로필(e.g., prod)을 사용하신다면 'dev'를 해당 프로필명으로 변경해주세요.
ENTRYPOINT ["java", "-Dspring.profiles.active=dev", "-jar", "app.jar"]
