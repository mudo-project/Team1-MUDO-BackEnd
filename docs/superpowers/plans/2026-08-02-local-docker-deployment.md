# MUDO Groupware 로컬 Docker 배포 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `C:\lecture\module06\Team1-MUDO-BackEnd` (MUDO Groupware 백엔드)를 Windows Docker Desktop에서 `MySQL + Spring Boot 앱` 컨테이너로 로컬 실행하고, 헬스체크·Flyway·재시작·데이터 유지까지 검증한다.

**Architecture:** 참고 문서(`2026-08-02-local-docker-deployment-guide.md`)의 구조를 이 저장소의 실제 상태에 맞게 조정한다. 이 프로젝트는 Redis·Swagger 의존성이 없으므로 컨테이너는 `mysql` + `app` 2개만 구성한다. DB 접속 정보는 새 프로필 파일을 만들지 않고 Spring Boot의 환경변수 우선순위(OS 환경변수가 `application-{profile}.yaml`보다 우선)를 이용해 기존 `local` 프로필 위에 `SPRING_DATASOURCE_*` 환경변수로 덮어쓴다. JWT 시크릿은 `JwtProperties`의 relaxed binding(`JWT_SECRET` env → `jwt.secret` 프로퍼티)을 그대로 활용한다.

**Tech Stack:** Docker Desktop, Docker Compose v2, Spring Boot 4.1 (Java 17, Gradle 9.5 wrapper), MySQL 8.4, `eclipse-temurin` 17 JDK/JRE 이미지.

---

## 사전 조사로 확인한 사실 (계획에 반영된 이유)

- **저장소에 `Dockerfile`, `docker-compose*.yml`, `.env*` 파일이 전혀 없다** — 참고 가이드는 기존 파일이 있다고 가정하지만 이 저장소는 전부 새로 만들어야 한다.
- **`build.gradle`에 Redis, Swagger/springdoc 의존성이 없다** — 참고 가이드의 Redis 컨테이너·Swagger 검증 단계는 이 프로젝트에 적용되지 않는다. 이번 계획은 `mysql` + `app` 2개 컨테이너만 구성하고, 검증은 `/actuator/health`로만 한다.
- **`approval` 도메인 6개 파일이 존재하지 않는 `global.error.*` 패키지를 import해서 현재 컴파일이 실패한다** — 이 상태로는 `./gradlew bootJar`가 실패해 Docker 이미지 빌드 자체가 안 되므로, Task 1에서 이 컴파일 오류부터 최소 수정으로 고친다(설계 변경이 아니라 잘못된 import 경로/메서드명 정정).
- **`application-prod.yaml`은 `${DB_URL}`/`${DB_USERNAME}`/`${DB_PASSWORD}` 커스텀 플레이스홀더를 쓰지만, `application-local.yaml`은 하드코딩된 값이라 커스텀 플레이스홀더가 없다** — 새 프로필 파일을 추가하는 대신 Spring Boot 표준 환경변수 `SPRING_DATASOURCE_URL`/`SPRING_DATASOURCE_USERNAME`/`SPRING_DATASOURCE_PASSWORD`로 `local` 프로필의 값을 덮어쓴다(OS 환경변수가 profile-specific yaml보다 우선순위가 높음). 이러면 `logback-spring.xml`의 `local,default` 프로필 블록(콘솔+파일 로그)도 그대로 유지된다.
- **`JwtProperties`는 `@ConfigurationProperties(prefix = "jwt")`이고 어떤 yaml에도 `jwt.secret`이 없다** — 기본값 `"local-development-only-change-this-secret-key"`가 쓰이고 있었다. `JWT_SECRET` 환경변수를 relaxed binding으로 주입하면 별도 yaml 수정 없이 해결된다.
- **`S3Properties`/`S3Config`는 `AWS_REGION`/`AWS_S3_BUCKET_NAME` 기본값이 있고 `S3Client` 자격증명은 지연 해석(lazy)이라 로컬 기동·헬스체크에는 AWS 관련 환경변수가 필요 없다** — 이번 계획에서는 설정하지 않는다(실제 S3 업로드를 로컬에서 테스트하려면 별도 작업으로 분리).
- **`eclipse-temurin` JRE 베이스 이미지에는 기본적으로 `curl`/`wget`이 없다** — 앱 컨테이너 헬스체크가 동작하려면 런타임 스테이지에 `curl`을 설치해야 한다(참고 가이드 14-7절이 예고한 문제).

