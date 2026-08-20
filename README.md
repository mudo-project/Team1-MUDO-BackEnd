<div align="center">

<img src="docs/assets/ieum-logo.png" alt="IEUM 로고" width="320" />

# 이음 (IEUM)

**흩어진 학원 업무 툴을 하나로 통합하고, 직관적인 UI와 AI 기능으로 더 편한 업무 환경을 만드는 학원 그룹웨어**

[![Java](https://img.shields.io/badge/Java-17-007396?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.14-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.4-4479A1?logo=mysql&logoColor=white)](https://www.mysql.com/)
[![AWS](https://img.shields.io/badge/AWS-ECS%20on%20EC2-FF9900?logo=amazonaws&logoColor=white)](https://aws.amazon.com/ecs/)

**[서비스 바로가기 → https://ieum.store](https://ieum.store/)**

`2026.07.28 ~ 2026.08.20` · 백엔드 6인 팀 프로젝트

</div>

---

## 목차

- [서비스 소개](#서비스-소개)
- [팀 구성](#팀-구성)
- [기술 스택](#기술-스택)
- [시스템 아키텍처](#시스템-아키텍처)
- [애플리케이션 아키텍처](#애플리케이션-아키텍처)
- [도메인 모듈](#도메인-모듈)
- [기술적 도전](#기술적-도전)
- [성능 검증](#성능-검증)
- [로컬 실행](#로컬-실행)
- [문서](#문서)

---

## 서비스 소개

학원은 출결, 급여, 결재, 공지, 시간표를 **각각 다른 도구로** 관리합니다. 엑셀과 메신저, 종이 결재가 뒤섞이면서 같은 정보를 여러 번 옮겨 적고, 어디가 최신인지 아무도 확신하지 못합니다.

**이음은 학원 운영에 필요한 업무를 하나의 그룹웨어로 통합합니다.**

| 영역 | 제공 기능 |
| --- | --- |
| **구성원 관리** | 계정·권한(RBAC), 원생 관리, 강의 관리 |
| **근태·급여** | 출퇴근(Wi-Fi IP 검증), 근태 정정 결재, 휴가, 급여명세서 자동 발송 |
| **전자결재** | 결재선 기반 문서 결재, 법인카드 지출 결재 |
| **협업** | 워크스페이스(업무·댓글), 메신저, 메모, 공지사항, 실시간 알림 |
| **일정·자원** | 시간표, 캘린더, 강의실 등 자원 사용 관리 |
| **파일** | S3 직접 업로드, Google Drive 연동 공유파일함 |
| **AI** | 온보딩 데이터 자동 임포트, 매출 리포트 자동 생성 |

### 멀티테넌트 SaaS

여러 학원에 **같은 제품을 제공하되 데이터는 완전히 분리**합니다. 학원마다 독립된 ECS Service와 RDS Database를 사용하며, 애플리케이션 코드는 항상 단일 학원의 데이터만 다룹니다.

> 이 결정 덕분에 도메인 코드에서 `academyId` 조건을 걸 필요가 없습니다. **학원 간 데이터 격리를 애플리케이션 로직이 아니라 배포·스키마 구조가 보장**하므로, 크로스 테넌트 데이터 유출을 코드 실수로 만들어낼 여지 자체가 없습니다.

---

## 팀 구성

백엔드 6인으로 도메인을 나눠 개발했습니다.

| 이름 | 역할 | 담당 |
| --- | --- | --- |
| **박민서** | PL / BE | 데이터 세팅(AI), 플랜 설계, 전자결재, 강의·원생관리, 출결관리 |
| **정유지** | PM / BE | 메신저, 메모, 공지사항, SMS |
| **김재원** | DBA / BE | 인증/인가, 계정·권한 관리, 플랜별 한도 지정, AI 매출 리포트 |
| **안정수** | DE / BE | 워크스페이스, 공유파일, 알림 |
| **서주원** | DE / BE | 근태 관리, 법인카드, 급여명세서 |
| **유강현** | DevOps / BE | 인프라 전담, 시간표, 캘린더 |

---

## 기술 스택

| 구분 | 사용 기술 |
| --- | --- |
| **Language / Runtime** | Java 17 |
| **Framework** | Spring Boot 3.5.14, Spring Web, Spring Data JPA, Spring Security, Spring AOP, Spring Retry |
| **인증** | JWT (jjwt 0.12.6), RBAC 권한 모델 |
| **Database** | MySQL 8.4, Flyway (스키마 버전 관리) |
| **실시간** | WebSocket + STOMP |
| **캐시** | Caffeine |
| **문서 생성** | Apache POI(Excel), OpenPDF(PDF) |
| **외부 연동** | AWS S3 (Transfer Manager), Google Drive API, Mailgun |
| **인프라** | AWS ECS on EC2, ALB, Route 53, WAF, RDS, Auto Scaling |
| **CI/CD** | GitHub Actions, Docker (멀티스테이지 빌드) |
| **관측성** | Actuator, Micrometer, Prometheus, Grafana, Loki, Grafana Alloy, Sentry, Alertmanager → Slack |
| **테스트** | JUnit 5, Testcontainers(MySQL), k6(부하테스트) |
| **기타** | MapStruct, springdoc-openapi(Swagger UI) |

---

## 시스템 아키텍처

```text
사용자 브라우저
    ↓
Route 53: *.ieum.store
    ↓
AWS WAF: 악성 요청·과다 요청 차단
    ↓
Application Load Balancer: HTTPS 종료, Host 기반 학원 분기
    ├─ academy-a.ieum.store → academy-a Target Group
    ├─ academy-b.ieum.store → academy-b Target Group
    └─ academy-c.ieum.store → academy-c Target Group
                                                ↓
ECS Cluster on EC2 Auto Scaling Group
    ├─ academy-a Service → Task 1개
    ├─ academy-b Service → Task 1개
    └─ academy-c Service → Task 1개
                                                ↓
RDS Cell 1
    ├─ mudo_tenant_academy_a / academy_a_user
    ├─ mudo_tenant_academy_b / academy_b_user
    └─ mudo_tenant_academy_c / academy_c_user
```

### 설계 결정과 트레이드오프

| 결정 | 이유 | 감수한 트레이드오프 |
| --- | --- | --- |
| 학원별 **Task + DB 분리** | 데이터 격리를 인프라가 보장, 장애 전파 차단 | 테넌트 수만큼 배포 단위 증가 |
| 여러 Task를 **같은 EC2에 밀집 배치** | 소규모 테넌트가 많은 초기 단계의 비용 효율 | Noisy neighbor 위험 → CPU는 공유·메모리는 상한 고정으로 완화 |
| **RDS는 1 Cell 공유**, DB/User만 분리 | 초기 운영 비용 최소화 | 장애 영향 범위가 넓음 → 한계 도달 시 Cell 추가로 확장 |
| 결제 플랜 = **자원 등급이 아닌 기능 정책** | 모든 테넌트에 동일한 응답 품질 보장 | 플랜별 수익-원가 구조가 단순하지 않음 |

### 관측 파이프라인

```text
ECS 호스트별 Grafana Alloy Daemon
    ├─ Actuator/Prometheus 지표 → Monitoring EC2 Prometheus
    └─ Docker stdout 로그       → Monitoring EC2 Loki
AWS 관리형 리소스 기본 지표      → CloudWatch
Prometheus + Loki + CloudWatch  → Grafana 대시보드
Prometheus 규칙 → Alertmanager  → Slack
```

---

## 애플리케이션 아키텍처

**하프 헥사고날 아키텍처** — 전면 헥사고날의 비용을 피하면서 필요한 곳에만 Port/Adapter를 적용했습니다.

```text
presentation / adapter-in
            ↓
       application
            ↓
         domain
            ↑
infrastructure / adapter-out
```

- 기본은 **레이어드 구조**를 유지합니다.
- **외부 시스템 연동**(Google Drive, S3, Mailgun), **영속성 교체 가능성**, **도메인 간 공개 계약**이 필요한 곳에만 Port/Adapter를 도입했습니다.
- 도메인 간 직접 참조를 금지하고, 공개 Application API 또는 Port를 통해서만 협업합니다.

호출 흐름은 다음 규칙을 따릅니다.

```text
Controller → Request/Command → UseCase → Service → Domain → Port/Adapter → Repository/JPA
```

자세한 계층 책임과 코드 리뷰 기준은 [ARCHITECTURE.md](docs/ARCHITECTURE.md), 모듈 간 협업 규칙은 [MODULES.md](docs/MODULES.md)를 참고하세요.

---

## 도메인 모듈

24개 도메인 모듈 + 공유 플랫폼 영역(`global`)으로 구성됩니다.

| 모듈 | 책임 |
| --- | --- |
| `auth` | 로그인, JWT 발급·검증 |
| `users` | 계정, 역할(Role), 권한(Permission) |
| `approval` | 전자결재 문서·결재선 |
| `attendance` | 출퇴근, 근태 정정, 휴가 |
| `payroll` | 급여명세서 생성·이메일 발송 |
| `corporatecard` | 법인카드 거래·지출 결재 |
| `workspace` | 워크스페이스, 업무(Task), 댓글, 반복 업무 |
| `messenger` | 실시간 메신저 |
| `notice` | 공지사항 |
| `memo` | 메모 |
| `notification` | 통합 알림(저장·조회·읽음·삭제) |
| `student` | 원생 관리 |
| `lecture` | 강의 관리 |
| `rollcall` | 출결 관리 |
| `timetable` | 시간표 |
| `calendar` | 캘린더 |
| `resourceusage` | 강의실 등 자원 사용 |
| `file` | S3 직접 업로드 |
| `sharedfile` | Google Drive 기반 공유파일함 |
| `google` | Google 계정 연동(OAuth), 토큰 관리 |
| `dataimport` | 온보딩 데이터 임포트(AI) |
| `revenuereport` | AI 매출 리포트 |
| `planquota` | 요금제별 사용 한도 |
| `platform` | 플랫폼 관리자, 테넌트 운영 |
| `global` | 공통 응답·예외·보안·로깅·관측성 (도메인 모듈 아님) |

각 모듈은 `src/main/java/com/academy/mudogroupware/<모듈>/docs/` 아래에 자체 설계 문서와 CHANGELOG를 관리합니다.

---

## 기술적 도전

### 1. WebSocket 좀비 소켓 — 서버가 죽은 연결을 영원히 붙잡고 있던 문제

**문제.** `/ws` STOMP 브로커에 heartbeat 설정이 없어, 클라이언트가 **비정상 종료**(네트워크 단절, 절전모드)하면 서버가 이를 감지하지 못하고 소켓 FD를 계속 점유했습니다. 탭을 닫는 정상 종료는 TCP FIN이 오므로 문제가 없지만, FIN조차 오지 않는 경우 서버에는 감지 수단이 아예 없었습니다. 누적되면 `ulimit` 도달로 신규 연결을 받지 못합니다.

**재현.** k6로 200명이 접속 후 무응답 상태로 방치하는 시나리오를 만들어 **적용 전/후 서버를 동시에 띄워 비교**했습니다.

| 지표 | heartbeat 미적용 | heartbeat 적용 |
| --- | --- | --- |
| 서버 주도 연결 회수 | **0건** | **200건 전량** |
| `ws_session_duration` (min ~ max) | **60.0s ~ 60.0s** | 20.4s ~ 24.6s |
| 서버 측 ESTABLISHED | 60초 내내 변화 없음 | 26.5초에 0 |

`min`과 `max`가 **둘 다 정확히 60초**라는 것은, 200개 세션 전부가 서버 개입 없이 제한 시간을 꽉 채웠다는 뜻입니다. 즉 **단 하나도 감지하지 못했습니다.**

**해결.** 전용 `ThreadPoolTaskScheduler`를 분리하고 STOMP heartbeat 10초/10초를 설정했습니다. 기존 `@Scheduled` 배치와 스레드 풀을 공유하면 고빈도 heartbeat가 저빈도 배치에 밀릴 수 있어 격리했습니다.

**정직한 한계.** 이 수정은 운영에서 실제 누수를 관측해서 고친 것이 아니라 **선제 방어**입니다. 사전에 운영 서버 FD를 직접 확인했을 때 `CLOSE_WAIT`는 0건이었고, 그 근거 데이터도 리포트에 남겼습니다.

→ [상세 리포트](docs/load-test-2026-08-19-websocket-heartbeat/REPORT.md)

### 2. 도메인 특성에 맞춘 동시성 제어 전략

**하나의 락 전략을 전사 적용하지 않고, 경합 빈도와 실패 비용에 따라 다르게 선택**했습니다.

| 도메인 | 전략 | 선택 이유 |
| --- | --- | --- |
| `workspace` | **비관적 락** (`PESSIMISTIC_WRITE`) | 동시 수정이 실제로 잦고, lost update가 조용히 발생하면 사용자가 알아채기 어려움 |
| `sharedfile` | **낙관적 락** (`@Version`) | 단일 행 대상이라 경합이 드물고, 충돌 시 실패 처리로 충분 |
| `google` | **애플리케이션 레벨 single-flight** | 행 경합이 아니라 "외부 API 중복 호출" 문제 |
| `notification` | **UNIQUE 멱등키 + 재시도** | 행 경합이 아니라 "중복 생성" 문제 |

**workspace — 2단계 락.** 업무·템플릿은 락을 바로 걸지 않습니다. ① 락 없이 워크스페이스 소속을 확인하고 ② 확인된 PK에만 락을 겁니다. 다른 워크스페이스 소속 행에 락을 잡았다 푸는 낭비를 없앴습니다.

**google — 락 없는 single-flight.** `ConcurrentMap#compute()`가 같은 키에 대해 원자적으로 실행되는 성질을 이용해, 캐시 미스 시 동시 요청들을 직렬화하고 **Google API 호출이 딱 1회만** 나가도록 했습니다(cache stampede 방지).

**notification — 락 대신 제약.** `idempotency_key` UNIQUE 제약을 두고, 위반 예외를 잡아 "이미 저장됨"으로 처리합니다. 다른 제약 위반은 삼키지 않고 그대로 던져 진짜 결함을 숨기지 않습니다.

→ 검증된 동시성 이슈 사례는 [CONCURRENCY_ANALYSIS.md](CONCURRENCY_ANALYSIS.md) 참고

### 3. 신뢰할 수 없는 클라이언트 IP — 서명 기반 검증

**문제.** 출퇴근을 학원 Wi-Fi 공인 IP로 검증하는데, `request.getRemoteAddr()`는 사용자 IP가 아니라 **Spring에 마지막으로 TCP 연결한 상대**(ALB·프록시)의 주소를 돌려줍니다. 이 프록시 IP를 화이트리스트에 등록하면 **학원 밖 어디서든 출퇴근이 통과**됩니다. `X-Forwarded-For`는 클라이언트가 위조할 수 있어 그대로 쓸 수 없습니다.

**해결.** Next 서버가 외부 공인 IP를 추출해 **HMAC-SHA256으로 서명**하고, Spring이 서명·시간·형식 무결성을 검증한 뒤에만 신뢰합니다. 서명 대상에 `METHOD + PATH + CLIENT_IP + TIMESTAMP`를 모두 포함해 **재사용 방지·요청 결박·시간 제한**을 동시에 확보했습니다.

> IP를 *전달하는* 문제를, IP의 출처를 *증명하는* 문제로 바꾼 사례입니다.

### 4. 테넌트 배포를 선언적으로 관리

테넌트가 늘어날수록 배포 절차가 사람 손을 타면 실수가 누적됩니다. `infra/tenants.yml` 하나에 테넌트 정의를 모으고, 파이프라인이 이 매니페스트를 읽어 동작하도록 했습니다.

```yaml
tenants:
  - code: academy-a
    enabled: true
    billing_plan: paid          # 자원 등급이 아니라 유료 엔드포인트·쿼터 메타데이터
    runtime_profile: shared-default
    cell: cell-1
    service: mudo-prod-svc-academy-a
    health_url: https://academy-a.ieum.store/actuator/health
```

배포는 **검증 → 배포 2단계**로 나뉩니다.

```text
validate  →  매니페스트 사전 검증 (--validate-only)
deploy    →  테스트·JAR 빌드 → 앱/마이그레이션 이미지 푸시
             → DB 마이그레이션 실행 → 학원별 순차 ECS 배포 + 헬스체크
```

`enabled: false`로 두면 리소스는 보존하되 Task를 실행하지 않아, 온보딩 리허설용 테넌트를 비용 없이 유지할 수 있습니다. 이미지는 Git SHA로 태깅해 이미 ECR에 있으면 재빌드를 건너뜁니다.

> **범위 명확화** — 이 파이프라인이 담당하는 것은 마이그레이션과 ECS 배포입니다. Target Group, Route 53 레코드, RDS Database/User, SSM 파라미터는 테넌트 온보딩 시 별도로 준비합니다(현 단계에서는 콘솔 기반, 향후 IaC 전환 대상).

→ [배포 가이드](docs/DEPLOYMENT.md) · [인프라 구축 가이드](docs/2026-08-03-academy-groupware-multi-tenant-aws-infra-guide.md)

---

## 성능 검증

### 피크타임 부하테스트 (직원 500명 규모 학원)

유료 플랜 최대 규모 학원의 피크타임을 30분간 재현했습니다.

| 지표 | 결과 |
| --- | --- |
| 총 요청 | 57,292건 |
| checks 성공률 | 99.23% |
| `http_req_duration` p95 | **82ms** |
| `http_req_failed` | 2.92% (기준 `<5%` ✅) |
| WebSocket 연결 성공률 | 100% (기준 `>95%` ✅) |

k6 threshold 4개 전부 통과했고, Tomcat 스레드·DB 커넥션 풀 모두 여유가 확인됐습니다.

**부하테스트로 실제 결함 4건을 발견해 수정**했습니다 — 랜덤 ID로 인한 대량 403, Wi-Fi IP 화이트리스트 IPv4/IPv6 불일치, 시드 데이터와 결재선 로직 불일치(실서비스 잠재 버그), RPS 설계값 오차.

→ [부하테스트 리포트](docs/load-test-2026-08-15/REPORT.md)

---

## 로컬 실행

### 사전 요구사항

- JDK 17
- Docker / Docker Compose

### 1. 환경 변수 설정

프로젝트 루트에 `.env.local`을 만듭니다. 아래 두 값은 **기본값이 없어 누락 시 기동에 실패**합니다.

```bash
JWT_SECRET=<32바이트 이상 임의 문자열>
GOOGLE_TOKEN_ENCRYPTION_KEY=<임의 문자열>
DB_USERNAME=mudo
DB_PASSWORD=mudo
LOCAL_MYSQL_ROOT_PASSWORD=<임의 문자열>
```

### 2. Docker Compose로 실행 (권장)

MySQL과 애플리케이션을 함께 띄웁니다.

```bash
docker compose --env-file .env.local -f docker-compose.local.yml up -d --build
```

### 3. 애플리케이션만 직접 실행

이미 MySQL이 있다면 Gradle로 바로 띄울 수 있습니다.

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

### 4. 확인

| 항목 | URL |
| --- | --- |
| 헬스체크 | http://localhost:8080/actuator/health |
| API 문서 (Swagger UI) | http://localhost:8080/swagger-ui.html |

### 테스트

```bash
./gradlew test
```

---

## 문서

프로젝트의 설계 판단과 운영 기준은 모두 문서로 남겼습니다.

| 문서 | 내용 |
| --- | --- |
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | 계층 책임, 의존 방향, Port/Adapter 적용 기준, 코드 리뷰 체크리스트 |
| [MODULES.md](docs/MODULES.md) | 모듈 정의, 모듈 간 협업 규칙, 공개 API 정책 |
| [API_CONTRACT.md](docs/API_CONTRACT.md) | API 응답 규격, 페이지네이션, 공통 계약 |
| [DATABASE.md](docs/DATABASE.md) | 스키마 정책, Flyway 마이그레이션 규칙 |
| [ERROR_HANDLING.md](docs/ERROR_HANDLING.md) | 예외 계층, 에러 코드 체계 |
| [LOGGING_CONVENTION.md](docs/LOGGING_CONVENTION.md) | 구조화 로깅 규칙, TraceId 전파 |
| [DEPLOYMENT.md](docs/DEPLOYMENT.md) | CI/CD 파이프라인, 배포 절차, 롤백 |
| [DATA_LIFECYCLE_POLICY.md](docs/DATA_LIFECYCLE_POLICY.md) | 소프트 삭제, 보존 기간 정책 |
| [멀티테넌트 AWS 인프라 가이드](docs/2026-08-03-academy-groupware-multi-tenant-aws-infra-guide.md) | 인프라 설계 근거, 트레이드오프, 구축 절차 |
| [S3 직접 업로드 아키텍처](docs/2026-08-07-s3-direct-upload-architecture.md) | Presigned URL 기반 업로드 설계 |
| [AGENTS.md](AGENTS.md) | 팀 공통 개발 기준(코드 작성·테스트·문서화) |

운영 대응 절차는 [`infra/runbooks/`](infra/runbooks/)에 정리돼 있습니다.
