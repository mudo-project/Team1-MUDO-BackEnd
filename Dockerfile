# 1단계: JDK가 포함된 임시 빌드 이미지에서 Spring Boot 실행 JAR를 만든다.
# 이 단계는 최종 실행 이미지에 포함되지 않는다.
FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace

# Gradle Wrapper와 빌드 설정, 애플리케이션 소스를 빌드 컨테이너로 복사한다.
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle settings.gradle ./
COPY src ./src

# 이미지 빌드에서는 JAR 생성만 수행한다. 전체 테스트는 이미지 빌드 전에 별도로 실행한다.
RUN chmod +x gradlew \
    && ./gradlew bootJar -x test --no-daemon

# 2단계: JDK보다 작은 JRE 이미지에는 실행 결과물만 넣는다.
FROM eclipse-temurin:17-jre-jammy AS runtime

# curl은 컨테이너 헬스체크에 사용한다.
# 애플리케이션은 root가 아닌 UID 10001 사용자로 실행한다.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --home-dir /app app

WORKDIR /app

# 빌드 단계에서 생성한 JAR만 복사하여 소스와 빌드 도구가 실행 이미지에 남지 않게 한다.
COPY --from=build --chown=app:app /workspace/build/libs/*.jar app.jar

# 컨테이너 메모리의 최대 75%를 JVM 힙 등에 활용하고 문자 인코딩을 UTF-8로 고정한다.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Seoul"

USER app

# EXPOSE는 문서 역할이며 실제 호스트 포트 연결은 Compose의 ports에서 정한다.
EXPOSE 8080

# Spring Boot가 완전히 기동돼 Actuator가 응답하는지를 Docker가 확인한다.
HEALTHCHECK --interval=10s --timeout=5s --start-period=90s --retries=10 \
    CMD curl --fail --silent http://localhost:8080/actuator/health || exit 1

# 컨테이너가 시작될 때 Spring Boot JAR를 PID 1 프로세스로 실행한다.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