---

## Task 1: `approval` 모듈 컴파일 오류 수정 (Docker 빌드의 전제 조건)

**Files:**
- Modify: `src/main/java/com/academy/mudogroupware/approval/domain/model/ApprovalErrorCode.java`
- Modify: `src/main/java/com/academy/mudogroupware/approval/domain/model/ApprovalTemplate.java:8`
- Modify: `src/main/java/com/academy/mudogroupware/approval/domain/model/ApprovalContent.java:3`
- Modify: `src/main/java/com/academy/mudogroupware/approval/domain/model/ApprovalLine.java:5`
- Modify: `src/main/java/com/academy/mudogroupware/approval/application/service/DecideApprovalLineService.java:11`
- Modify: `src/main/java/com/academy/mudogroupware/approval/application/service/ApprovalQueryService.java:19`

- [ ] **Step 1: `ApprovalErrorCode.java`의 잘못된 import와 메서드명을 고친다**

`src/main/java/com/academy/mudogroupware/approval/domain/model/ApprovalErrorCode.java`에서:

```java
import com.academy.mudogroupware.global.error.ErrorCode;
```
→
```java
import com.academy.mudogroupware.global.domain.common.exception.ErrorCode;
```

그리고 실제 `ErrorCode` 인터페이스는 `getHttpStatus()`를 요구하므로(파일: `src/main/java/com/academy/mudogroupware/global/domain/common/exception/ErrorCode.java:8`):

```java
    @Override
    public HttpStatus getStatus() {
        return status;
    }
```
→
```java
    @Override
    public HttpStatus getHttpStatus() {
        return status;
    }
```

- [ ] **Step 2: 나머지 5개 파일의 `global.error.BusinessException` import를 고친다**

아래 5개 파일에서 동일한 한 줄을 각각 고친다:
- `ApprovalTemplate.java:8`
- `ApprovalContent.java:3`
- `ApprovalLine.java:5`
- `DecideApprovalLineService.java:11`
- `ApprovalQueryService.java:19`

```java
import com.academy.mudogroupware.global.error.BusinessException;
```
→
```java
import com.academy.mudogroupware.global.domain.common.exception.BusinessException;
```

- [ ] **Step 3: 컴파일 확인**

```powershell
.\gradlew.bat compileJava
```

Expected: `BUILD SUCCESSFUL` (더 이상 `package com.academy.mudogroupware.global.error does not exist` 오류가 없어야 한다).

- [ ] **Step 4: 커밋**

```powershell
git add src/main/java/com/academy/mudogroupware/approval
git commit -m "fix: approval 모듈의 존재하지 않는 global.error 패키지 참조 수정"
```

---

## Task 2: `Dockerfile` 및 `.dockerignore` 작성

**Files:**
- Create: `Dockerfile`
- Create: `.dockerignore`

- [ ] **Step 1: `.dockerignore` 작성**

```gitignore
.git
.gradle
.idea
build
logs
uploads
*.md
docs
```

- [ ] **Step 2: `Dockerfile` 작성 (멀티스테이지: Gradle 빌드 → JRE 실행)**

```dockerfile
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle settings.gradle ./
COPY src ./src
RUN chmod +x gradlew && ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

- [ ] **Step 3: 이미지 단독 빌드로 검증 (Compose 이전에 Dockerfile 자체가 되는지 먼저 확인)**

```powershell
docker build -t mudo-groupware-backend:local .
```

Expected: 마지막 줄이 `writing image sha256:...` 이후 `naming to docker.io/library/mudo-groupware-backend:local done`으로 끝난다. Gradle 컴파일 오류가 나면 Task 1이 제대로 반영됐는지 다시 확인한다.

- [ ] **Step 4: 커밋**

```powershell
git add Dockerfile .dockerignore
git commit -m "add: 로컬/운영 공용 Dockerfile 작성"
```

---

## Task 3: `.env.local` / `.env.local.example` 작성 및 `.gitignore` 갱신

**Files:**
- Create: `.env.local` (Git에 커밋되지 않음)
- Create: `.env.local.example`
- Modify: `.gitignore`

- [ ] **Step 1: `.gitignore`에 로컬 환경변수 파일 제외 규칙 추가**

`.gitignore` 끝에 추가:

```gitignore

