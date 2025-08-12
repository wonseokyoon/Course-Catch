# Dockerfile

# --- 1단계: 프로젝트 빌드 (요리하는 주방) ---
# Java 17과 Gradle을 사용할 수 있는 JDK 이미지를 'builder'라는 이름으로 사용
FROM openjdk:17-jdk-slim AS builder

# 작업 디렉토리 설정
WORKDIR /app

# 빌드에 필요한 파일들(build.gradle, gradlew, src 등)을 컨테이너에 복사
COPY . .

# Gradle을 사용해 프로젝트를 빌드합니다. (실행 권한 부여 후 빌드)
# 이 과정이 끝나면 build/libs/ 안에 .jar 파일이 생성됩니다.
RUN chmod +x ./gradlew
RUN ./gradlew build

# --- 2단계: 최종 이미지 생성 (손님에게 나갈 접시) ---
# Java를 실행만 할 수 있는 더 가벼운 JRE 이미지를 사용
FROM openjdk:17-jre-slim

# 작업 디렉토리 설정
WORKDIR /app

# 1단계(builder)에서 생성된 .jar 파일만 최종 이미지로 가져옵니다.
# 'app.jar' 라는 이름으로 복사됩니다.
COPY --from=builder /app/build/libs/*.jar app.jar

# 8080 포트를 외부에 노출
EXPOSE 8080

# 컨테이너가 시작될 때 "java -jar app.jar" 명령어를 실행하여 애플리케이션을 구동
ENTRYPOINT ["java", "-jar", "app.jar"]