# Stage 1: Build the application with Gradle in a clean, optimized environment
FROM openjdk:21-jdk-slim AS builder

WORKDIR /app

# 1. 빌드에 필요한 최소한의 파일만 먼저 복사하여 의존성 레이어를 분리합니다.
# 이렇게 하면 소스 코드가 변경되어도 매번 의존성을 새로 받지 않아 빌드 속도가 향상됩니다.
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# 2. 애플리케이션 의존성을 먼저 다운로드합니다.
RUN ./gradlew dependencies

# 3. 나머지 소스 코드를 복사합니다.
COPY src ./src

# 4. 'clean'을 추가하여 이전 빌드 결과를 삭제하고, 테스트를 제외한 후 새로 빌드합니다.
RUN ./gradlew clean build --no-daemon -x test

# Stage 2: Create the final, minimal, and secure image for deployment
FROM eclipse-temurin:21-jre

WORKDIR /app

# 보안 강화를 위해 root가 아닌 별도의 사용자를 생성하고 전환합니다.
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring

# 빌드 단계에서 생성된 실행 가능한 JAR 파일만 복사합니다.
COPY --from=builder /app/build/libs/*.jar app.jar

# Spring Boot의 기본 포트인 8080을 사용합니다.
EXPOSE 8080

# 로컬 환경과 동일하게 'dev' 프로필을 활성화하여 애플리케이션을 실행합니다.
ENTRYPOINT ["java", "-Dspring.profiles.active=dev", "-jar", "app.jar"]