### Local Docker ###
.env.local
```

- [ ] **Step 2: JWT 시크릿 생성**

```powershell
$jwtBytes = New-Object byte[] 64
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($jwtBytes)
$rng.Dispose()
[Convert]::ToBase64String($jwtBytes)
```

출력된 Base64 문자열을 다음 단계의 `JWT_SECRET` 값으로 사용한다.

- [ ] **Step 3: `.env.local` 작성**

저장소 최상위에 `.env.local`을 만들고(위에서 생성한 문자열로 `JWT_SECRET`을 반드시 교체):

```dotenv
# MySQL 컨테이너 초기화용 (로컬 전용, 운영 값과 무관)
LOCAL_MYSQL_ROOT_PASSWORD=local-root-password-2026
DB_USERNAME=mudo_app
DB_PASSWORD=local-db-password-2026

# Spring Boot — JwtProperties.secret 에 relaxed binding으로 주입됨
JWT_SECRET=<위에서_생성한_Base64_문자열>
```

- [ ] **Step 4: `.env.local.example` 작성 (Git에 커밋되는 템플릿, 실제 값 없음)**

```dotenv
LOCAL_MYSQL_ROOT_PASSWORD=changeme
DB_USERNAME=mudo_app
DB_PASSWORD=changeme
JWT_SECRET=<generate-with-powershell-see-plan-task-3-step-2>
```

- [ ] **Step 5: `.env.local`이 Git에 잡히지 않는지 확인**

```powershell
git status --short
git check-ignore -v .env.local
```

Expected: `git check-ignore -v .env.local`이 `.gitignore:<line>:.env.local	.env.local`처럼 규칙을 출력한다(즉 무시되고 있음). `git status --short`에는 `.env.local`이 나타나면 안 된다.

- [ ] **Step 6: 커밋 (`.env.local`은 절대 포함하지 않음)**

```powershell
git add .gitignore .env.local.example
git commit -m "add: 로컬 Docker 환경변수 템플릿 및 gitignore 규칙 추가"
```

---

## Task 4: `docker-compose.local.yml` 작성

**Files:**
- Create: `docker-compose.local.yml`

- [ ] **Step 1: 파일 작성**

```yaml
name: mudo-groupware-local

