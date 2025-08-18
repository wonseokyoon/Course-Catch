## --- Stage 1: 애플리케이션 빌드 단계 ---
## 소스 코드 컴파일
FROM openjdk:21-jdk-slim AS builder
## 컨테이너 내의 작업 디렉터리를 /app으로 설정
WORKDIR /app
# 모든 소스코드를 컨테이너의 /app 디렉터리로 복사
COPY . .
# Gradle Wrapper 스크립트에 실행 권한을 부여
RUN chmod +x ./gradlew
# Gradle을 사용하여 애플리케이션을 빌드
# - clean: 이전 빌드 결과물(build 폴더)을 모두 삭제하여 깨끗한 상태에서 빌드를 시작
# - build: 소스 코드를 컴파일하고 실행 가능한 JAR 파일을 생성
# - --no-daemon: CI/CD 환경에 적합하도록 Gradle 데몬을 사용하지 않고 일회성으로 빌드를 실행
# - -x test: CI 단계에서 이미 테스트를 수행했으므로, Docker 이미지를 만드는 과정에서는 테스트를 건너뜀
RUN ./gradlew clean build --no-daemon -x test


# --- Stage 2: 이미지 생성 ---
# JRE만 포함된 가벼운 이미지를 사용하여, 최소한의 파일만으로 이미지를 만듬
FROM eclipse-temurin:21-jre
# 컨테이너 내의 작업 디렉터리
WORKDIR /app
# 현재 이미지의 /app 디렉터리로 복사하고, 이름을 app.jar로 변경
COPY build/libs/*-SNAPSHOT.jar app.jar
# 8080 포트를 외부에 노출
EXPOSE 8080
# "java -jar app.jar" 명령어로 Spring Boot 애플리케이션을 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
