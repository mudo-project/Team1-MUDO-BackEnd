# 배포 파이프라인 (CI/CD)

## 1. 개요

운영 배포는 `.github/workflows/deploy-production.yml`(Deploy production to Amazon ECS)이
담당한다. `main` 브랜치 push 또는 `workflow_dispatch`(deploy 입력 true)로 트리거되며 두
job으로 구성된다.

```text
validate (≤10분)
  └─ infra/scripts/deploy_production.py --validate-only 로 배포 매니페스트 검증

deploy (≤60분, validate 통과 후 실행)
  1) ./gradlew test bootJar --no-daemon        ← 테스트 + 애플리케이션 JAR 생성
  2) 앱 이미지 빌드·푸시 (Dockerfile.ci)         ← 1)에서 만든 JAR를 그대로 사용
  3) 마이그레이션 이미지 빌드·푸시 (Dockerfile.migration)
  4) infra/scripts/deploy_production.py 로 마이그레이션 실행 + 학원별 순차 ECS 배포
```

각 이미지는 같은 Git SHA(`${{ github.sha }}`, `migration-${{ github.sha }}`)로 태깅되어
ECR에 저장된다. 이미지가 이미 ECR에 있으면(`Check reusable ECR images` 스텝) 재빌드를
건너뛴다.

## 2. Dockerfile 구성

레포 루트에 목적이 다른 Dockerfile 세 개가 있다.

| 파일 | 용도 | 비고 |
|---|---|---|
| `Dockerfile` | 로컬 개발용 애플리케이션 이미지 | JDK 빌드 스테이지 포함 멀티스테이지. `docker compose` 등 로컬 실행에 사용 |
| `Dockerfile.ci` | 운영 배포 전용 애플리케이션 이미지 | Gradle 빌드 없이 `build/libs/*.jar`만 복사. deploy job에서만 사용 |
| `Dockerfile.migration` | Flyway 전용 마이그레이션 이미지 | MySQL Connector/J를 Maven Central에서 직접 받아 앱을 컴파일하지 않는다 |

### 2.1 `Dockerfile.ci`가 별도로 존재하는 이유

deploy job은 이미지 빌드 전에 이미 `./gradlew test bootJar`로 JAR를 만든다. 기존
`Dockerfile`(로컬 개발용)을 그대로 쓰면 컨테이너 안에서 Gradle이 처음부터 다시 컴파일해
같은 소스를 두 번 빌드하게 된다. `Dockerfile.ci`는 이 중복을 없애고 이미 만든 JAR를
`COPY`만 한다.

`build/libs`는 루트 `.dockerignore`가 `build`를 통째로 제외하므로, 기본 빌드 컨텍스트에는
포함되지 않는다. `.dockerignore`를 수정하는 대신 `docker/build-push-action`의
`build-contexts` 입력으로 `build/libs`를 별도 컨텍스트(`prebuilt-jar`)로 넘기고
`Dockerfile.ci`에서 `COPY --from=prebuilt-jar`로 받는다.

```yaml
# .github/workflows/deploy-production.yml (앱 이미지 빌드 스텝 일부)
with:
  context: .
  file: Dockerfile.ci
  build-contexts: |
    prebuilt-jar=build/libs
```

로컬 개발용 `Dockerfile`은 그대로 유지되며, 운영 배포 워크플로에서만 `Dockerfile.ci`를
사용한다.

### 2.2 `Dockerfile.migration`의 MySQL Connector/J 처리

`Dockerfile.migration`은 Flyway가 MySQL에 접속할 드라이버 jar 하나만 필요하다. 이전에는
애플리케이션 전체를 `./gradlew bootJar`로 컴파일한 뒤 그 결과물에서
`mysql-connector-j-9.7.0.jar`를 추출했다. 드라이버 하나를 얻으려고 전체 소스를 컴파일하는
비용이 커서, `alpine` 스테이지에서 Maven Central의 동일 버전 jar를 직접 받아 sha256
체크섬으로 검증하는 방식으로 바꿨다.

```dockerfile
FROM alpine:3.20 AS dependency-download
RUN apk add --no-cache curl coreutils
WORKDIR /workspace
RUN curl -fsSL -o mysql-connector-j-9.7.0.jar \
        https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/9.7.0/mysql-connector-j-9.7.0.jar \
    && echo "0353648eaa1c91e0f4020c959abf756bc866ffd583df22ae6b6f6e0cbd43eb44  mysql-connector-j-9.7.0.jar" \
        > mysql-connector-j-9.7.0.jar.sha256 \
    && sha256sum -c mysql-connector-j-9.7.0.jar.sha256
```

버전(9.7.0)은 `build.gradle`의 `runtimeOnly 'com.mysql:mysql-connector-j'`를 Spring Boot
BOM(`org.springframework.boot` 플러그인 3.5.14)이 관리해 결정된다. 별도 버전 카탈로그
파일은 없으므로, Spring Boot 버전을 올려 BOM이 관리하는 MySQL Connector/J 버전이 바뀌면
`Dockerfile.migration`의 다운로드 URL·파일명·체크섬을 함께 갱신해야 한다.

## 3. CI 소요 시간 최적화 (2026-08-15)

### 3.1 배경

운영 배포(ECS)에 30~40분이 걸려 원인을 진단했다. 학원별 순차 배포는 원인이 아니었다
(`infra/tenants.yml`에 활성 테넌트가 `academy-a` 1개뿐). 실제 병목은 같은 소스를 세 번
컴파일하는 구조였다.

```text
① deploy job의 ./gradlew clean test bootJar   ← 전체 테스트 + 컴파일 (clean으로 캐시 무효화)
② Dockerfile(앱 이미지)가 컨테이너 안에서 gradle bootJar 재실행 ← 처음부터 다시 컴파일
③ Dockerfile.migration이 드라이버 jar 하나 때문에 앱 전체를 또 컴파일
```

### 3.2 적용한 변경

1. **`Dockerfile.migration`**: Gradle 컴파일 제거, Maven Central 직접 다운로드 +
   sha256 체크섬 검증으로 대체 (§2.2)
2. **`Dockerfile.ci` 신규 도입**: 앱 이미지 빌드에서 Gradle 재컴파일 제거, deploy job이
   만든 JAR를 `build-contexts`로 재사용 (§2.1)
3. **`clean` 제거**: `./gradlew clean test bootJar` → `./gradlew test bootJar`.
   `clean`이 `actions/setup-java`의 `cache: gradle` 증분 빌드 이점을 매번 무효화하고
   있었다. 테스트 정확성에는 `clean`이 필요 없다.

### 3.3 적용하지 않은 항목

- **`validate` job의 중복 python 검증**: `deploy` job의 "Build and test application"
  스텝이 `deploy_production.py --validate-only`를 다시 실행해 `validate` job과
  겹친다. 운영 배포 전 이중 안전장치로 의미가 있어 이번에는 유지했다.
- **앱/마이그레이션 이미지 병렬 빌드**: 같은 job의 연속된 step이라 순차 실행된다. 위
  변경으로 각 빌드 자체가 짧아져 우선순위를 낮췄다.

### 3.4 검증 상태

- MySQL Connector/J 체크섬은 Maven Central에서 직접 내려받아 재계산해 확인했다.
- `sha256sum -c` 문법은 로컬에서 동일 커맨드로 확인했다.
- `docker/build-push-action`의 `build-contexts` 동작과 전체 워크플로는 로컬에 Docker
  daemon이 없어 실측하지 못했다. 다음 운영 배포(`main` push) 실행 시 Actions 로그로
  확인이 필요하다.