services:
  mysql:
    image: mysql:8.4
    container_name: mudo-mysql-local
    environment:
      MYSQL_ROOT_PASSWORD: ${LOCAL_MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: mudo_groupware
      MYSQL_USER: ${DB_USERNAME}
      MYSQL_PASSWORD: ${DB_PASSWORD}
      TZ: Asia/Seoul
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
    ports:
      - "3306:3306"
    volumes:
      - mudo-mysql-data:/var/lib/mysql
    healthcheck:
      test: ["CMD-SHELL", "mysqladmin ping -h 127.0.0.1 -uroot -p$$MYSQL_ROOT_PASSWORD --silent"]
      interval: 5s
      timeout: 5s
      retries: 30
      start_period: 30s
    restart: unless-stopped
    networks:
      - mudo-local-network

  app:
    build:
      context: .
      dockerfile: Dockerfile
    image: mudo-groupware-backend:local
    container_name: mudo-app-local
    env_file:
      - .env.local
    environment:
      SPRING_PROFILES_ACTIVE: local
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/mudo_groupware?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
      SPRING_DATASOURCE_USERNAME: ${DB_USERNAME}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
    ports:
      - "8080:8080"
    volumes:
      - ./logs:/app/logs
    depends_on:
      mysql:
        condition: service_healthy
    healthcheck:
      test: ["CMD-SHELL", "curl -sf http://localhost:8080/actuator/health | grep -q '\"status\":\"UP\"'"]
      interval: 10s
      timeout: 5s
      retries: 30
      start_period: 90s
    restart: "no"
    networks:
      - mudo-local-network

volumes:
  mudo-mysql-data:

networks:
  mudo-local-network:
    driver: bridge
```

- [ ] **Step 2: Compose 문법 검증**

```powershell
docker compose --env-file .env.local -f docker-compose.local.yml config --quiet
docker compose --env-file .env.local -f docker-compose.local.yml config --services
```

Expected: 첫 명령은 아무 출력 없이 종료, 두 번째 명령은 `mysql`과 `app` 두 줄만 출력한다.

- [ ] **Step 3: 커밋**

```powershell
git add docker-compose.local.yml
git commit -m "add: 로컬 전용 docker-compose 파일 작성"
```

---

## Task 5: 전체 기동 및 기능 검증

**Files:** 없음 (검증 전용 태스크)

- [ ] **Step 1: 포트 사용 여부 확인**

```powershell
Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
  Where-Object LocalPort -In 3306, 8080 |
  Select-Object LocalAddress, LocalPort, OwningProcess
```

Expected: 아무 것도 안 나오거나(사용 가능), 나온다면 해당 프로세스를 종료하거나 `docker-compose.local.yml`의 왼쪽 포트를 바꾼다.

- [ ] **Step 2: 이미지 빌드 후 전체 기동**

```powershell
docker compose --env-file .env.local -f docker-compose.local.yml up -d --build
docker compose --env-file .env.local -f docker-compose.local.yml ps
```

Expected: `mysql`, `app` 모두 `running`이고 잠시 후 `healthy` 상태가 된다.

- [ ] **Step 3: 헬스체크**

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

Expected: `status: UP`.

- [ ] **Step 4: Flyway 마이그레이션 확인**

```powershell
docker exec mudo-mysql-local mysql `
  -umudo_app `
  -plocal-db-password-2026 `
  -e "USE mudo_groupware; SHOW TABLES; SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

Expected: `users`, `refresh_tokens`, `flyway_schema_history` 테이블이 보이고, `V1.1.1`/`V1.2.1` 두 행 모두 `success = 1`.

- [ ] **Step 5: 앱 컨테이너 재시작 검증**

```powershell
docker compose --env-file .env.local -f docker-compose.local.yml restart app
docker compose --env-file .env.local -f docker-compose.local.yml logs --tail 100 app
Invoke-RestMethod http://localhost:8080/actuator/health
```

Expected: 재시작 후에도 `status: UP`.

- [ ] **Step 6: 데이터 유지 검증 (컨테이너 내리고 다시 올리기)**

```powershell
docker compose --env-file .env.local -f docker-compose.local.yml down
docker compose --env-file .env.local -f docker-compose.local.yml up -d
Invoke-RestMethod http://localhost:8080/actuator/health
docker exec mudo-mysql-local mysql -umudo_app -plocal-db-password-2026 -e "SELECT COUNT(*) FROM mudo_groupware.flyway_schema_history;"
```

Expected: 재기동 후에도 `flyway_schema_history` 행 수가 그대로 유지된다(`down`만 실행하면 `mudo-mysql-data` 볼륨은 삭제되지 않으므로).

- [ ] **Step 7: (선택) 완전 초기화 재현 절차 한 번 검증**

```powershell
docker compose --env-file .env.local -f docker-compose.local.yml down -v
docker compose --env-file .env.local -f docker-compose.local.yml up -d --build
Invoke-RestMethod http://localhost:8080/actuator/health
```

> **주의:** `down -v`는 `mudo-mysql-data` 볼륨을 삭제한다. 지금 단계는 로컬 테스트 데이터뿐이므로 안전하지만, 이후 로컬에 의미 있는 데이터를 쌓았다면 이 단계는 건너뛴다.

---

## Self-Review 체크리스트 (계획 작성자용, 실행 전 확인)

- [x] 참고 가이드의 "오늘 완료했다고 볼 수 있는 조건" 8개 항목이 모두 Task 5에 매핑되는가 — MySQL/앱 컨테이너 기동(Step 2), 헬스체크(Step 3), Flyway(Step 4), 재시작(Step 5), 데이터 유지(Step 6), `.env.local` 미추적(Task 3 Step 5), 전체 재빌드 재현(Step 7). Swagger 항목만 이 프로젝트에 springdoc 의존성이 없어 제외 — Task 상단 "사전 조사" 절에 이유 명시.
- [x] 플레이스홀더("TBD" 등) 없음 — 전 단계에 실제 명령/코드 포함.
- [x] 타입/네이밍 일관성 — 컨테이너명(`mudo-mysql-local`, `mudo-app-local`), DB명(`mudo_groupware`), 계정명(`mudo_app`)이 Task 3~5에서 동일하게 사용됨.
