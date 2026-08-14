# 학원 그룹웨어 다중 학원 SaaS AWS 인프라 구축 가이드

> 최초 작성일: 2026-08-03
> 최종 정책·구현 대조일: 2026-08-05
> 대상 리전: `ap-northeast-2`(서울)
> 대상 독자: AWS 운영 인프라를 처음 구성하는 개발자
> 문서 성격: 비즈니스 검토를 포함한 목표 아키텍처와 콘솔 구축 절차
> 기존 단일 학원용 `EC2 1대 + nginx + Docker Compose` 운영안은 이 문서로 대체한다.

---

## 0. 먼저 읽어야 할 결론

이 서비스는 여러 학원에 같은 그룹웨어 제품을 제공하되, 다음 방식으로 자원을 공유한다.

- 학원마다 독립된 ECS Service와 애플리케이션 Task 1개를 기본 제공한다.
- 여러 학원의 Task를 같은 EC2 인스턴스에 밀집 배치한다.
- Task 예약 자원이 기존 EC2에 들어가지 않으면 ECS Capacity Provider가 EC2를 추가한다.
- 결제 플랜과 무관하게 모든 학원 Task에 같은 CPU·메모리·스레드·DB Pool 기본값을 적용한다.
- CPU는 학원마다 같은 기본 몫을 예약하면서 남는 CPU를 다른 학원이 사용할 수 있게 한다.
- 메모리는 예약량과 최대값을 함께 설정하고, CPU처럼 무제한으로 빌려주지 않는다.
- Basic·Standard·Premium 같은 결제 플랜은 자원 등급이 아니라 사용할 수 있는 유료 엔드포인트·기능·호출량 정책을 결정한다.
- RDS는 처음에 1개 Cell을 공유하되 학원별 Database와 User를 분리한다.
- RDS 성능이 부족하면 먼저 사양을 높이고, 한계와 장애 영향이 커지면 두 번째 RDS Cell을 만든다.
- 외부 공격은 WAF, 애플리케이션 사용량 제한, 학원별 DB 커넥션 제한으로 RDS에 도달하기 전에 차단한다.
- AWS 관리형 리소스의 기본 지표와 핵심 장애 알림은 CloudWatch에 유지한다.
- 애플리케이션·JVM 지표는 Grafana Alloy가 수집해 전용 모니터링 EC2의 Prometheus로 보내고, 컨테이너 로그는 Loki로 보낸다.
- 전용 모니터링 EC2에는 Grafana, Prometheus, Loki, Alertmanager를 Docker Compose로 실행한다.
- 신규 구축이므로 Promtail은 사용하지 않고 Grafana Alloy로 수집기를 통일한다.

최종 요청 흐름은 다음과 같다.

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

관측 경로
    ECS 호스트별 Alloy Daemon
        ├─ Actuator/Prometheus 지표 → Monitoring EC2 Prometheus
        └─ Docker stdout 로그 → Monitoring EC2 Loki
    AWS 기본 지표 → CloudWatch
    Prometheus + Loki + CloudWatch → Monitoring EC2 Grafana
    Prometheus 규칙 → Alertmanager → Slack
    CloudWatch 핵심 알람 → SNS → Slack
```

---

## 1. 유지해야 하는 제약조건

### 1-1. 계정과 권한

- AWS 루트 사용자는 팀이 사용할 수 없고, 루트 사용자나 별도 계정 관리자에게 권한 변경을 요청할 수도 없다.
- 인프라 담당자와 개발팀원은 하나의 IAM 사용자 `team04-02`를 공유한다.
- 공유 IAM 사용자에는 Access Key를 발급하지 않는다.
- 현재 계정에서 `team04-02`의 Access Key는 0개인 것을 확인했다.
- IAM 사용자 MFA 장치와 연결 정책은 현재 사용자에게 조회 권한이 없어 콘솔 로그인 과정과 실제 권한 오류를 기준으로 확인한다.
- 별도 `groupware-ops-role`, `groupware-infra-admin-role` 전환은 구성할 수 없으므로 `team04-02`로 허용된 범위의 작업을 직접 수행한다.
- 기존 `AWSServiceRoleFor*` 서비스 연결 역할과 공용 `ecsTaskExecutionRole`은 수정하지 않고, 생성 권한이 허용되면 `mudo-prod-*` 전용 역할만 새로 만든다.
- 같은 IAM 사용자를 쓰기 때문에 CloudTrail만으로 실제 작업자를 구분할 수 없다.
- 운영 변경 시 GitHub 이슈에 작업자, 일시, 대상, 이유, 결과를 기록한다.

> 이 방식은 AWS 권장 방식이 아니다. 사람별 계정을 쓸 수 없고 루트·관리자 협조도 받을 수 없다는 프로젝트 제약을 수용한 보완책이다. 면접에서는 이 제약, 최소 권한 분리 미완료, 감사 추적 한계를 숨기지 않고 설명한다.

### 1-2. 브랜치와 이미지 배포

- `develop` 머지: 테스트 후 Docker Hub에 `develop` 및 Git SHA 태그를 게시한다.
- 프론트 개발자는 Docker Hub 이미지를 이용해 로컬 연동 테스트를 한다.
- `main` 머지: 다시 테스트하고 Amazon ECR에 Git SHA 태그로 게시한 뒤 ECS 운영 서비스를 갱신한다.
- 운영 AWS 자격 증명은 GitHub Secrets의 장기 Access Key가 아니라 GitHub OIDC로 발급받는다.
- 운영 이미지는 `latest`가 아니라 Git SHA로 고정한다.

### 1-3. 파일 저장 요구사항

- 원장·회계용 민감 파일은 S3 Finance 버킷을 사용한다.
- 교사·조교를 제외한 직원용 파일은 S3 Staff 버킷을 사용한다.
- 전 구성원 공용 파일은 Google Drive API를 사용한다.
- 다중 학원 서비스이므로 S3와 Google Drive 모두 학원 간 데이터가 섞이지 않게 분리한다.

### 1-4. 모니터링

- ALB, WAF, ECS, EC2, RDS 같은 AWS 기본 지표와 모니터링 EC2 자체 장애 알림은 CloudWatch에 유지한다.
- JVM, Tomcat, Hikari, 비동기 Executor와 학원별 사용량 지표는 Prometheus에 저장한다.
- 애플리케이션 stdout 로그는 Grafana Alloy가 수집해 Loki에 저장한다.
- Grafana는 Prometheus, Loki, CloudWatch를 데이터 소스로 사용한다.
- 모니터링 서버는 앱 ECS 인스턴스와 분리해 앱 부하와 배포가 대시보드를 직접 중단시키지 않게 한다.
- 서비스 이용료 대시보드는 AWS 청구 금액이 아니라 애플리케이션의 플랜·사용량 정책으로 계산한 내부 금액을 표시한다.
- AWS 루트 계정이 IAM 사용자의 Billing 조회를 막아놓은 경우, Grafana에서 실제 AWS 청구액을 표시하려 하지 않는다.
- 모니터링 EC2 한 대는 단일 장애점이므로 CloudWatch 핵심 알람을 없애지 않는다.

### 1-5. 결제 플랜과 자원 정책

- 모든 학원은 기본적으로 동일한 `shared-default` ECS 런타임 프로필을 사용한다.
- 결제 플랜은 CPU·메모리·스레드·Hikari 크기로 구분하지 않는다.
- 결제 플랜은 사용할 수 있는 유료 엔드포인트·기능과 호출량·저장공간 정책으로 구분한다.
- 플랜 변경은 애플리케이션 구독·Entitlement 변경으로 처리하고 ECS Task 재배포와 분리한다.
- 유료 엔드포인트의 과부하는 결제 등급에 따른 Task 크기 확장이 아니라 엔드포인트별 Rate Limit·쿼터·동시 실행 제한으로 방어한다.
- 실제 부하가 공용 Cell의 안전 범위를 반복해서 넘는 학원은 결제 플랜과 별개로 전용 Task 또는 다른 Cell로 이동한다.

---

## 2. 회사·면접관 관점의 설계 평가

### 2-1. 이 구조의 비즈니스 장점

1. 학원이 늘어날 때마다 EC2 한 대를 추가하지 않으므로 초기 원가가 낮다.
2. 모든 학원에 같은 런타임 자원 상한을 적용해 기본 품질과 공정성을 단순하게 관리할 수 있다.
3. 한 학원의 애플리케이션 장애가 다른 학원의 프로세스를 직접 종료시키지 않는다.
4. 동일한 운영 이미지를 사용해 배포 편차를 줄인다.
5. EC2가 꽉 찼을 때만 인스턴스를 추가하므로 유휴 자원을 줄인다.
6. RDS Cell을 추가하는 방식으로 고객 증가에 따라 장애 영향 범위를 나눌 수 있다.

### 2-2. 반드시 설명해야 하는 트레이드오프

- 학원별 Spring JVM Task는 데이터 격리가 쉬운 대신 학원 수만큼 JVM 기본 메모리가 필요하다.
- `binpack`은 비용 효율은 높지만 한 EC2 장애가 여러 학원에 동시에 영향을 줄 수 있다.
- RDS에서 Database/User를 나눠도 CPU, 메모리, I/O는 공유된다.
- 학원별 RDS CPU 30%를 정확히 보장할 수 없으므로 앱 요청량과 커넥션 수를 제한해야 한다.
- 유료 엔드포인트가 보고서·대량 파일 처리처럼 무거우면 동일 자원 정책만으로는 보호되지 않으므로 엔드포인트별 Rate Limit·쿼터·동시 실행 제한이 필요하다.
- ASG 최소 1대는 비용이 낮지만 인스턴스 장애 시 새 인스턴스가 준비될 때까지 중단될 수 있다.
- ASG 최소 2대는 장애 대응력이 높지만 평상시 유휴 자원이 늘어난다.
- 단일 모니터링 EC2는 비용과 구성이 단순하지만 장애 중 대시보드·로그 검색이 일시 중단될 수 있다.
- Prometheus와 Loki를 직접 운영하므로 버전 업그레이드, 디스크, 보존 기간과 백업 책임이 팀에 있다.

### 2-3. 사업 단계별 운영 모드

| 단계 | ECS ASG | RDS | 목적 |
|---|---:|---|---|
| 개발·내부 검증 | min 1 / desired 1 | Single-AZ 가능 | 비용 최소화 |
| 첫 유료 학원 운영 | min 2 / desired 2 | Multi-AZ 권장 | 단일 장애점 감소 |
| 학원 증가 | max 범위 내 자동 확장 | 사양 상향 | 기존 Cell 확장 |
| Cell 한계 접근 | EC2 ASG 추가 또는 기존 확장 | RDS Cell 2 생성 | 장애 범위와 부하 분리 |

실제 고객 데이터를 받기 시작하면 `min 2 + RDS Multi-AZ`로 전환하는 것을 기본 운영 정책으로 삼는다.

### 2-4. 이번 단계에서 하지 않는 것

- Kubernetes/EKS
- Thanos 또는 Mimir 기반의 메트릭 고가용성·장기 보관
- Loki 분산 모드와 S3 객체 저장소
- 모니터링 EC2 Multi-AZ 이중화
- 학원별 RDS 인스턴스 생성
- Spot 인스턴스에 운영 앱 전부 배치
- 자동 RDS Cell 이동
- DB 쓰기 노드의 수평 확장
- 학원 수요를 예측하는 자체 EC2 스케줄러

ECS Capacity Provider가 제공하는 배치와 확장을 우선 사용하고, 자체 스케줄러는 실제 한계가 확인된 후 검토한다.

### 2-5. 비즈니스 성공 기준

인프라가 동작한다는 사실만으로 성공으로 판단하지 않는다. 월별로 다음 값을 기록한다.

| 지표 | 확인 이유 |
|---|---|
| 학원 1곳당 월 인프라 원가 | 결제 플랜 가격과 마진 검증 |
| EC2 예약 자원 사용률 | binpack 효과와 유휴 자원 확인 |
| RDS Cell당 수용 학원 수 | 다음 Cell 투자 시점 결정 |
| 신규 학원 온보딩 시간 | 운영 자동화 투자 우선순위 결정 |
| 학원별 p95 응답시간·오류율 | 모든 결제 플랜에서 같은 기본 품질을 지키는지 확인 |
| 장애 영향 학원 수 | Cell 분리 시점 결정 |
| 공격 차단 요청과 비용 증가량 | 비용 공격 방어 효과 검증 |
| RTO·RPO 복구 시험 결과 | 계약·SLA 근거 마련 |

결제 플랜은 CPU·메모리 크기가 아니라 사용 가능한 유료 엔드포인트·기능, 월 호출량, 저장공간과 지원 수준으로 구분한다. 플랜 변경은 애플리케이션 권한·구독 데이터 변경으로 처리하며 ECS Task 재배포를 요구하지 않는다.

### 2-6. 콘솔 구축과 IaC의 관계

이 문서는 처음 구성하는 사람이 AWS 리소스 관계를 이해하도록 콘솔 순서로 작성한다. 하지만 콘솔 수동 구성이 최종 운영 자동화의 목표는 아니다.

1. 첫 Cell은 이 문서로 수동 구축하고 모든 설정값을 기록한다.
2. 장애·배포·확장 시험을 통과해 설계가 안정화됐는지 확인한다.
3. 두 번째 Cell을 만들기 전 Terraform, CloudFormation 또는 CDK로 재현한다.
4. 이후 Cell은 IaC 코드로 생성하고 콘솔 직접 변경은 긴급 작업으로 제한한다.

면접에서는 “콘솔만 사용한다”가 아니라 “첫 구축은 학습과 검증을 위해 콘솔로 진행하고, 반복 가능한 시점부터 IaC로 전환한다”고 설명한다.

---

## 3. 구축 전에 결정하고 기록할 값

다음 표의 `<값>`을 먼저 채운다. 값이 정해지지 않았다면 AWS 리소스를 만들지 않는다.

| 항목 | 예시 | 실제 값 |
|---|---|---|
| AWS 계정 ID | `123456789012` | `635249349258` |
| 리전 | `ap-northeast-2` | `ap-northeast-2` |
| 공유 IAM 사용자 | `team-user` | `team04-02` |
| IAM 관리 제약 | 별도 관리자 역할 사용 | 루트·관리자 접근 및 요청 불가. `iam:ListUsers`·`iam:ListUserPolicies`·`kms:CreateKey`·`cloudtrail:LookupEvents` 등 일부 액션도 계정 정책상 차단됨 |
| GitHub 조직/사용자 | `team-mudo` | `mudo-project` |
| GitHub 저장소 | `Team1-MUDO-BackEnd` | `Team1-MUDO-BackEnd` |
| 서비스 기본 도메인 | `ieum.store` | `ieum.store` |
| 첫 학원 코드 | `academy-a` | `academy-a` |
| 첫 학원 표시 이름 | `A학원` | `테스트학원A` |
| 운영 DB 엔진 버전 | `MySQL 8.0.x` | `MySQL 8.4.5` (8.0 라인 Extended Support 전환으로 8.4 LTS 사용) |
| 운영 EC2 아키텍처 | `X86_64` | `X86_64` |
| 시작 인스턴스 유형 | `m7i.large` 예시 | `t3.small` (계정 정책상 인스턴스 크기 제한, `db.t3.small`/`t3.small`만 허용됨) |
| ASG 최대 인스턴스 수 | `4` 예시 | `2` (min/desired 1, max 2 — binpack·오토스케일링 데모 목적) |
| RDS 시작 클래스 | 부하 테스트 후 결정 | `db.t3.small`, Multi-AZ |
| 모니터링 도메인 | `monitoring.ieum.store` | `monitoring.ieum.store` |
| 모니터링 EC2 | `t4g.medium` 예시 | `<MONITORING_INSTANCE_TYPE>` |
| 모니터링 EBS | gp3 100 GiB 예시 | `<MONITORING_VOLUME_SIZE>` |
| Prometheus 보존 기간 | 15일 | `<PROMETHEUS_RETENTION>` |
| Loki 보존 기간 | 30일 | `<LOKI_RETENTION>` |

명명 규칙은 다음으로 통일한다.

```text
mudo-prod-<resource>
mudo-prod-tenant-<tenant-code>-<resource>
```

공통 태그:

```text
Project=MUDO
Environment=prod
ManagedBy=team
CostCenter=groupware
```

학원 전용 리소스에는 다음 태그도 추가한다.

```text
Tenant=<tenant-code>
BillingPlan=basic|standard|premium
RuntimeProfile=shared-default
```

### 3-1. 현재 코드 준비 상태 확인

이 문서의 AWS 리소스를 만들었다고 현재 코드가 곧바로 다중 학원 운영을 지원하는 것은 아니다. 2026-08-05 저장소 기준 상태는 다음과 같다.

현재 코드에 반영된 항목:

- `application-prod.yaml`은 운영 DB URL·사용자·비밀번호를 환경변수로 받고, 앱 시작 시 Flyway 자동 실행을 끈다.
- 공통 런타임 기본값은 Hikari Pool 10, Tomcat 최대 스레드 60·대기열 100, 비동기 최대 스레드 4·대기열 40, 무거운 작업 동시 수 2로 상향·통일했다.
- 별도 bounded 비동기 Executor, 거부 지표, 무거운 작업 동시성 제한 컴포넌트가 추가됐다.
- `TENANT_ID`, `TENANT_PLAN`, `DEPLOYMENT_SHA`는 운영 프로필에서 필수이며 로그 MDC와 Micrometer 공통 태그에 사용된다.
- 운영 Actuator는 `health`, `prometheus`만 노출하고 health 상세정보를 숨긴다.
- Prometheus Registry와 Tomcat·Hikari·비동기 Executor·무거운 작업 지표가 연결돼 있다.
- 운영 로그는 stdout으로 출력되며 `tenantId`, `plan`, `deploymentSha`, `traceId`를 포함한다.
- Grafana·Prometheus·Loki·Alertmanager Compose, Grafana Alloy 설정, 기본 대시보드·알람·Runbook 원본이 `infra/` 아래에 준비돼 있다.
- `main` 운영 워크플로는 Git SHA 이미지 게시, 공통 `runtime-profiles.yml` 기반 Task Definition 생성, Cell 용량 검사와 학원별 순차 배포를 수행한다.
- Flyway 전용 이미지와 학원별 ECS Migration Task Definition, 실패 시 이전 Task Definition으로 되돌리는 배포 스크립트가 준비돼 있다.

main 운영 자동 배포 전에 남은 항목:

- Heavy Job Limiter를 실제 보고서·대량 파일 등 무거운 유료 엔드포인트 UseCase에 적용한다.
- 결제 플랜별 유료 엔드포인트 접근 권한은 서버가 조회한 구독·Entitlement 정보로 검증한다. 클라이언트가 보낸 플랜명이나 헤더를 최종 근거로 사용하지 않는다.
- Finance/Staff S3 버킷 분기와 서버가 결정한 학원 Prefix 강제를 구현한다. 사용자가 보낸 `tenantId`로 파일 경로를 결정하지 않는다.
- `TENANT_ID`를 인증된 학원, DB 계정·Database, 파일 Prefix와 교차 검증한다.
- Redis Pub/Sub, Google Drive, Sentry는 실제 운영 요구에 맞게 연결 여부를 확인하고 빠진 부분을 구현한다.
- 운영 ECR·ECS Service·Task Role·Execution Role·SSM Parameter와 Migration Task 실행 권한을 AWS에 실제 생성하고 테스트 Cell에서 검증한다.
- `/actuator/prometheus`는 애플리케이션 보안 설정상 수집을 허용하므로, ALB Listener 고정 응답과 보안 그룹으로 인터넷 접근을 차단하고 Alloy 내부 수집만 허용한다.
- 로그의 JWT·비밀번호·개인정보 마스킹을 검증하고, Loki Label은 저카디널리티 필드로 제한한다.
- 준비된 모니터링 파일은 실제 AWS 테스트 인스턴스에서 UID/GID, ARM64 호환성, SSM 주입, Slack 알림, IAM 권한까지 검증한 뒤 운영에 반영한다.

다음 테스트가 통과하기 전에는 main 자동 배포를 활성화하지 않는다.

```text
서로 다른 TENANT_ID의 파일 경로 격리 테스트
다른 학원 DB 계정으로 접근 실패 테스트
공통 런타임 프로필 Hikari Pool 설정 테스트
공통 Tomcat·비동기 Executor·무거운 작업 동시성 설정 테스트
결제 플랜별 유료 엔드포인트 허용·거부 테스트
Tomcat 또는 비동기 작업 포화 시 무제한 대기하지 않고 429/503으로 제한되는지 확인
WAF 밖 애플리케이션 Rate Limit 테스트
Redis가 연결된 두 애플리케이션 인스턴스 간 WebSocket 메시지 테스트
prod 프로필에서 Flyway 자동 실행 비활성화 확인
민감정보 로그 마스킹 테스트
`/actuator/prometheus`가 ALB에서는 차단되고 Alloy의 컨테이너 내부 수집은 성공하는지 확인
Alloy 중단·재시작 후 로그 위치와 메트릭 전송이 복구되는지 확인
```

---

### 3-2. 실제 인프라 구축 진행 현황 (2026-08-07~08 기준)

이 계정(`635249349258`, `team04-02`)에서 실제로 진행한 내역이다. 계정 고유의 제약과 실수로 가이드 원문과 달라진 부분을 같이 적어둔다.

| 장 | 상태 | 비고 |
|---|---|---|
| 5. IAM | ✅ 완료(변형) | 역할 전환 없이 공유 계정(`team04-02`)만 사용. `iam:ListUsers`, `iam:ListUserPolicies`, `kms:CreateKey`, `cloudtrail:LookupEvents`, `application-autoscaling:DescribeScalingActivities` 등 일부 액션이 계정 정책상 차단됨(원인 미확인) |
| 6. 비용 한도 | ⬜ 미착수 | |
| 7. VPC | ✅ 완료 | `mudo-prod-vpc`(10.40.0.0/16), 서브넷 6개, NAT 2개(가이드 초기 권장 1개보다 많음) |
| 8. 도메인·ACM | ✅ 완료 | `ieum.store`를 가비아에서 구매(Route 53 직접 구매 아님) → Route 53 Hosted Zone 생성 후 가비아 네임서버를 Route 53 NS로 위임(가이드 원문엔 없는 단계) → ACM 인증서 발급 |
| 9. ECR·로그·KMS·SSM | ✅ 완료(변형) | 커스텀 KMS 키(`alias/mudo-prod-ssm`) 생성 권한 없음(계정 제약) → SecureString은 AWS 관리형 키(`alias/aws/ssm`) 사용으로 대체 |
| 10. RDS | ✅ 완료(변형) | `db.t3.small` Multi-AZ (`db.t3.medium` 이상은 계정 인스턴스 크기 제한으로 거부됨). 엔진 `MySQL 8.4.5`(8.0 라인이 Extended Support 유료 전환 상태라 회피). `max_connections=144` 확인 |
| 11. ECS IAM 역할 | 🟡 부분 완료 | 인스턴스 역할·Task Execution Role·academy-a Task Role 완료. 모니터링 EC2 역할·Alloy 역할·GitHub OIDC 배포 역할은 각 해당 장에서 진행 예정 |
| 12. ECS Cluster·ASG·Capacity Provider | ✅ 완료(변형) | ECS 콘솔 마법사(ASG 동시 생성)가 원인 불명의 권한 오류로 실패 → **시작 템플릿+ASG를 EC2 콘솔에서 직접 생성**하는 방식으로 우회. `t3.medium`이 계정 정책상 거부되어 `t3.small`로 축소. ASG는 `min 1 / desired 1 / max 2`(가이드 권장 2/2/4보다 작음, binpack·오토스케일링 데모 목적). AMI는 커뮤니티 AMI에서 `al2023-ami-ecs-hvm-*-x86_64`(AWS 공식)를 직접 검색해서 선택해야 함 — AWS Marketplace 이미지나 일반 Amazon Linux 이미지를 잘못 고르면 ECS Agent가 없어서 등록이 안 됨. 클러스터가 이전 실패 시도로 `INACTIVE` 상태로 남아있어 재생성(재활성화) 필요했음 |
| 13. 런타임 프로필 | ✅ 완료(값 변경) | `shared-default`를 `t3.small`(2 vCPU/2GiB) 1대에 학원 2개가 binpack으로 들어가도록 축소: `cpu 850`(≈0.83 vCPU), `memoryReservation 800MiB`(≈0.78GiB), `memory 950MiB`(≈0.93GiB). **ECS 콘솔의 Task Definition 컨테이너 CPU/메모리 입력 필드는 units/MiB가 아니라 vCPU/GiB 소수값을 그대로 받는다** — 이 단위를 모르고 `850`을 그대로 넣었다가 "850 vCPU" 요구로 해석되어 Task가 영원히 배치 안 되는 문제를 겪었음. 반드시 소수(vCPU 0.83, GiB 0.78/0.93)로 입력 |
| 14. Task Definition | ✅ 완료(임시) | academy-a만 생성. ECR에 실제 이미지가 아직 없어(20장 CI/CD 이전) `nginx:alpine` placeholder(명령어 재정의로 8080에서 200 응답) 사용. CD 활성화 후 실제 이미지로 교체 예정 |
| 15. ALB·Target Group | ✅ 완료 | `mudo-prod-tg-academy-a`(8080), 리스너 규칙 3개(우선순위 1: `/actuator/prometheus*` 차단 403, 100: `academy-a.ieum.store` 호스트 헤더 전달, 기본값: 404) |
| 16. ECS Service | ✅ 완료(인프라 스모크 테스트) | `mudo-prod-svc-academy-a`, 배치 전략 `binpack`(MEMORY), 원하는 태스크 1, 상태 검사 유예 60초. Target Healthy 확인, `https://academy-a.ieum.store/actuator/health` 200 확인. **단, 이 200 응답은 ALB→nginx placeholder 연결만 검증한 것**이며 실제 애플리케이션·DB·SSM·S3·Task Role·Flyway 동작은 검증되지 않았다. 애플리케이션 검증은 실제 ECR 이미지 배포(20장 CD 활성화) 이후 별도로 확인한다 |
| 17. WAF | ✅ 완료(관찰 전용) | `mudo-prod-web-acl`, ALB에 연결. AWS 관리형 규칙 3개(Common, KnownBadInputs, SQLi) + Rate-based 규칙(2000/5분, 소스 IP 기준) 전부 Count 모드 — **차단은 아직 적용되지 않은 관찰 전용 상태**. 오탐 검증 기간 후 Block 전환 기준·롤백 절차는 별도 항목으로 트래킹 |
| 18. S3 격리 | ✅ 완료 | 공유 버킷(`mudo-prod-staff-635249349258`) + `tenants/{code}/` prefix + IAM Resource 조건으로 확정(가이드 원문과 동일 방식). academy-a Task Role 정책을 prefix 기준(`tenants/academy-a/*`)으로 교체 완료 |
| 19. WebSocket·Redis | ✅ 확인 완료(보류) | 코드 조사 결과 Redis 클라이언트 의존성·연동 코드가 아직 없음(WebSocket은 인메모리 SimpleBroker). ElastiCache는 만들지 않음 — 실제 Redis 코드 구현 시점에 다시 결정 |
| 20. main 운영 CI/CD | ⬜ 미착수 | GitHub OIDC 역할·워크플로우 연결 예정, 오늘 목표 지점 |
| 21. 모니터링 | ⬜ 미착수 | |
| 22. 새 학원 온보딩 | 📄 문서화 완료 | academy-b/c 추가 절차를 별도 런북으로 정리해 아티팩트로 게시 |
| 23. 자동 확장 검증 | ⬜ 미착수 | academy-b 추가로 binpack 확인 → academy-c 추가로 실제 스케일아웃 트리거 시나리오로 진행 예정 |

---

## 4. 전체 구축 순서

아래 순서를 바꾸지 않는다.

1. 제한된 공유 IAM 사용자와 기존 AWS 서비스 역할 확인
2. 비용 알람과 최대 확장 한도 결정
3. VPC·서브넷·라우팅·보안 그룹 생성
4. ACM 인증서와 Route 53 준비
5. ECR·CloudWatch 핵심 로그·KMS·SSM 준비
6. RDS Cell 1 생성
7. ECS용 IAM 역할 생성
8. ECS Cluster·Launch Template·ASG·Capacity Provider 생성
9. ALB 생성
10. 첫 학원 DB·S3 Prefix·시크릿 생성
11. 운영 WebSocket을 사용한다면 ElastiCache 생성
12. 모니터링 IAM Role·EC2·EBS·내부 DNS 생성
13. Grafana·Prometheus·Loki·Alertmanager 설치
14. Alloy Task Definition과 ECS Daemon Service 생성
15. 첫 학원 Task Definition·Target Group·ECS Service 생성
16. ALB Host 규칙과 DNS 연결
17. WAF 생성 및 ALB 연결
18. Grafana 데이터 소스·대시보드·알림 구성
19. 수동 배포와 장애 시험
20. GitHub Actions main CD 활성화
21. 두 번째 학원 추가 시험
22. EC2 자동 증설과 RDS 부하 제한 시험

각 장 끝의 검증 항목을 통과하기 전에는 다음 장으로 넘어가지 않는다.

---

## 5. 제한된 IAM 공유 로그인과 역할 구성

### 5-1. 실제 계정 제약 확인

2026-08-05 콘솔 확인 기준은 다음과 같다.

```text
AWS Account ID: <ACCOUNT_ID>
Shared IAM User: team04-02
Region: ap-northeast-2
Access Keys: 0
Root/Admin access: unavailable
Root/Admin permission request: unavailable
```

`team04-02`는 `iam:ListMFADevices`, `iam:ListUserPolicies`, `iam:ListGroupsForUser` 같은 IAM 조회가 거부된다. 따라서 사용자에게 어떤 정책과 MFA 장치가 연결됐는지 완전히 감사할 수 없다. 역할 생성 버튼은 보이지만 실제 생성 권한은 MUDO 전용 역할을 하나씩 만들며 검증한다.

### 5-2. 확인된 기존 역할

다음 기존 역할은 계정에서 확인됐다.

```text
ecsTaskExecutionRole
AWSServiceRoleForECS
AWSServiceRoleForAutoScaling
AWSServiceRoleForElasticLoadBalancing
AWSServiceRoleForRDS
```

- `AWSServiceRoleFor*` 역할은 AWS가 관리하는 서비스 연결 역할이므로 정책과 신뢰 관계를 직접 수정하지 않는다.
- 공용 `ecsTaskExecutionRole`은 MUDO 전용 역할 생성이 거부될 때만 제한적으로 재사용한다.
- MUDO 전용 역할을 만들 수 있으면 `mudo-prod-*` 이름으로 분리하고 필요한 최소 권한만 부여한다.

### 5-3. 역할 생성 권한 검증

첫 검증 대상으로 다음 역할을 만든다.

```text
Role: mudo-prod-ecs-instance-role
Trusted service: EC2
Policies:
  - AmazonEC2ContainerServiceforEC2Role
  - AmazonSSMManagedInstanceCore
```

공통 태그를 적용한다.

```text
Project=MUDO
Environment=prod
ManagedBy=team
CostCenter=groupware
```

이 역할 생성이 거부되면 ECS on EC2, SSM, GitHub OIDC 자동 배포를 현재 목표 구조대로 완성할 수 없다. 그 경우 기존 역할로 가능한 범위를 먼저 확인하고, 운영 배포는 EC2 직접 Docker 실행 방식의 Test Cell로 축소한다.

### 5-4. 현재 계정의 운영 원칙

- 별도 Ops/Admin 역할 전환 없이 `team04-02`로 콘솔 작업을 수행한다.
- Access Key를 만들지 않는다.
- 권한 오류를 우회하기 위해 기존 서비스 연결 역할을 확장하지 않는다.
- 권한이 없어 만들 수 없는 보안 장치는 구현 완료로 표시하지 않는다.
- GitHub OIDC Provider 또는 배포 역할 생성이 거부되면 `PRODUCTION_DEPLOY_ENABLED=false`를 유지하고 수동 배포만 사용한다.
- 운영 서비스로 전환하기 전에는 사람별 IAM·SSO와 최소 권한 역할 분리를 별도 개선 과제로 등록한다.

### 5-5. 공유 로그인 감사 보완

저장소에 운영 변경 이슈 템플릿을 두고 다음을 필수 입력으로 만든다.

```text
작업자 실명:
작업 시작/종료 시각:
사용 역할:
대상 리소스:
변경 전 상태:
변경 내용:
검증 결과:
롤백 방법:
```

---

## 6. 비용 한도와 확장 상한

자동 확장은 장애 대응 기능이지 무제한 결제를 허용하는 기능이 아니다.

### 6-1. AWS Billing 접근이 가능한 경우

1. AWS 콘솔 → **Billing and Cost Management**
2. **Budgets** → **Create budget**
3. Cost budget 선택
4. 월 예산 입력
5. 실제 비용 50%, 80%, 100% 알림 설정
6. 예상 비용 80%, 100% 알림도 설정
7. 이메일과 SNS 주제 `mudo-prod-cost-alerts` 연결

### 6-2. IAM 사용자가 Billing을 볼 수 없는 현재 제약

현재 팀은 루트·계정 관리자에게 Billing 설정이나 예산 알림 생성을 요청할 수 없다. 따라서 AWS Budgets와 실제 청구액 조회는 구축 완료 조건에서 제외하되, 다음 기술적 상한을 필수로 적용한다.

- ASG Max size 고정
- RDS Storage autoscaling 최대 용량 고정
- 학원별 API Rate Limit과 무거운 작업 동시성 제한
- 모니터링 EBS 용량·Prometheus·Loki 보존 기간 고정
- CloudWatch에서 접근 가능한 서비스별 사용량·리소스 지표 알람 설정

이 제약은 비용 통제가 완성됐다는 의미가 아니다. 실제 운영 계정으로 이전할 때 Billing Budget과 예상 비용 알림을 반드시 추가한다.

### 6-3. 기술적 최대값

아래 최대값을 문서에 숫자로 기록한다.

| 항목 | 시작값 예시 | 의미 |
|---|---:|---|
| ECS ASG Max size | 4 | 공격 때문에 EC2가 무한히 늘어나는 것 방지 |
| Capacity Provider targetCapacity | 85 | 약 15% 배치 여유 확보 |
| RDS Storage maximum | 200 GiB | 스토리지 비용 폭증 방지 |
| 학원별 API 요청량 | 부하 테스트로 확정 | 과도한 호출 제한 |
| 학원별 DB Pool | 공통 기본값 10 등 | RDS 연결 고갈 방지 |
| 무거운 작업 동시 수 | 공통 기본값 2 등 | 보고서·엑셀 공격 방지 |
| 모니터링 EBS 최대 | 초기 100 GiB | 로그 폭증으로 디스크 비용 증가 방지 |
| Prometheus 보존 | 초기 15일 | 메트릭 디스크 상한 관리 |
| Loki 보존 | 초기 30일 | 로그 디스크 상한 관리 |

> AWS Budgets 알림에는 지연이 있을 수 있으므로 WAF와 서비스별 최대값이 1차 방어선이다.

### 6-4. 초기 월 비용과 손익분기점

다음 값은 서울 리전 On-Demand, 월 730시간, 환율 `1 USD = 1,400원`, 낮은 초기 트래픽을 가정한 예산 범위다. 실제 구축 직전 AWS Pricing Calculator로 다시 계산한다.

| 항목 | 월 예상 범위 |
|---|---:|
| ECS용 `m7i.large` 2대와 EBS | 26~30만 원 |
| RDS MySQL Multi-AZ와 스토리지·백업 | 22~27만 원 |
| NAT, ALB, Public IPv4, 초기 트래픽 | 9~14만 원 |
| Valkey/Redis Primary + Replica | 약 4만 원 |
| WAF, CloudWatch, S3, ECR 기타 | 6~12만 원 |
| 전용 모니터링 EC2와 100 GiB EBS | 6~10만 원 |
| 합계, 부가세 제외 | 약 73~97만 원 |
| 합계, 부가세 10% 가정 | 약 80~107만 원 |

학원당 월 10만 원을 받는다면 AWS 비용만 넘기는 단순 손익분기점은 약 8~11개 학원이다. 인건비, 결제 수수료, 고객지원, 세금과 추가 개발비는 포함하지 않은 값이다.

```text
AWS 원가율 50% 목표 → 같은 비용 구간에서 약 16~22개 학원
AWS 원가율 30% 목표 → 같은 비용 구간에서 약 27~36개 학원
```

학원이 늘면 같은 비용 구간이 유지되지 않는다. ECS 예약 메모리, RDS Hikari 연결 예산, 로그량 때문에 EC2 또는 RDS가 추가되는 시점마다 학원당 원가와 손익분기점을 다시 계산한다.

---

## 7. VPC와 네트워크 구성

### 7-1. VPC 생성

1. AWS 콘솔 검색 → `VPC`
2. VPC dashboard → **Create VPC**
3. Resources to create → **VPC only**
4. Name tag → `mudo-prod-vpc`
5. IPv4 CIDR → `10.30.0.0/16`
6. IPv6 → 없음
7. Tenancy → Default
8. Create VPC
9. 생성 후 Actions → Edit VPC settings
10. DNS resolution과 DNS hostnames 활성화

### 7-2. 서브넷 6개 생성

VPC → **Subnets** → **Create subnet**에서 다음을 만든다.

| 이름 | AZ | CIDR | 용도 |
|---|---|---|---|
| `mudo-prod-public-2a` | ap-northeast-2a | `10.30.0.0/24` | ALB, NAT |
| `mudo-prod-public-2c` | ap-northeast-2c | `10.30.1.0/24` | ALB, NAT |
| `mudo-prod-app-2a` | ap-northeast-2a | `10.30.10.0/24` | ECS EC2, 초기 모니터링 EC2 |
| `mudo-prod-app-2c` | ap-northeast-2c | `10.30.11.0/24` | ECS EC2 |
| `mudo-prod-db-2a` | ap-northeast-2a | `10.30.20.0/24` | RDS |
| `mudo-prod-db-2c` | ap-northeast-2c | `10.30.21.0/24` | RDS |

앱과 DB 서브넷의 **Auto-assign public IPv4 address**는 끈다.

### 7-3. 인터넷 게이트웨이

1. VPC → **Internet gateways** → Create
2. 이름 `mudo-prod-igw`
3. 생성 후 Actions → Attach to a VPC
4. `mudo-prod-vpc` 선택

### 7-4. NAT Gateway

초기 비용 절감 모드에서는 NAT Gateway 1개로 시작한다.

1. VPC → **NAT gateways** → Create NAT gateway
2. 이름 `mudo-prod-nat-2a`
3. Subnet `mudo-prod-public-2a`
4. Connectivity type Public
5. Elastic IP → Allocate Elastic IP
6. Create

유료 서비스의 가용성을 강화할 때 `public-2c`에도 NAT Gateway를 만들고 앱 서브넷별 라우트를 같은 AZ NAT로 분리한다.

### 7-5. 라우트 테이블

다음 세 종류를 만든다.

```text
mudo-prod-public-rt
  0.0.0.0/0 → Internet Gateway

mudo-prod-app-rt
  0.0.0.0/0 → NAT Gateway

mudo-prod-db-rt
  외부 기본 경로 없음
```

- public route table에는 public-2a, public-2c를 연결한다.
- app route table에는 app-2a, app-2c를 연결한다.
- db route table에는 db-2a, db-2c를 연결한다.

### 7-6. 보안 그룹

#### `mudo-prod-alb-sg`

인바운드:

- TCP 80 / `0.0.0.0/0`
- TCP 443 / `0.0.0.0/0`

아웃바운드:

- 기본 전체 허용으로 시작하고 운영 안정화 후 축소 검토

#### `mudo-prod-ecs-instance-sg`

인바운드:

- TCP `32768-61000` / Source `mudo-prod-alb-sg`

SSH 22는 열지 않는다. ECS Exec 또는 SSM으로 점검한다.

#### `mudo-prod-rds-sg`

인바운드:

- MySQL TCP 3306 / Source `mudo-prod-ecs-instance-sg`

인터넷 IP나 `0.0.0.0/0`를 절대 입력하지 않는다.

#### `mudo-prod-monitoring-sg`

인바운드:

- TCP 3000 / Source `mudo-prod-alb-sg`: Grafana 화면
- TCP 9090 / Source `mudo-prod-ecs-instance-sg`: Alloy의 Prometheus remote write
- TCP 3100 / Source `mudo-prod-ecs-instance-sg`: Alloy의 Loki push

TCP 9090, 3100과 Alloy 디버그 포트 12345를 인터넷에 공개하지 않는다. Grafana 컨테이너는 같은 Docker 네트워크에서 Prometheus와 Loki에 접근하므로 이 두 포트를 호스트 전체에 공개할 필요도 없다.

### 7-7. 검증

- ALB용 public 서브넷만 인터넷 게이트웨이 경로가 있는지 확인
- 앱 서브넷에는 Public IP 자동 할당이 꺼져 있는지 확인
- DB 서브넷에는 NAT 및 인터넷 게이트웨이 기본 경로가 없는지 확인
- 보안 그룹에 SSH와 MySQL 인터넷 공개 규칙이 없는지 확인
- 모니터링 EC2에 Public IP가 없고 Grafana 3000만 ALB Security Group에서 접근 가능한지 확인
- Prometheus 9090과 Loki 3100이 ECS 인스턴스 Security Group 외에는 차단되는지 확인

---

## 8. 도메인과 ACM 인증서

### 8-1. 인증서 요청

1. 콘솔 검색 → `Certificate Manager`
2. 리전이 서울인지 확인
3. **Request a certificate**
4. Request a public certificate 선택
5. Domain names에 다음 두 개 입력

```text
ieum.store
*.ieum.store
```

6. Validation method → DNS validation
7. Key algorithm → RSA 2048 기본값
8. Request
9. 인증서 상세 화면 → **Create records in Route 53**
10. Status가 Issued가 될 때까지 기다린다.

Wildcard 인증서는 한 단계 하위 도메인만 보호한다. `academy-a.ieum.store`는 가능하지만 `api.academy-a.ieum.store`는 별도 인증서가 필요하다.

### 8-2. DNS 준비

Route 53 → Hosted zones에서 `<BASE_DOMAIN>` Hosted Zone이 존재하는지 확인한다.

ALB를 만든 뒤 다음 Alias를 추가한다.

```text
*.ieum.store → mudo-prod-alb
```

학원마다 DNS 레코드를 만들지 않고 wildcard 레코드 하나를 사용하되, ALB Host 규칙은 등록된 학원만 허용한다.

---

## 9. ECR, 로그 그룹, KMS, SSM

### 9-1. ECR 저장소

1. 콘솔 검색 → `Elastic Container Registry`
2. Private registry → Repositories → Create repository
3. 이름 `mudo-groupware-backend`
4. Tag immutability → Mutable을 사용하지 말고 가능하면 Immutable
5. Scan on push 활성화
6. Encryption → AES-256 또는 프로젝트 KMS 정책에 따라 KMS
7. Create

Lifecycle policy 예시:

- Git SHA 태그 이미지는 최근 배포·롤백에 필요한 수량을 남긴다.
- untagged 이미지는 7일 이후 제거한다.
- `develop` Docker Hub 정책과 운영 ECR 정책을 혼합하지 않는다.

### 9-2. CloudWatch 핵심 로그 그룹

CloudWatch → Logs → Log groups → Create log group:

```text
/mudo/prod/app
/mudo/prod/migration
/mudo/prod/monitoring
```

- `/mudo/prod/app`은 Loki 장애 시 긴급 전환할 수 있는 예비 그룹이며 앱의 평상시 기본 저장소는 Loki다.
- Migration Task와 모니터링 EC2의 CloudWatch Agent 로그는 CloudWatch에 유지한다.
- Retention은 처음 30일
- 민감정보가 장기 보존되지 않도록 애플리케이션 로그를 마스킹
- 운영 요구가 생기면 보존 기간을 늘리되 비용을 함께 검토

### 9-3. KMS 키

1. 콘솔 검색 → `KMS`
2. Customer managed keys → Create key
3. Symmetric / Encrypt and decrypt
4. Alias `alias/mudo-prod-ssm`
5. Key administrator → `team04-02`
6. Key users → `team04-02`, `mudo-prod-ecs-task-execution-role`

공유 IAM 사용자는 SecureString을 생성·변경할 때 암호화 권한이 필요하고, Task Execution Role은 Task 시작 시 복호화 권한이 필요하다. MUDO 전용 Task Execution Role 생성이 거부돼 기존 `ecsTaskExecutionRole`을 사용한다면 Key user도 해당 실제 역할로 바꾼다.

### 9-4. Parameter Store 경로

공통값:

```text
/mudo/prod/shared/AWS_REGION
/mudo/prod/shared/S3_FINANCE_BUCKET
/mudo/prod/shared/S3_STAFF_BUCKET
/mudo/prod/shared/SENTRY_DSN
/mudo/prod/shared/REDIS_HOST
/mudo/prod/shared/REDIS_PORT
/mudo/prod/shared/REDIS_PASSWORD
```

모니터링 값:

```text
/mudo/prod/monitoring/GRAFANA_ADMIN_USER
/mudo/prod/monitoring/GRAFANA_ADMIN_PASSWORD
/mudo/prod/monitoring/SLACK_WEBHOOK_URL
/mudo/prod/monitoring/PROMETHEUS_RETENTION
/mudo/prod/monitoring/LOKI_RETENTION
```

학원별 값:

```text
/mudo/prod/tenants/<tenant-code>/TENANT_ID
/mudo/prod/tenants/<tenant-code>/DB_URL
/mudo/prod/tenants/<tenant-code>/DB_USERNAME
/mudo/prod/tenants/<tenant-code>/DB_PASSWORD
/mudo/prod/tenants/<tenant-code>/MIGRATOR_DB_USERNAME
/mudo/prod/tenants/<tenant-code>/MIGRATOR_DB_PASSWORD
/mudo/prod/tenants/<tenant-code>/JWT_SECRET
/mudo/prod/tenants/<tenant-code>/FRONTEND_ORIGINS
/mudo/prod/tenants/<tenant-code>/GOOGLE_DRIVE_SERVICE_ACCOUNT_JSON_B64
/mudo/prod/tenants/<tenant-code>/GOOGLE_DRIVE_SHARED_DRIVE_ID
```

첫 학원 값 형식 예시:

```text
DB_URL=jdbc:mysql://<cell-1-endpoint>:3306/mudo_tenant_academy_a?useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=academy_a_app
MIGRATOR_DB_USERNAME=academy_a_migrator
FRONTEND_ORIGINS=https://academy-a.ieum.store
```

JWT Secret과 DB 비밀번호는 예시 문자열을 재사용하지 말고 비밀번호 관리 도구 또는 안전한 난수 생성기로 학원별 생성한다.

비밀번호, JWT, Sentry DSN, Google JSON, Grafana 비밀번호와 Slack Webhook은 SecureString으로 만들고 KMS 키 `alias/mudo-prod-ssm`을 선택한다.

Google JSON이 Standard Parameter 크기 제한을 넘으면 Advanced Parameter를 사용하거나 Secrets Manager로 옮긴다. 값 크기를 확인하지 않고 잘라서 저장하면 안 된다.

ECS가 Parameter Store 값을 환경변수로 주입한 뒤 값이 변경돼도 실행 중 Task에는 자동 반영되지 않는다. 값을 바꾼 후 ECS Service에서 **Force new deployment**를 실행한다.

---

## 10. RDS Cell 1 구성

### 10-1. DB 서브넷 그룹

1. RDS → Subnet groups → Create DB subnet group
2. 이름 `mudo-prod-rds-cell-1-subnet-group`
3. VPC `mudo-prod-vpc`
4. AZ `ap-northeast-2a`, `ap-northeast-2c`
5. Subnet `mudo-prod-db-2a`, `mudo-prod-db-2c`

### 10-2. RDS 생성

1. RDS → Databases → **Create database**
2. Creation method → Standard create
3. Engine → MySQL
4. Engine version → 코드와 검증한 MySQL 8.0 버전
5. Template:
   - 내부 검증은 Dev/Test
   - 유료 서비스는 Production
6. DB instance identifier → `mudo-prod-rds-cell-1`
7. Master username → `mudo_admin`
8. Credentials management → 팀 정책에 따라 자체 관리, 값은 안전한 관리 채널에 보관
9. DB instance class → 부하 테스트 결과 `<DB_CLASS>`
10. Storage type → gp3
11. 초기 용량 예시 → 50 GiB
12. Storage autoscaling 활성화
13. Maximum storage threshold 예시 → 200 GiB
14. Availability:
    - 내부 검증은 Single-AZ 가능
    - 실제 유료 운영은 Multi-AZ 선택
15. VPC → `mudo-prod-vpc`
16. DB subnet group → 앞에서 만든 그룹
17. Public access → No
18. VPC security group → `mudo-prod-rds-sg`
19. Port → 3306
20. Initial database name은 비워도 된다. 학원별 DB를 별도로 생성한다.
21. Backup retention → 최소 7일
22. Encryption 활성화
23. KMS 키 선택
24. Deletion protection 활성화
25. Performance Insights 또는 Database Insights를 사용할 수 있으면 활성화
26. Create database

### 10-3. 시작 클래스 선택 기준

`db.t*.micro`를 다중 학원 운영의 고정 답으로 사용하지 않는다.

- T 계열은 CPU 크레딧 때문에 지속 부하에서 성능 판단이 어려울 수 있다.
- 내부 검증은 작은 클래스가 가능하다.
- 실제 고객 운영은 부하 테스트 후 메모리와 커넥션을 감당하는 클래스를 선택한다.
- 정상 피크에서 CPU 70% 이하, 연결 수 70% 이하, 충분한 FreeableMemory를 초기 목표로 삼는다.

### 10-4. 학원별 DB와 계정 생성

ECS Task가 아니라 관리용 임시 접속 환경에서 마스터 계정으로 실행한다.

```sql
CREATE DATABASE mudo_tenant_academy_a
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER 'academy_a_app'@'%'
  IDENTIFIED BY '<강한 앱 비밀번호>';

GRANT SELECT, INSERT, UPDATE, DELETE
  ON mudo_tenant_academy_a.*
  TO 'academy_a_app'@'%';

ALTER USER 'academy_a_app'@'%'
  WITH MAX_USER_CONNECTIONS 10;

CREATE USER 'academy_a_migrator'@'%'
  IDENTIFIED BY '<강한 마이그레이션 비밀번호>';

GRANT SELECT, INSERT, UPDATE, DELETE,
      CREATE, ALTER, INDEX, DROP, REFERENCES
  ON mudo_tenant_academy_a.*
  TO 'academy_a_migrator'@'%';

FLUSH PRIVILEGES;
```

`MAX_USER_CONNECTIONS`는 모든 학원에 같은 기본값을 적용하고 RDS 전체 연결 예산에 따라 결정한다. 특정 학원만 반복적으로 연결을 고갈시키면 결제 플랜을 올리는 대신 Rate Limit, 쿼리 개선, 전용 Cell 이관을 검토한다.

### 10-5. RDS 연결 예산

RDS Cell마다 다음 표를 운영한다.

```text
RDS max_connections
- 관리자·마이그레이션·장애 대응 여유 30%
= 학원 앱에 배분 가능한 연결 70%
```

모든 학원의 Hikari `maximumPoolSize` 합뿐 아니라 신규 학원과 순차 Rolling Update 중 추가되는 Task 한 개의 Pool까지 앱 배분 가능 연결 수를 넘지 않게 한다. 상세 계산은 13-5를 따른다.

초기값 예시이며 부하 테스트로 조정한다.

| 런타임 프로필 | Hikari maximumPoolSize | 무거운 작업 동시 수 |
|---|---:|---:|
| shared-default | 10 | 2 |

### 10-6. RDS Proxy 결정

초기에는 학원별 Hikari 제한으로 시작한다. 다음 조건에서 RDS Proxy를 검토한다.

- 연결 생성·해제가 급증한다.
- 장애 조치 시 연결 복구가 느리다.
- 전체 연결 수가 RDS 한계에 접근한다.

학원마다 다른 DB 사용자를 쓰면 RDS Proxy 연결 풀이 사용자별 작은 풀로 나뉘어 효율이 낮아질 수 있다. 따라서 처음부터 필수 리소스로 만들지 않고 실제 연결 압력이 확인된 후 도입한다.

### 10-7. RDS 확장 순서

```text
1. 느린 쿼리·인덱스·커넥션 누수 개선
2. 같은 RDS의 인스턴스 클래스 상향
3. 조회 부하가 주원인이면 Read Replica 추가
4. 학원 수·장애 영향·비용이 커지면 RDS Cell 2 생성
5. 신규 학원을 Cell 2에 배치하고 필요 시 기존 학원 이관
```

일반 RDS의 Read Replica는 자동으로 늘어나지 않는다. 쓰기 부하는 Read Replica로 해결되지 않는다.

### 10-8. 백업 복구 시험

1. RDS → Databases → Cell 1 선택
2. Actions → Restore to point in time
3. 테스트용 인스턴스 이름 지정
4. 복구 완료 후 학원별 DB와 주요 데이터 확인
5. 시험 결과에 RTO와 RPO 기록
6. 테스트 DB 삭제

삭제 전에 대상이 테스트 DB인지 두 번 확인한다.

---

## 11. ECS IAM 역할

### 11-1. ECS EC2 인스턴스 역할

IAM → Roles → Create role:

- Use case → EC2
- 이름 → `mudo-prod-ecs-instance-role`
- 정책 → `AmazonEC2ContainerServiceforEC2Role`
- SSM 점검이 필요하면 `AmazonSSMManagedInstanceCore` 추가

이 역할은 컨테이너 애플리케이션의 S3 권한으로 사용하지 않는다.

### 11-2. Task Execution Role

IAM → Roles → Create role:

- Use case → Elastic Container Service Task
- 이름 → `mudo-prod-ecs-task-execution-role`
- 정책 → `AmazonECSTaskExecutionRolePolicy`

추가 인라인 정책:

- `/mudo/prod/shared/*`와 `/mudo/prod/tenants/*`에 대한 `ssm:GetParameters`
- `alias/mudo-prod-ssm` 실제 키 ARN에 대한 `kms:Decrypt`
- Resource는 현재 계정과 리전 ARN으로 제한

이 역할은 ECR 이미지 Pull, CloudWatch Logs 전송, SSM 시크릿 주입에 사용한다.

### 11-3. 학원별 Task Role

학원마다 역할을 하나 만든다.

```text
mudo-prod-tenant-academy-a-task-role
```

이 역할에는 해당 학원의 S3 Prefix만 허용한다.

```text
s3://mudo-prod-finance-<suffix>/tenants/academy-a/*
s3://mudo-prod-staff-<suffix>/tenants/academy-a/*
```

컨테이너가 다른 학원의 Prefix에 접근하려 하면 IAM에서 거부되어야 한다.

### 11-4. 모니터링 EC2 역할

IAM → Roles → Create role:

- Use case → EC2
- 이름 → `mudo-prod-monitoring-ec2-role`
- `AmazonSSMManagedInstanceCore`
- CloudWatch 메트릭·로그 조회에 필요한 읽기 전용 최소 권한
- CloudWatch Agent가 메모리·디스크 지표와 자체 로그를 전송할 최소 권한
- `/mudo/prod/monitoring/*`에 대한 `ssm:GetParameters`
- `alias/mudo-prod-ssm`에 대한 `kms:Decrypt`

CloudWatch 데이터 소스 읽기 정책에는 최소한 `cloudwatch:GetMetricData`, `cloudwatch:ListMetrics`, `cloudwatch:GetMetricStatistics`, `logs:StartQuery`, `logs:GetQueryResults`, `logs:StopQuery`, `logs:DescribeLogGroups`와 대시보드 변수에 필요한 제한된 `Describe` 권한을 포함한다. 쓰기·삭제 권한은 CloudWatch Agent가 실제로 사용하는 Namespace와 로그 그룹에만 별도 허용한다.

Grafana CloudWatch 데이터 소스는 이 인스턴스 역할을 사용한다. Access Key를 서버나 Grafana 설정 파일에 저장하지 않는다. 컨테이너에서 인스턴스 메타데이터 자격 증명을 사용해야 하므로 EC2 Metadata options는 `IMDSv2 required`, hop limit `2`로 설정하고 Grafana 외 컨테이너를 임의로 추가하지 않는다.

### 11-5. Alloy Task Execution Role

Alloy 이미지를 ECR에서 받고 자체 시작 로그를 CloudWatch로 보낼 최소 실행 역할 `mudo-prod-alloy-task-execution-role`을 만든다. Alloy Task Role에는 AWS 리소스 변경 권한을 주지 않는다.

Alloy는 Docker Socket을 읽기 때문에 해당 호스트 컨테이너 정보를 볼 수 있다. 공식 이미지를 Digest로 고정하고, 설정 변경 PR 검토와 이미지 스캔을 필수화한다. Alloy UI 12345는 외부에 공개하지 않는다.

### 11-6. GitHub Actions 배포 역할

1. IAM → Identity providers → Add provider
2. Provider type → OpenID Connect
3. URL → `https://token.actions.githubusercontent.com`
4. Audience → `sts.amazonaws.com`
5. IAM → Roles → Create role → Web identity
6. 이름 `mudo-prod-ci-deploy-role`

Trust policy 조건은 반드시 다음 범위로 제한한다.

```text
repo:<GITHUB_OWNER>/<GITHUB_REPO>:ref:refs/heads/main
```

배포 역할 권한:

- ECR 로그인·Push
- ECS Task Definition 등록
- 지정 Cluster의 ECS Service 업데이트·조회
- 배포에 필요한 `iam:PassRole`
- 마이그레이션 Task 실행 권한
- CloudWatch Logs 조회 또는 배포 상태 확인에 필요한 최소 권한

VPC, RDS 삭제, IAM 사용자 생성 같은 권한은 주지 않는다.

---

## 12. ECS Cluster와 EC2 Capacity Provider

### 12-1. ECS Cluster 생성

1. 콘솔 검색 → `Elastic Container Service`
2. ECS → Clusters → **Create cluster**
3. Cluster name → `mudo-prod-cluster`
4. Infrastructure → Amazon EC2 instances 선택
5. 새 Auto Scaling Group 생성 방식 선택
6. Operating system → Amazon Linux 2023 ECS-optimized AMI
7. CPU architecture → `<CPU_ARCH>`
8. EC2 instance type → `<INSTANCE_TYPE>`
9. EC2 instance role → `mudo-prod-ecs-instance-role`
10. Desired capacity:
    - 내부 검증 `1`
    - 유료 운영 `2`
11. Minimum capacity:
    - 내부 검증 `1`
    - 유료 운영 `2`
12. Maximum capacity → `<ASG_MAX>`
13. VPC → `mudo-prod-vpc`
14. Subnets → `mudo-prod-app-2a`, `mudo-prod-app-2c`
15. Security group → `mudo-prod-ecs-instance-sg`
16. Auto-assign public IP → Off
17. Container Insights with enhanced observability 활성화
18. Create

콘솔이 CloudFormation Stack, Launch Template, ASG, Capacity Provider를 만드는 동안 완료될 때까지 기다린다.

### 12-2. ASG 확인

EC2 → Auto Scaling Groups에서 생성된 그룹을 연다.

- Min/Desired/Max 값 확인
- Health check type 확인
- 여러 인스턴스를 사용할 때 app-2a와 app-2c가 모두 지정됐는지 확인
- 인스턴스가 ECS Cluster의 Infrastructure 탭에 등록되는지 확인

### 12-3. Capacity Provider 설정

ECS → Clusters → `mudo-prod-cluster` → Infrastructure → Capacity providers에서 확인 또는 생성한다.

- Managed scaling → On
- Managed termination protection 또는 scaling protection → On
- Managed draining → On
- Target capacity → `85`
- Minimum scaling step size → `1`
- Maximum scaling step size → 초기 `1` 또는 부하에 맞는 값
- Instance warm-up → 앱 이미지 Pull과 ECS 등록 시간을 고려해 설정

Target capacity 85는 약 15%의 배치 여유를 의도한다. 실제 메모리 여유와 정확히 같은 값은 아니므로 CloudWatch로 검증한다.

### 12-4. 인스턴스 선택 원칙

- 한 종류의 인스턴스로 시작해 배치 계산을 단순하게 한다.
- Java 애플리케이션은 메모리가 먼저 부족해질 가능성이 높으므로 메모리 비율을 우선 본다.
- Graviton을 사용하려면 Docker 이미지와 모든 네이티브 의존성이 ARM64를 지원하는지 CI에서 검증한다.
- 검증 전에는 X86_64를 사용해 아키텍처 문제를 줄인다.
- 운영 기본 용량 전체를 Spot으로 구성하지 않는다.

---

## 13. 결제 플랜과 공통 Task 자원 정책

결제 플랜과 인프라 자원 프로필을 분리한다.

| 구분 | 결정 대상 | 변경 방식 |
|---|---|---|
| 결제 플랜 | 유료 엔드포인트·기능 접근, 월 호출량, 저장공간, 지원 수준 | 애플리케이션의 구독·Entitlement 데이터 변경 |
| 런타임 프로필 | ECS CPU·메모리, Tomcat, Hikari, 비동기 Executor, 무거운 작업 동시 수 | 인프라 매니페스트 변경과 부하 테스트 후 순차 배포 |

Basic·Standard·Premium을 Task 크기로 사용하지 않는다. 모든 학원은 기본적으로 `shared-default` 런타임 프로필을 사용한다. 다음 값은 최초 가설이며 실제 API·보고서·파일 처리 부하 테스트 결과로 조정한다.

| 런타임 프로필 | CPU 예약값 | memoryReservation | memory 최대 | DB Pool |
|---|---:|---:|---:|---:|
| shared-default | 1024 units | 1536 MiB | 2048 MiB | 10 |

| 런타임 프로필 | Tomcat 최대 스레드 | Tomcat 대기열 | 비동기 최대 스레드 | 비동기 대기열 | 무거운 작업 동시 수 |
|---|---:|---:|---:|---:|---:|
| shared-default | 60 | 100 | 4 | 40 | 2 |

동일 자원 분배는 인스턴스 전체를 학원 수로 매 순간 `100/N` 분할한다는 뜻이 아니다. 각 학원 Task에 같은 예약값과 상한을 주고, ECS는 예약값으로 배치한다. CPU가 남으면 다른 Task가 버스트할 수 있지만 메모리와 Hikari 연결은 정해진 상한을 넘지 않는다.

유료 엔드포인트가 일반 API보다 무거운 경우에도 Task 크기를 플랜별로 키우지 않는다. 대신 엔드포인트별 Rate Limit, 월 쿼터, 별도 bounded Executor와 동시 실행 제한으로 공용 Cell을 보호한다. 플랜 업그레이드는 Task 재배포 없이 Entitlement 변경만으로 반영한다.

기존 EC2/ECS 구조는 유지한다.

- 학원별 ECS Service와 Task 1개를 유지해 장애·배포·로그·DB 계정을 격리한다.
- 동일한 `shared-default` Task를 같은 EC2 ASG에 `binpack`으로 배치해 유휴 자원을 줄인다.
- 새 학원의 동일 규격 Task를 기존 EC2에 배치할 수 없을 때 Capacity Provider가 EC2를 추가한다.
- 결제 플랜별 EC2 풀이나 인스턴스를 따로 만들지 않는다.
- 특정 학원만 반복적으로 공용 Cell에 영향을 주면 상품 플랜과 별개인 운영 조치로 전용 Task, 별도 Cell 또는 별도 런타임 프로필을 검토한다.

유료 엔드포인트 권한 판단은 인증된 학원과 서버가 조회한 구독 정보를 입력으로 Application 또는 Domain 정책에서 수행한다. `global/` 필터나 클라이언트 헤더가 Basic·Standard·Premium 접근 여부를 직접 결정하지 않는다.

인스턴스 배치 가능량은 다음처럼 계산한다.

```text
배치 가능 CPU units = EC2 vCPU × 1024 - OS/ECS/Alloy 예약 CPU
배치 가능 메모리 = EC2 실제 메모리 - OS/ECS/Alloy/배포 여유 메모리

수용 가능 학원 수
= min(
    배치 가능 CPU / shared-default CPU 예약값,
    배치 가능 메모리 / shared-default memoryReservation,
    RDS 앱 배분 가능 연결 / shared-default DB Pool
  )
```

CPU·메모리만 남아도 RDS 연결 예산이 부족하면 해당 RDS Cell에 신규 학원을 넣지 않는다.

### 13-1. CPU 동작 원칙

- EC2 launch type에서 컨테이너 `cpu`는 예약과 CPU share 역할을 한다.
- Task-level CPU를 낮은 고정값으로 강제하면 유휴 CPU 버스트가 제한될 수 있다.
- 초기에는 컨테이너 CPU 예약값을 사용하고 Task-level CPU hard quota는 두지 않는다.
- 두 학원만 바쁘면 남는 CPU를 더 사용할 수 있다.
- 세 학원이 모두 바쁘면 CPU share 비율에 따라 경쟁한다.
- CPU가 남는다고 새로운 학원을 실제 사용률만 보고 배치하지 않는다. 예약값으로 배치한다.

### 13-2. 메모리 동작 원칙

- `memoryReservation`은 ECS 배치 기준이 되는 최소 예약량이다.
- `memory`는 컨테이너가 넘을 수 없는 최대값이다.
- 메모리는 CPU처럼 안전하게 비율 회수되지 않는다.
- 최대값 초과 시 OOM 종료가 발생할 수 있다.
- 인스턴스 전체 메모리의 10~20%는 OS, ECS Agent, 배포 교체용으로 남긴다.

### 13-3. JVM 설정

컨테이너 메모리 최대값보다 JVM heap을 작게 둔다.

예시:

```text
JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=65.0 -XX:InitialRAMPercentage=25.0
```

heap 외에도 Metaspace, Thread stack, Direct buffer가 메모리를 사용하므로 heap을 컨테이너 최대값과 같게 잡지 않는다.

### 13-4. 앱 스레드와 DB 연결의 관계

Tomcat 스레드, 비동기 작업 스레드, Hikari 연결은 서로 다른 자원이다.

- Tomcat 스레드는 동시에 처리할 HTTP 요청 수를 제한한다.
- Hikari 연결은 동시에 DB를 사용할 수 있는 요청 수를 제한한다.
- 비동기 Executor는 알림·파일·보고서 같은 백그라운드 작업의 동시 수를 제한한다.
- WebSocket 연결은 항상 Tomcat 작업 스레드 하나를 점유하지는 않지만 세션 메모리, 메시지 처리 스레드와 Redis 연결을 사용한다.

예를 들어 `shared-default`의 Tomcat 최대값이 60이고 Hikari가 10이면 요청 60개가 모두 DB를 동시에 사용하는 것이 아니다. 최대 10개만 DB 연결을 빌리고 나머지는 짧게 기다린다. 이 대기가 길어지면 전체 요청 스레드가 묶이므로 Hikari connection timeout과 API timeout을 짧고 유한하게 둔다.

스레드 수를 크게 잡는다고 처리량이 비례해 늘지 않는다. 오히려 DB 연결 대기, 문맥 전환, 스레드 스택 메모리가 늘어난다. 따라서 다음 원칙을 적용한다.

1. Tomcat, 비동기 Executor와 무거운 작업 Executor는 모두 상한과 유한한 대기열을 둔다.
2. 대기열이 찼을 때 무제한 적재하지 않고 API 특성에 따라 429 또는 503을 반환한다.
3. 보고서·대량 업로드·대량 알림은 일반 요청과 같은 Executor에서 실행하지 않는다.
4. 학원별 Hikari 상한 합계는 RDS Cell 연결 예산 안에 둔다.
5. Tomcat·비동기 스레드가 사용하는 메모리는 공통 런타임 프로필 부하 테스트 후 `memoryReservation`과 `memory`에 포함한다.
6. EC2에 임의의 “총 스레드 100개” 같은 고정 한도를 만들지 않는다. 서로 다른 JVM의 스레드는 ECS가 직접 예약하지 않으므로 CPU·메모리 예약, 앱별 상한, 실측 지표로 관리한다.

### 13-5. RDS 연결 예산과 배포 여유

MySQL 연결은 서버 측 연결 처리 자원과 메모리를 소비한다. 앱 Task가 늘어날 때 Hikari Pool도 함께 늘기 때문에 앱 개수는 RDS 연결 예산으로 제한해야 한다.

```text
RDS 앱 배분 가능 연결 = floor(RDS max_connections × 0.70)

신규 학원 수용 조건 =
  현재 실행 중인 모든 학원의 Hikari 상한 합
  + 신규 학원의 Hikari 상한
  + 순차 Rolling Update 중 한 개 학원의 추가 Hikari 상한
  <= RDS 앱 배분 가능 연결
```

나머지 30%는 관리자 접속, Migration Task, 모니터링, 장애·재연결 폭주에 사용한다. 모든 학원을 동시에 배포하면 여러 Task가 한꺼번에 두 배로 떠 연결 예산을 초과할 수 있으므로 학원별 ECS Service를 반드시 순차 배포한다.

`minimum-idle`을 `maximum-pool-size`와 같게 두면 시작 직후 연결을 모두 확보할 수 있다. 초기에는 작은 `minimum-idle` 또는 Hikari 기본 동작을 부하 테스트로 검증하되, 용량 계산은 항상 `maximum-pool-size` 최악값을 사용한다.

### 13-6. 런타임 프로필과 Cell 매니페스트 관리

콘솔에서 학원마다 값을 손으로 다르게 입력하지 않는다. 시크릿을 제외한 다음 세 파일을 저장소에서 버전 관리하고 Task Definition 생성 및 입점 검사 스크립트의 유일한 입력으로 사용한다.

```text
infra/runtime-profiles.yml  공통 CPU·메모리·스레드·DB Pool 상한
infra/tenants.yml           학원 코드, 결제 플랜, 런타임 프로필, 배치 Cell, ECS Service
infra/cells.yml             Cell별 ECS/RDS 식별자와 연결 안전 비율
```

`infra/runtime-profiles.yml` 예시:

```yaml
runtime_profiles:
  shared-default:
    cpu: 1024
    memory_reservation_mib: 1536
    memory_limit_mib: 2048
    tomcat_max_threads: 60
    tomcat_queue_capacity: 100
    hikari_max_pool_size: 10
    hikari_connection_timeout_ms: 3000
    async_core_pool_size: 2
    async_max_pool_size: 4
    async_queue_capacity: 40
    heavy_job_max_concurrency: 2
```

`infra/tenants.yml` 예시:

```yaml
tenants:
  - code: academy-a
    enabled: false
    billing_plan: basic
    runtime_profile: shared-default
    cell: cell-1
    service: mudo-prod-svc-academy-a
    task_family: mudo-prod-tenant-academy-a
    health_url: https://academy-a.ieum.store/actuator/health
    s3_bucket: mudo-prod-staff-<account-suffix>
```

`enabled: false`인 학원은 매니페스트 검증만 하고 실제 배포 대상에서 제외한다. DB·SSM·S3·Target Group·ECS Service와 격리 시험이 모두 끝난 뒤에만 `true`로 변경한다.

`infra/cells.yml` 예시:

```yaml
cells:
  cell-1:
    ecs_cluster: mudo-prod-cluster
    capacity_provider: mudo-prod-capacity-provider
    rds_identifier: mudo-prod-rds-cell-1
    rds_app_connection_ratio: 0.70
    ecs_app_capacity_ratio: 0.80
    ecs_registered_cpu: null
    ecs_registered_memory_mib: null
    rds_max_connections: null
```

마지막 세 값은 AWS 리소스를 생성한 뒤 실제 등록 용량과 RDS `max_connections`를 확인해 입력한다. `null`인 동안 실제 운영 배포는 실패하며, 예상값으로 임의 입력해 잠금을 우회하지 않는다.

DB 비밀번호, JWT, Redis 비밀번호 같은 시크릿은 이 파일에 넣지 않고 SSM Parameter Store에 둔다.

### 13-7. 신규 학원 입점 검사

두 번째 학원부터는 Task Definition을 만들기 전에 자동 검사 스크립트를 실행한다.

```text
1. tenants.yml에서 Cell별 `runtime_profile` 수를 집계한다.
2. runtime-profiles.yml로 CPU·memoryReservation·Hikari 상한 합계를 계산한다.
3. AWS에서 해당 RDS의 실제 max_connections와 ECS/ASG 설정을 조회한다.
4. 신규 학원과 한 번의 Rolling Update 추가 Task까지 포함한다.
5. CPU, 메모리, RDS 연결 중 하나라도 안전 한도를 넘으면 생성을 중단한다.
6. ECS만 부족하면 ASG 최대값·비용을 검토하고, RDS가 부족하면 사양 상향 또는 새 Cell을 선택한다.
```

입점 판단식은 다음과 같다.

```text
수용 가능 여부 =
  ECS 예약 CPU 충족
  AND ECS 예약 메모리 충족
  AND RDS 정상 Task Pool 합 + 신규 Pool + 배포 Surge Pool <= RDS 앱 연결 예산
  AND ALB Target Group·Listener Rule 등 서비스 Quota 충족
```

Tomcat 스레드 합계를 RDS 연결처럼 단순 합산해 EC2 입점 기준으로 사용하지 않는다. 대신 공통 최대 스레드 상태에서 부하 테스트한 CPU·메모리 결과가 해당 Task 예약량 안에 드는지 검증한다.

### 13-8. 애플리케이션 환경변수 규칙

Spring Boot 기본 설정은 표준 환경변수 이름을 사용하고, 프로젝트 전용 제한은 `APP_` 접두사를 사용한다.

```text
SERVER_TOMCAT_THREADS_MAX
SERVER_TOMCAT_THREADS_MAX_QUEUE_CAPACITY
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE
SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT
APP_ASYNC_CORE_POOL_SIZE
APP_ASYNC_MAX_POOL_SIZE
APP_ASYNC_QUEUE_CAPACITY
APP_HEAVY_JOB_MAX_CONCURRENCY
```

`APP_*` 값은 이름만 Task Definition에 넣어서는 동작하지 않는다. Spring 설정 클래스와 별도 bounded Executor, 거부 정책, 무거운 작업 Semaphore 또는 동등한 제한 로직을 먼저 구현해야 한다.

`TENANT_PLAN`은 로그·Prometheus 태그와 사용량 집계를 위한 메타데이터로 사용할 수 있지만 유료 엔드포인트 권한의 최종 근거가 아니다. 권한 판단은 서버가 조회한 최신 구독·Entitlement 데이터를 사용한다.

---

## 14. 첫 학원 Task Definition

### 14-1. Task Definition 생성

ECS → Task definitions → Create new task definition with JSON을 선택한다.

아래 예시는 구조를 설명하는 템플릿이다. ARN과 값은 실제 값으로 교체한다.

```json
{
  "family": "mudo-prod-tenant-academy-a",
  "networkMode": "bridge",
  "requiresCompatibilities": ["EC2"],
  "executionRoleArn": "arn:aws:iam::<ACCOUNT_ID>:role/mudo-prod-ecs-task-execution-role",
  "taskRoleArn": "arn:aws:iam::<ACCOUNT_ID>:role/mudo-prod-tenant-academy-a-task-role",
  "containerDefinitions": [
    {
      "name": "app",
      "image": "<ACCOUNT_ID>.dkr.ecr.ap-northeast-2.amazonaws.com/mudo-groupware-backend:<GIT_SHA>",
      "essential": true,
      "cpu": 1024,
      "memoryReservation": 1536,
      "memory": 2048,
      "portMappings": [
        {
          "containerPort": 8080,
          "hostPort": 0,
          "protocol": "tcp"
        }
      ],
      "dockerLabels": {
        "mudo.observe": "true",
        "mudo.tenant": "academy-a",
        "mudo.service": "backend",
        "mudo.environment": "prod",
        "mudo.deployment_sha": "<GIT_SHA>",
        "prometheus.scrape": "true"
      },
      "environment": [
        {"name": "SPRING_PROFILES_ACTIVE", "value": "prod"},
        {"name": "TENANT_ID", "value": "academy-a"},
        {"name": "TENANT_PLAN", "value": "basic"},
        {"name": "MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE", "value": "health,prometheus"},
        {"name": "MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS", "value": "never"},
        {"name": "SERVER_TOMCAT_THREADS_MAX", "value": "60"},
        {"name": "SERVER_TOMCAT_THREADS_MAX_QUEUE_CAPACITY", "value": "100"},
        {"name": "SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE", "value": "10"},
        {"name": "SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT", "value": "3000"},
        {"name": "APP_ASYNC_CORE_POOL_SIZE", "value": "2"},
        {"name": "APP_ASYNC_MAX_POOL_SIZE", "value": "4"},
        {"name": "APP_ASYNC_QUEUE_CAPACITY", "value": "40"},
        {"name": "APP_HEAVY_JOB_MAX_CONCURRENCY", "value": "2"},
        {"name": "S3_FINANCE_BUCKET", "value": "mudo-prod-finance-<account-suffix>"},
        {"name": "S3_STAFF_BUCKET", "value": "mudo-prod-staff-<account-suffix>"},
        {"name": "JAVA_TOOL_OPTIONS", "value": "-XX:MaxRAMPercentage=65.0 -XX:InitialRAMPercentage=25.0"}
      ],
      "secrets": [
        {"name": "SPRING_DATASOURCE_URL", "valueFrom": "arn:aws:ssm:ap-northeast-2:<ACCOUNT_ID>:parameter/mudo/prod/tenants/academy-a/DB_URL"},
        {"name": "SPRING_DATASOURCE_USERNAME", "valueFrom": "arn:aws:ssm:ap-northeast-2:<ACCOUNT_ID>:parameter/mudo/prod/tenants/academy-a/DB_USERNAME"},
        {"name": "SPRING_DATASOURCE_PASSWORD", "valueFrom": "arn:aws:ssm:ap-northeast-2:<ACCOUNT_ID>:parameter/mudo/prod/tenants/academy-a/DB_PASSWORD"},
        {"name": "JWT_SECRET", "valueFrom": "arn:aws:ssm:ap-northeast-2:<ACCOUNT_ID>:parameter/mudo/prod/tenants/academy-a/JWT_SECRET"},
        {"name": "CORS_ALLOWED_ORIGINS", "valueFrom": "arn:aws:ssm:ap-northeast-2:<ACCOUNT_ID>:parameter/mudo/prod/tenants/academy-a/FRONTEND_ORIGINS"},
        {"name": "WEBSOCKET_ALLOWED_ORIGINS", "valueFrom": "arn:aws:ssm:ap-northeast-2:<ACCOUNT_ID>:parameter/mudo/prod/tenants/academy-a/FRONTEND_ORIGINS"},
        {"name": "GOOGLE_DRIVE_SERVICE_ACCOUNT_JSON_B64", "valueFrom": "arn:aws:ssm:ap-northeast-2:<ACCOUNT_ID>:parameter/mudo/prod/tenants/academy-a/GOOGLE_DRIVE_SERVICE_ACCOUNT_JSON_B64"},
        {"name": "GOOGLE_DRIVE_SHARED_DRIVE_ID", "valueFrom": "arn:aws:ssm:ap-northeast-2:<ACCOUNT_ID>:parameter/mudo/prod/tenants/academy-a/GOOGLE_DRIVE_SHARED_DRIVE_ID"},
        {"name": "SENTRY_DSN", "valueFrom": "arn:aws:ssm:ap-northeast-2:<ACCOUNT_ID>:parameter/mudo/prod/shared/SENTRY_DSN"},
        {"name": "REDIS_HOST", "valueFrom": "arn:aws:ssm:ap-northeast-2:<ACCOUNT_ID>:parameter/mudo/prod/shared/REDIS_HOST"},
        {"name": "REDIS_PORT", "valueFrom": "arn:aws:ssm:ap-northeast-2:<ACCOUNT_ID>:parameter/mudo/prod/shared/REDIS_PORT"},
        {"name": "REDIS_PASSWORD", "valueFrom": "arn:aws:ssm:ap-northeast-2:<ACCOUNT_ID>:parameter/mudo/prod/shared/REDIS_PASSWORD"}
      ],
      "healthCheck": {
        "command": ["CMD-SHELL", "wget -qO- http://localhost:8080/actuator/health || exit 1"],
        "interval": 30,
        "timeout": 5,
        "retries": 3,
        "startPeriod": 60
      },
      "logConfiguration": {
        "logDriver": "json-file",
        "options": {
          "max-size": "20m",
          "max-file": "5"
        }
      }
    }
  ]
}
```

Docker 실행 이미지에 `wget`이 없다면 health check 명령을 `curl` 또는 애플리케이션에 포함된 도구로 바꾼다. 실제 이미지에서 명령이 존재하는지 로컬에서 먼저 확인한다.

`json-file` 로그는 호스트 디스크를 무한히 사용하지 않도록 `max-size`와 `max-file`을 반드시 설정한다. Alloy가 Docker API에서 stdout 로그를 읽어 Loki로 보내며, Loki 장애가 길어져 로컬 회전 범위를 넘은 로그는 유실될 수 있다. 법적·회계상 반드시 보존할 이벤트는 로그만 믿지 말고 RDS 감사 테이블이나 S3 감사 원장에 별도로 기록한다.

Actuator는 기존 앱 포트 8080을 사용한다. Alloy는 같은 호스트에서 컨테이너 Bridge IP의 8080으로 직접 수집하고, 외부 사용자는 아래 ALB 차단 규칙 때문에 `/actuator/prometheus`에 접근할 수 없어야 한다.

### 14-2. 브리지 네트워크 선택 이유

- `hostPort: 0`으로 여러 학원 Task가 한 EC2의 서로 다른 동적 포트를 사용한다.
- ALB가 ECS를 통해 실제 인스턴스 ID와 동적 포트를 등록한다.
- `awsvpc`보다 ENI 개수 때문에 Task 밀도가 먼저 막힐 가능성이 낮다.

대신 Task별 보안 그룹을 사용할 수 없는 트레이드오프가 있다. 더 강한 네트워크 격리가 필요해지면 `awsvpc`와 ENI trunking을 검토한다.

---

## 15. ALB와 학원별 Target Group

### 15-1. 첫 학원 Target Group

1. EC2 콘솔 → Target Groups → Create target group
2. Target type → Instances
3. 이름 → `mudo-prod-tg-academy-a`
4. Protocol HTTP / Port 8080
5. VPC → `mudo-prod-vpc`
6. Health check protocol → HTTP
7. Health check path → `/actuator/health`
8. Success codes → `200`
9. **Advanced health check settings**(AWS 기본값을 그대로 두지 말 것 — 2026-08-15 실측 기준, 기본값(interval 30초·healthy threshold 5회·unhealthy threshold 2회·deregistration delay 300초)으로는 테넌트 1개 롤링 배포에 약 8분이 걸린다. 아래 값으로 조정하면 약 2~3분대로 줄어든다):
   - Health check interval → `10`초 (기본 30초)
   - Healthy threshold → `3`회 = 30초 (기본 5회=150초)
   - Unhealthy threshold → `6`회 = 60초 (기본 2회=60초와 **동일하게 유지** — interval을 줄인 만큼 횟수를 올려서 장애 허용 시간(60초)은 원래대로 지킨다. 2026-08-15에 unhealthy threshold를 2회로 뒀다가 배포 중 순간적 지연(신구 Task가 동시에 RDS 커넥션을 다투는 구간 등)만으로 정상 Task가 20초 만에 강제 종료되는 사고가 실제로 있었다.)
   - Target group attributes → `deregistration_delay.timeout_seconds` → `90`초 (기본 300초)
     - WebSocket(채팅/알림)을 쓰는 앱이라 0에 가깝게 낮추지는 않는다 — 배포 중 열려있던 연결이 정상 종료할 시간은 남겨둔다.
10. Create
11. 인스턴스를 수동 등록하지 않는다. ECS Service가 동적 포트로 등록한다.

CLI로 기존 Target Group에 적용할 때:

```bash
$AWSCLI elbv2 modify-target-group \
  --target-group-arn <TARGET_GROUP_ARN> \
  --health-check-interval-seconds 10 \
  --healthy-threshold-count 3 \
  --unhealthy-threshold-count 6

$AWSCLI elbv2 modify-target-group-attributes \
  --target-group-arn <TARGET_GROUP_ARN> \
  --attributes Key=deregistration_delay.timeout_seconds,Value=90
```

### 15-2. ALB 생성

1. EC2 → Load Balancers → Create load balancer
2. Application Load Balancer → Create
3. 이름 `mudo-prod-alb`
4. Scheme → Internet-facing
5. IP address type → IPv4
6. VPC → `mudo-prod-vpc`
7. Mapping → public-2a, public-2c
8. Security group → `mudo-prod-alb-sg`
9. Listener HTTP 80 → 임시 default action 또는 HTTPS Redirect는 생성 후 설정
10. Listener HTTPS 443 추가
11. ACM 인증서 선택
12. 기본 HTTPS 동작은 고정 응답 404로 설정
13. Create

### 15-3. Listener 규칙

ALB → Listeners and rules:

#### HTTP 80

- 모든 요청을 HTTPS 443으로 Redirect
- Status code 301

#### HTTPS 443

- Priority 1
- Condition → Path → `/actuator/prometheus`, `/actuator/prometheus/*`
- Action → Fixed response 403

- Priority 100
- Condition → Host header → `academy-a.ieum.store`
- Action → Forward → `mudo-prod-tg-academy-a`

등록되지 않은 Host는 기본 404를 반환해야 한다. 외부에서 `https://academy-a.ieum.store/actuator/prometheus`를 호출해 403이고, Alloy에서는 같은 Task의 내부 주소로 수집이 성공하는지 모두 확인한다.

학원마다 Target Group과 Listener Rule이 하나씩 늘어나므로 **Service Quotas** 콘솔에서 ALB 규칙·Target Group 한도를 확인한다. 현재 한도의 70~80%에 도달하기 전에 한도 상향을 요청하거나 새 ALB Cell을 만든다.

### 15-4. Route 53 Alias

1. Route 53 → Hosted zones → `<BASE_DOMAIN>`
2. Create record
3. Record name → `*.groupware`
4. Record type A
5. Alias 활성화
6. Route traffic to → Alias to Application Load Balancer
7. 서울 리전과 `mudo-prod-alb` 선택
8. Create

---

## 16. 첫 학원 ECS Service

1. ECS → Clusters → `mudo-prod-cluster`
2. Services → Create
3. Compute options → Capacity provider strategy
4. Use cluster default 또는 생성한 Capacity Provider 선택
5. Application type → Service
6. Task definition family → `mudo-prod-tenant-academy-a`
7. 최신 Revision 선택
8. Service name → `mudo-prod-svc-academy-a`
9. Desired tasks → `1`
10. Deployment type → Rolling update
11. Deployment circuit breaker → Enable
12. Rollback on failure → Enable
13. Minimum healthy percent → 100
14. Maximum percent → 200
15. Load balancing → Application Load Balancer
16. Existing load balancer → `mudo-prod-alb`
17. Listener → HTTPS 443
18. Target group → `mudo-prod-tg-academy-a`
19. Container → app:8080
20. Health check grace period → Spring Boot 기동 시간을 고려해 60~120초
21. Placement strategy → `binpack`, field `memory`
22. Enable ECS Exec은 운영 점검이 필요할 때만 활성화하고 권한과 로그를 함께 구성
23. Create service

검증:

- Service desired 1 / running 1
- Target Group target이 Healthy
- `https://academy-a.<BASE_DOMAIN>/actuator/health`가 200
- Grafana Explore의 Loki에서 `tenant="academy-a"` 로그 조회
- Prometheus에서 `up{tenant="academy-a"}=1` 확인
- RDS의 academy-a DB에 정상 연결

배포 중에는 기존 Task와 새 Task가 동시에 실행될 수 있다. 기존 인스턴스에 여유가 없으면 Capacity Provider가 새 EC2를 추가할 수 있다. 이것은 낭비가 아니라 안전한 배포 여유다.

---

## 17. WAF와 악성 사용 방어

### 17-1. WAF Web ACL 생성

1. 콘솔 검색 → `WAF & Shield`
2. Web ACLs → Create web ACL
3. 이름 `mudo-prod-web-acl`
4. Resource type → Regional resources
5. Region → 서울
6. Associated AWS resources → `mudo-prod-alb`
7. Default action → Allow

### 17-2. Managed Rules

처음에는 Count 모드로 오탐을 관찰한 후 Block으로 전환한다.

- AWSManagedRulesCommonRuleSet
- AWSManagedRulesKnownBadInputsRuleSet
- AWSManagedRulesSQLiRuleSet

관리 규칙 버전과 비용은 생성 화면에서 확인한다.

### 17-3. Rate-based Rule

1. Add rules → Add my own rules and rule groups
2. Rule type → Rate-based rule
3. 이름 `mudo-prod-rate-limit-ip`
4. Aggregate key → Source IP
5. 처음에는 Count
6. 정상 사용량을 관찰해 제한값 확정
7. 오탐 검증 후 Block 또는 Challenge

로그인 경로에는 더 낮은 제한을 둔다.

```text
/api/auth/login
/api/auth/refresh
/api/password/**
```

IP만으로는 여러 IP를 사용하는 공격을 막지 못한다. 로그인 이후에는 애플리케이션이 다음 키로 별도 제한한다.

```text
tenantId + userId + endpoint category
```

### 17-4. 애플리케이션 제한

- 학원별 초당·분당 요청량
- 사용자별 로그인 시도
- 대량 검색 범위와 페이지 크기
- 엑셀·보고서 생성 동시 수
- 파일 업로드 최대 크기
- WebSocket 연결 수
- 쿼리·트랜잭션 타임아웃

학원별 Task이므로 임계치 초과 학원만 `429 Too Many Requests` 또는 일시 정지 상태로 전환할 수 있어야 한다.

### 17-5. 자동 확장 비용 공격 방지

- WAF가 먼저 차단하고 ECS 확장은 그 뒤에 일어나야 한다.
- ASG Max size를 무한대로 두지 않는다.
- RDS Storage maximum을 설정한다.
- Aurora Serverless로 변경한다면 Max ACU를 반드시 설정한다.
- CloudWatch 알람은 임계치 초과 시 Slack으로 전송한다.
- 초기에는 자동 학원 정지보다 알림과 수동 차단으로 시작한다.

---

## 18. S3와 Google Drive의 학원 격리

### 18-1. S3 버킷 두 개

S3 → Create bucket:

```text
mudo-prod-finance-<account-suffix>
mudo-prod-staff-<account-suffix>
```

공통 설정:

- Block all public access 활성화
- Versioning 활성화
- Default encryption 활성화
- HTTPS가 아닌 요청을 거부하는 Bucket Policy
- Lifecycle과 보존 기간 결정

### 18-2. 학원별 Prefix

객체 경로는 항상 다음 형식을 사용한다.

```text
tenants/<tenant-code>/<yyyy>/<mm>/<uuid>-<safe-file-name>
```

파일명을 사용자가 보낸 그대로 경로에 사용하지 않는다.

### 18-3. 학원별 Task Role 정책

academy-a Task Role은 다음 Prefix만 접근한다.

```text
finance bucket: tenants/academy-a/*
staff bucket: tenants/academy-a/*
```

`s3:ListBucket`에는 `s3:prefix` 조건을 적용한다. 애플리케이션 RBAC는 같은 학원 안에서 원장·회계·직원 권한을 다시 검사한다.

### 18-4. Google Drive

기존의 전사 공유 Drive 하나를 모든 학원이 함께 사용하면 안 된다.

학원마다 다음을 만든다.

- 학원 전용 Shared Drive
- 학원 전용 서비스 계정 또는 학원별 최소 권한 연동
- 학원별 Drive ID
- 학원별 서비스 계정 JSON SecureString

academy-a Task에는 academy-a Drive 설정만 주입한다.

---

## 19. WebSocket과 Redis

현재 프로젝트에는 WebSocket 기능이 있다. ECS Rolling Update는 Desired count가 1이어도 배포 중 구버전과 신버전 Task가 동시에 요청을 받을 수 있다. 따라서 서로 다른 Task에 연결된 사용자 사이의 채팅·알림 정합성이 필요하면 운영 시작 전에 Redis Pub/Sub를 구성한다.

Redis 없이 시작할 수 있는 경우는 WebSocket이 아직 운영 기능이 아니거나, 배포 중 메시지 단절을 사업적으로 허용하는 내부 검증 환경뿐이다.

### 19-1. Cache 보안 그룹

VPC → Security groups → Create security group:

- 이름 `mudo-prod-cache-sg`
- VPC `mudo-prod-vpc`
- Inbound TCP 6379
- Source `mudo-prod-ecs-instance-sg`
- 인터넷 CIDR 허용 금지

### 19-2. Cache Subnet Group

ElastiCache → Subnet groups → Create subnet group:

- 이름 `mudo-prod-cache-subnet-group`
- VPC `mudo-prod-vpc`
- `mudo-prod-db-2a`, `mudo-prod-db-2c` 선택

DB와 같은 private subnet을 재사용하되 Security Group은 분리한다.

### 19-3. ElastiCache 생성

1. 콘솔 검색 → `ElastiCache`
2. Valkey 또는 Redis OSS compatible cache 생성
3. 이름 `mudo-prod-realtime-cache`
4. Cluster mode → Disabled로 시작
5. 내부 검증:
   - 작은 노드 1개
   - 복제본 없음 허용
6. 유료 운영:
   - Primary 1개 + Replica 1개
   - Multi-AZ와 automatic failover 활성화
7. Subnet group → `mudo-prod-cache-subnet-group`
8. Security group → `mudo-prod-cache-sg`
9. Encryption in transit → 활성화
10. Encryption at rest → 활성화
11. AUTH token 또는 지원되는 인증 방식 설정
12. 생성 후 Primary endpoint 기록

### 19-4. 애플리케이션 연결

Parameter Store의 다음 값을 Task에 주입한다.

```text
REDIS_HOST
REDIS_PORT
REDIS_PASSWORD
```

Redis는 다음 용도로만 명확히 구분해서 사용한다.

- WebSocket 채팅·알림 Pub/Sub
- 여러 Task가 존재할 때 분산 Rate Limit
- 필요성이 검증된 조회 캐시

Pub/Sub 메시지는 영구 저장소가 아니다. 반드시 보존해야 하는 메시지는 RDS에 먼저 기록하고 Redis는 전달 신호로 사용한다.

프론트엔드는 Redis 유무와 관계없이 WebSocket 재연결과 지수 백오프를 구현한다.

---

## 20. main 운영 CI/CD

### 20-1. GitHub Actions Secret과 Variable

GitHub 저장소 → Settings → Secrets and variables → Actions에서 다음 값을 등록한다.

Repository secret:

```text
AWS_ROLE_ARN=arn:aws:iam::<ACCOUNT_ID>:role/mudo-prod-ci-deploy-role
```

Repository variables:

```text
AWS_REGION=ap-northeast-2
ECR_REPOSITORY=mudo-groupware-backend
PRODUCTION_DEPLOY_ENABLED=false
```

`PRODUCTION_DEPLOY_ENABLED`는 모든 AWS 리소스와 첫 학원 검증이 완료될 때까지 `false`로 유지한다. `true`로 바꾸면 이후 `main` push가 실제 운영 배포를 실행한다.

이 프로젝트는 `main` 머지 후 자동 CD를 유지하므로 GitHub Environment의 Required reviewer와 수동 승인 규칙은 사용하지 않는다. 워크플로의 `github.ref == 'refs/heads/main'` 조건, GitHub OIDC Role의 `main` 제한, `PRODUCTION_DEPLOY_ENABLED`, 학원별 `enabled`를 함께 배포 안전장치로 사용한다.

AWS Access Key와 Secret Access Key는 등록하지 않는다.

### 20-2. 워크플로 권한

```yaml
permissions:
  contents: read
  id-token: write
```

`actions/checkout`에는 다음을 적용한다.

```yaml
with:
  persist-credentials: false
```

### 20-3. main 배포 순서

```text
main push
  ↓
Checkout
  ↓
Gradle build + test
  ↓
Docker build
  ↓
ECR에 Git SHA Push
  ↓
Canary 또는 내부 학원 Migration Task 실행
  ↓
전체 학원 DB에 backward-compatible migration 순차 적용
  ↓
학원별 Task Definition 새 Revision 등록
  ↓
학원별 ECS Service 순차 update-service
  ↓
services-stable 대기
  ↓
외부 Smoke Test
```

### 20-4. 배포 대상 관리 파일

13-6의 `infra/runtime-profiles.yml`, `infra/tenants.yml`, `infra/cells.yml`을 배포 입력으로 사용한다. 시크릿을 넣지 않는 학원 매니페스트 예시는 다음과 같다.

```yaml
tenants:
  - code: academy-a
    service: mudo-prod-svc-academy-a
    task_family: mudo-prod-tenant-academy-a
    billing_plan: basic
    runtime_profile: shared-default
    cell: cell-1
    health_url: https://academy-a.ieum.store/actuator/health
```

CI는 학원별 값을 직접 하드코딩하지 않고 다음 순서로 처리한다.

```text
매니페스트 스키마 검증
→ Cell 입점·배포 Surge 용량 검사
→ runtime profile + tenant + cell을 조합해 Task Definition 생성
→ 생성 결과와 현재 Task Definition 차이 확인
→ 학원별 순차 배포
```

DB 비밀번호와 JWT 값은 매니페스트에 넣지 않는다. Task Definition은 SSM ARN만 참조한다. 런타임 프로필 값을 콘솔에서 직접 수정해 매니페스트와 달라지는 구성 드리프트도 배포 전에 실패 처리한다. `billing_plan`은 로그·지표 메타데이터로 전달할 수 있지만 CPU·메모리·Hikari 값을 선택하는 데 사용하지 않는다.

### 20-5. DB 마이그레이션 원칙

- Flyway는 일반 앱 계정이 아닌 학원별 migrator 계정으로 실행
- 애플리케이션 이미지와 별도로 `Dockerfile.migration`에서 Flyway 전용 이미지를 만들고 같은 Git SHA에 대응하는 `migration-<GIT_SHA>` 태그로 ECR에 저장
- GitHub Public Runner가 RDS에 직접 접속하지 않음
- ECS Migration Task를 private app subnet에서 실행
- 스키마 변경은 이전 앱 버전과도 호환되는 expand/contract 방식 사용
- DB 마이그레이션은 코드처럼 즉시 되돌리기 어렵다는 점을 PR에 명시
- 한 학원 Migration 실패 시 이후 학원과 앱 배포 중단

### 20-6. 롤백

- ECS Deployment Circuit Breaker 자동 롤백 활성화
- 외부 Smoke Test 실패 시 이전 Task Definition Revision으로 update-service
- DB 스키마는 자동 DROP으로 롤백하지 않는다.
- 롤백 가능한 이미지 SHA를 ECR에 유지한다.

### 20-7. 관측성 구성 배포

애플리케이션 배포와 모니터링 스택 변경을 한 작업에서 동시에 수행하지 않는다. 실패 시 원인과 롤백 대상을 분리하기 위해 다음 순서를 사용한다.

```text
관측성 설정 PR
→ Compose·Prometheus Rule·Loki·Alloy 설정 정적 검증
→ Alloy 이미지 build·scan·ECR SHA/Digest push
→ 테스트 ECS 호스트에서 Alloy 수집 확인
→ 모니터링 EC2 Compose config 검증
→ 모니터링 Stack 한 서비스씩 갱신
→ Alloy Daemon Service 순차 갱신
→ 기존 학원 지표·로그 연속성 확인
```

Grafana Dashboard JSON과 Provisioning 파일은 Git에서 관리한다. 모니터링 변경 전후 30분의 `up`, 수집 실패, 로그 유입량을 비교하고 문제가 생기면 이전 이미지 Digest와 설정 Revision으로 되돌린다.

---

## 21. 전용 모니터링 EC2와 관측성 스택

### 21-1. 역할 분리

도구를 중복해서 저장소로 사용하지 않고 역할을 나눈다.

| 영역 | 저장·수집 도구 | 이유 |
|---|---|---|
| ALB, WAF, ECS, EC2, RDS 기본 지표 | CloudWatch | AWS가 기본 제공하고 모니터링 EC2 장애와 독립적 |
| JVM, Tomcat, Hikari, 앱 커스텀 지표 | Alloy → Prometheus | 앱 내부 포화와 학원별 사용량 분석 |
| 애플리케이션 stdout 로그 | Alloy → Loki | Task 교체 후에도 학원·레벨별 검색 |
| 통합 시각화 | Self-hosted Grafana | Prometheus, Loki, CloudWatch를 한 화면에서 조회 |
| 앱 지표 알림 | Prometheus Rule → Alertmanager → Slack | 앱 내부 임계치 알림 |
| AWS·모니터링 서버 장애 알림 | CloudWatch Alarm → SNS → Slack | 관측성 서버가 죽어도 알림 유지 |

Promtail은 신규 설치하지 않는다. 현재 Grafana 생태계의 통합 수집기인 Grafana Alloy로 메트릭과 로그 수집을 통일한다.

### 21-2. 초기 사양과 비용 상한

첫 Cell의 시작값:

```text
EC2: t4g.medium, 2 vCPU, 4 GiB
EBS: gp3 100 GiB, 암호화
Prometheus retention: 15d
Loki retention: 30d
Grafana/Prometheus/Loki/Alertmanager: 단일 Docker Compose
```

`t4g.medium`은 초기 저부하 가설이다. 지속 CPU 크레딧 고갈, 메모리 75% 이상, 디스크 70% 이상 또는 쿼리 지연이 반복되면 `m7g.large` 같은 일반 목적 8 GiB 클래스로 변경한다. ARM64 이미지를 사용하기 전에 선택한 Grafana Stack 이미지와 플러그인의 ARM64 지원을 검증하고, 검증하지 못하면 X86_64 인스턴스를 선택한다.

이 서버는 초기에는 1대만 사용한다. 대시보드 고가용성보다 비용 절감을 우선한 결정이며, CloudWatch 핵심 알람을 별도로 유지해 단일 장애점을 보완한다.

### 21-3. 모니터링 EC2 생성

EC2 → Instances → Launch instances:

1. Name → `mudo-prod-monitoring-1`
2. AMI → Amazon Linux 2023
3. Architecture → 선택한 인스턴스와 일치
4. Instance type → `<MONITORING_INSTANCE_TYPE>`
5. Key pair → 사용하지 않음
6. VPC → `mudo-prod-vpc`
7. Subnet → `mudo-prod-app-2a`
8. Auto-assign public IP → Disable
9. Security group → `mudo-prod-monitoring-sg`
10. IAM instance profile → `mudo-prod-monitoring-ec2-role`
11. Root EBS → gp3, 최소 20 GiB, 암호화
12. 추가 Data EBS → gp3 `<MONITORING_VOLUME_SIZE>`, 암호화
13. Delete on termination → 데이터 볼륨은 Off
14. Metadata version → V2 only
15. Metadata response hop limit → 2
16. Detailed monitoring → 비용을 확인한 뒤 필요 시 활성화
17. Launch instance

SSH 22를 열지 않는다. Systems Manager → Session Manager에서 접속한다.

CloudWatch → Alarms → Create alarm에서 `StatusCheckFailed_System >= 1` 알람을 만들고 EC2 recovery action과 SNS 알림을 연결한다. 지원하지 않는 인스턴스 유형이면 알림 후 수동 복구 절차를 사용한다.

데이터 볼륨은 `/opt/mudo-observability/data`에 마운트하고 `/etc/fstab`에는 디바이스 이름이 아니라 UUID로 등록한다. 잘못된 `fstab` 때문에 재부팅이 막히지 않도록 `nofail`을 적용하고 재부팅 시험을 한다.

### 21-4. 내부 DNS

Route 53 → Hosted zones → Create hosted zone:

1. Domain name → `mudo.internal`
2. Type → Private hosted zone
3. VPC → `mudo-prod-vpc`
4. Create
5. A Record `monitoring.mudo.internal`
6. Value → 모니터링 EC2의 Private IPv4

Alloy는 다음 주소를 사용한다.

```text
Prometheus remote write: http://monitoring.mudo.internal:9090/api/v1/write
Loki push: http://monitoring.mudo.internal:3100/loki/api/v1/push
```

모니터링 EC2를 새로 만들거나 Private IP가 변경되면 이 Record를 먼저 갱신한다.

### 21-5. 서버 디렉터리와 설치 원칙

모니터링 구성 원본은 저장소에서 버전 관리한다.

```text
infra/monitoring/
├─ README.md
├─ docker-compose.monitoring.yml
├─ prometheus/prometheus.yml
├─ prometheus/rules/
├─ loki/loki-config.yml
├─ alertmanager/alertmanager.template.yml
├─ grafana/provisioning/
└─ grafana/dashboards/

infra/alloy/
├─ Dockerfile
├─ config.alloy
└─ task-definition.template.json

infra/runbooks/
├─ application-down.md
├─ database-pool.md
├─ monitoring-stack.md
└─ thread-saturation.md
```

서버 배치 경로:

```text
/opt/mudo-observability/config
/opt/mudo-observability/data/prometheus
/opt/mudo-observability/data/loki
/opt/mudo-observability/data/grafana
/opt/mudo-observability/data/alertmanager
```

각 이미지의 공식 기본 UID/GID를 확인해 데이터 디렉터리 소유권을 설정한다. 무조건 `chmod 777`로 해결하지 않는다. 업그레이드 전후 컨테이너 UID가 바뀌는지도 확인한다.

Session Manager에서 Docker Engine과 Docker Compose Plugin을 설치하고 Docker 서비스를 활성화한다. 설치 버전과 설치 출처를 운영 변경 이슈에 기록한다. 인터넷의 임의 설치 스크립트를 root로 바로 실행하지 않는다.

2026-08-05 저장소 검증 기준 버전은 다음과 같다.

| 구성요소 | 고정 버전 |
|---|---|
| Grafana | `13.1.0` |
| Prometheus | `v3.12.0` |
| Loki | `3.7.2` |
| Alertmanager | `v0.32.1` |
| Grafana Alloy | `v1.18.0` |

운영 반영 직전 공식 릴리스와 이미지 Digest를 다시 확인하고, 검증된 Digest를 Task Definition과 운영 변경 이슈에 기록한다. `latest`를 운영에 사용하지 않는다.

### 21-6. 모니터링 Docker Compose

`docker-compose.monitoring.yml`의 기본 구조:

### 21-5-1. 운영 반영: 데이터 볼륨 권한과 502 복구

모니터링 서버는 호스트 디렉터리를 컨테이너에 바인드 마운트한다. 따라서 최초 생성자 또는 이전 컨테이너가 `root` 소유로 남긴 파일 때문에 Grafana, Prometheus, Loki, Alertmanager가 시작 직후 종료될 수 있다. 이 경우 ALB 대상이 비정상이 되어 `https://monitoring.ieum.store`에서 `502 Bad Gateway`가 발생한다.

`deploy-monitoring-stack.sh`는 배포 전에 아래 데이터 디렉터리와 SSM에서 렌더링한 `alertmanager/alertmanager.yml`의 소유권을 매번 보정한다. 기존에 잘못 생성된 파일도 다음 재배포에서 함께 복구된다.

| 디렉터리 | 컨테이너 | 소유권 |
| --- | --- | --- |
| `data/grafana` | Grafana | `472:472` |
| `data/prometheus` | Prometheus | `65534:65534` |
| `data/loki` | Loki | `10001:10001` |
| `data/alertmanager` | Alertmanager | `65534:65534` |
| `alertmanager/alertmanager.yml` | Alertmanager 설정 | `65534:65534`, mode `600` |

복구 시에는 다음 순서로 확인한다.

1. ALB 대상 그룹 `mudo-prod-tg-monitoring`의 대상 상태가 `Healthy`인지 확인한다.
2. 모니터링 EC2에서 `docker ps -a`와 각 컨테이너 로그를 확인한다. `not writable`, `permission denied`가 보이면 볼륨 소유권 문제다.
3. 수정된 배포 스크립트가 main에 반영된 뒤 모니터링 GitHub Actions를 다시 실행한다.
4. `docker ps -a`에서 Grafana, Prometheus, Loki, Alertmanager가 모두 `Up`인지 확인하고, 최근 로그에 오류가 없는지 확인한다.

   ```bash
   docker logs --tail 50 mudo-observability-grafana-1
   docker logs --tail 50 mudo-observability-prometheus-1
   docker logs --tail 50 mudo-observability-loki-1
   docker logs --tail 50 mudo-observability-alertmanager-1
   ```

5. 모니터링 EC2에서 각 서비스의 준비 상태를 확인한다.

   ```bash
   curl -fsS http://127.0.0.1:3000/api/health
   curl -fsS http://127.0.0.1:9090/-/ready
   curl -fsS http://127.0.0.1:3100/ready
   curl -fsS http://127.0.0.1:9093/-/ready
   ```

6. 마지막으로 `https://monitoring.ieum.store/api/health`가 `HTTP 200`인지 확인한다. ALB는 Grafana 포트 3000만 검사하므로, 이 응답만으로 Prometheus·Loki·Alertmanager까지 정상이라고 판단하지 않는다.

`chmod 777`로 우회하지 않는다. 이미지 버전을 변경할 때는 각 이미지의 실행 UID/GID를 다시 확인해 위 소유권 값을 함께 검토한다.

```yaml
name: mudo-observability

services:
  grafana:
    image: grafana/grafana:13.1.0
    restart: unless-stopped
    ports:
      - "3000:3000"
    environment:
      GF_SERVER_ROOT_URL: https://${MONITORING_DOMAIN}
      GF_SECURITY_ADMIN_USER: ${GRAFANA_ADMIN_USER}
      GF_SECURITY_ADMIN_PASSWORD: ${GRAFANA_ADMIN_PASSWORD}
      GF_SECURITY_COOKIE_SECURE: "true"
      GF_USERS_ALLOW_SIGN_UP: "false"
      GF_AUTH_ANONYMOUS_ENABLED: "false"
      GF_AWS_ALLOWED_AUTH_PROVIDERS: default
    volumes:
      - /opt/mudo-observability/data/grafana:/var/lib/grafana
      - ./grafana/provisioning:/etc/grafana/provisioning:ro
    depends_on:
      - prometheus
      - loki
    networks:
      - observability

  prometheus:
    image: prom/prometheus:v3.12.0
    restart: unless-stopped
    ports:
      - "9090:9090"
    command:
      - --config.file=/etc/prometheus/prometheus.yml
      - --storage.tsdb.path=/prometheus
      - --storage.tsdb.retention.time=${PROMETHEUS_RETENTION:-15d}
      - --web.enable-remote-write-receiver
    volumes:
      - ./prometheus:/etc/prometheus:ro
      - /opt/mudo-observability/data/prometheus:/prometheus
    networks:
      - observability

  loki:
    image: grafana/loki:3.7.2
    restart: unless-stopped
    ports:
      - "3100:3100"
    command:
      - -config.file=/etc/loki/loki-config.yml
    volumes:
      - ./loki/loki-config.yml:/etc/loki/loki-config.yml:ro
      - /opt/mudo-observability/data/loki:/loki
    networks:
      - observability

  alertmanager:
    image: prom/alertmanager:v0.32.1
    restart: unless-stopped
    command:
      - --config.file=/etc/alertmanager/alertmanager.yml
      - --storage.path=/alertmanager
    volumes:
      - ./alertmanager/alertmanager.yml:/etc/alertmanager/alertmanager.yml:ro
      - /opt/mudo-observability/data/alertmanager:/alertmanager
    networks:
      - observability

networks:
  observability:
    driver: bridge
```

9090과 3100은 호스트에 바인딩되지만 Security Group이 ECS 인스턴스에서 오는 트래픽만 허용한다. 3000만 ALB에서 접근할 수 있다.

`.env.monitoring`은 서버에서 SSM 값을 받아 생성하고 권한을 `600`으로 제한한다. Git에는 올리지 않는다.

```text
MONITORING_DOMAIN=monitoring.ieum.store
GRAFANA_ADMIN_USER=<SSM에서 받은 값>
GRAFANA_ADMIN_PASSWORD=<SSM에서 받은 값>
PROMETHEUS_RETENTION=15d
```

`MONITORING_DOMAIN`은 Grafana의 실제 외부 주소다. 모니터링 EC2의 사설 IP(`10.40.10.33`)는 VPC 내부 통신용이므로 브라우저 접속 주소로 사용하지 않는다.

Alertmanager의 실제 `alertmanager.yml`도 SSM의 Slack Webhook을 주입해 서버에서 렌더링한다. Webhook이 포함된 결과 파일은 Git에 올리지 않고 권한을 `600`으로 제한한다.

`alertmanager.template.yml` 구조:

```yaml
global:
  resolve_timeout: 5m
  slack_api_url: '<SLACK_WEBHOOK_FROM_SSM>'

route:
  receiver: mudo-infra-slack
  group_by: [alertname, tenant, service]
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 4h

receivers:
  - name: mudo-infra-slack
    slack_configs:
      - channel: '#mudo-infra-alerts'
        send_resolved: true
        title: '[{{ .Status }}] {{ .CommonLabels.alertname }}'
        text: '{{ .CommonAnnotations.summary }}'
```

채널 이름과 템플릿을 실제 Slack 정책에 맞게 바꾸고 테스트 알람으로 resolved 메시지까지 확인한다.

실행:

```bash
docker compose --env-file .env.monitoring -f docker-compose.monitoring.yml config
docker compose --env-file .env.monitoring -f docker-compose.monitoring.yml pull
docker compose --env-file .env.monitoring -f docker-compose.monitoring.yml up -d
docker compose --env-file .env.monitoring -f docker-compose.monitoring.yml ps
```

`config` 출력은 시크릿을 포함할 수 있으므로 CI 로그나 이슈에 붙이지 않는다.

### 21-7. Prometheus와 Alertmanager 기본 설정

`prometheus/prometheus.yml`:

```yaml
global:
  scrape_interval: 30s
  evaluation_interval: 30s

rule_files:
  - /etc/prometheus/rules/*.yml

alerting:
  alertmanagers:
    - static_configs:
        - targets: ["alertmanager:9093"]

scrape_configs:
  - job_name: prometheus
    static_configs:
      - targets: ["prometheus:9090"]
```

앱 지표는 Alloy의 remote write로 들어온다. 초기 단일 Cell·낮은 수집량에서만 Prometheus remote-write receiver를 사용한다. 수집량과 Cell 수가 커지면 Mimir 또는 다른 remote-write 저장소로 변경한다.

첫 알림 규칙:

```text
학원별 Hikari pending 지속
Hikari connection timeout 발생
Tomcat busy/max 80% 이상 지속
비동기 Executor queue 80% 이상
비동기 작업 rejected 발생
학원별 오류율 급증
Actuator scrape up == 0
Alloy remote write 실패·재시도 증가
Loki 수집 중단
```

순간값 한 번으로 알람을 울리지 않고 5~10분 지속 조건을 둔다. 각 알람에는 `tenant`, `service`, `severity`, Runbook URL을 포함한다.

### 21-8. Loki 단일 노드 설정

초기 Loki는 single-binary 모드와 로컬 filesystem을 사용한다.

```yaml
auth_enabled: false

server:
  http_listen_port: 3100

common:
  path_prefix: /loki
  replication_factor: 1
  ring:
    kvstore:
      store: inmemory
  storage:
    filesystem:
      chunks_directory: /loki/chunks
      rules_directory: /loki/rules

schema_config:
  configs:
    - from: 2024-01-01
      store: tsdb
      object_store: filesystem
      schema: v13
      index:
        prefix: index_
        period: 24h

limits_config:
  retention_period: 720h

compactor:
  working_directory: /loki/compactor
  retention_enabled: true
  delete_request_store: filesystem
```

`auth_enabled: false`이므로 Loki 3100은 반드시 private SG로 제한한다. 인터넷 또는 ALB Listener에 연결하지 않는다. 버전 변경 전에는 해당 Loki 버전의 설정 검증 명령으로 구성 파일을 검사한다.

Loki Label은 다음처럼 값 종류가 제한된 항목만 사용한다.

```text
tenant
service
environment
level
```

`requestId`, `userId`, 이메일, 파일명, 전체 URL은 Label로 만들지 않고 JSON 로그 본문 필드로 저장한다. Label cardinality가 커지면 메모리와 검색 성능이 급격히 나빠질 수 있다.

### 21-9. Grafana 데이터 소스 Provisioning

`grafana/provisioning/datasources/datasources.yml`:

```yaml
apiVersion: 1

datasources:
  - name: Prometheus
    uid: prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true

  - name: Loki
    uid: loki
    type: loki
    access: proxy
    url: http://loki:3100

  - name: CloudWatch
    uid: cloudwatch
    type: cloudwatch
    access: proxy
    jsonData:
      authType: default
      defaultRegion: ap-northeast-2
```

CloudWatch 데이터 소스는 `mudo-prod-monitoring-ec2-role`의 임시 자격 증명을 사용한다. Grafana에 Access Key와 Secret Key를 입력하지 않는다.

### 21-10. Grafana 외부 연결

EC2 → Target Groups → Create target group:

1. Target type → Instances
2. Name → `mudo-prod-tg-monitoring`
3. Protocol/Port → HTTP 3000
4. VPC → `mudo-prod-vpc`
5. Health check path → `/api/health`
6. 모니터링 EC2 등록
7. Healthy 확인

기존 ALB HTTPS Listener에 다음 규칙을 추가한다.

```text
Host = monitoring.ieum.store
→ mudo-prod-tg-monitoring
```

Route 53 wildcard가 없다면 `monitoring.ieum.store` Alias Record를 기존 ALB로 추가한다. 별도 ALB를 만들지 않는다.

공유 계정 제약 때문에 초기 Grafana는 공유 로컬 계정 `groupware-grafana` 하나를 사용한다. Anonymous access와 self sign-up은 끄고 비밀번호는 SSM에서 관리한다. 이 방식 역시 실제 사용자를 감사 로그로 구분할 수 없으므로 운영 변경 이슈 기록 절차를 동일하게 적용한다.

### 21-11. Alloy 이미지와 ECS Daemon Service

Alloy 설정을 포함한 전용 이미지를 만든다.

```dockerfile
FROM grafana/alloy:v1.18.0@sha256:<VERIFIED_DIGEST>
COPY config.alloy /etc/alloy/config.alloy
ENTRYPOINT ["/bin/alloy", "run", "--storage.path=/var/lib/alloy", "/etc/alloy/config.alloy"]
```

ECR 저장소 이름은 `mudo-observability-alloy`로 하고 이미지 스캔을 활성화한다. 버전과 Digest를 PR에서 검토한다.

`config.alloy` 기본 구조:

```alloy
discovery.docker "local" {
  host = "unix:///var/run/docker.sock"
}

discovery.relabel "mudo_metrics" {
  targets = discovery.docker.local.targets

  rule {
    source_labels = ["__meta_docker_container_label_prometheus_scrape"]
    regex         = "true"
    action        = "keep"
  }

  rule {
    source_labels = ["__meta_docker_port_private"]
    regex         = "8080"
    action        = "keep"
  }

  rule {
    source_labels = ["__meta_docker_network_ip", "__meta_docker_port_private"]
    separator     = ":"
    target_label  = "__address__"
  }

  rule {
    target_label = "__metrics_path__"
    replacement  = "/actuator/prometheus"
  }

  rule {
    source_labels = ["__meta_docker_container_label_mudo_tenant"]
    target_label  = "tenant"
  }

  rule {
    source_labels = ["__meta_docker_container_label_mudo_service"]
    target_label  = "service"
  }

  rule {
    source_labels = ["__meta_docker_container_label_mudo_environment"]
    target_label  = "environment"
  }
}

prometheus.scrape "mudo_apps" {
  targets         = discovery.relabel.mudo_metrics.output
  scrape_interval = "30s"
  scrape_timeout  = "10s"
  forward_to      = [prometheus.remote_write.central.receiver]
}

prometheus.remote_write "central" {
  endpoint {
    url = "http://monitoring.mudo.internal:9090/api/v1/write"
  }
}

discovery.relabel "mudo_logs" {
  targets = discovery.docker.local.targets

  rule {
    source_labels = ["__meta_docker_container_label_mudo_observe"]
    regex         = "true"
    action        = "keep"
  }

  rule {
    source_labels = ["__meta_docker_container_label_mudo_tenant"]
    target_label  = "tenant"
  }

  rule {
    source_labels = ["__meta_docker_container_label_mudo_service"]
    target_label  = "service"
  }

  rule {
    source_labels = ["__meta_docker_container_label_mudo_environment"]
    target_label  = "environment"
  }
}

loki.source.docker "mudo_apps" {
  host       = "unix:///var/run/docker.sock"
  targets    = discovery.relabel.mudo_logs.output
  forward_to = [loki.process.mudo_json.receiver]
}

loki.process "mudo_json" {
  stage.json {
    expressions = {
      level = "level",
    }
  }

  stage.labels {
    values = {
      level = "",
    }
  }

  forward_to = [loki.write.central.receiver]
}

loki.write "central" {
  endpoint {
    url = "http://monitoring.mudo.internal:3100/loki/api/v1/push"
  }
}
```

Docker Label의 점은 Alloy 메타 Label에서 밑줄로 변환된다. 예를 들어 `mudo.tenant`는 `__meta_docker_container_label_mudo_tenant`로 읽는다.

Alloy Task Definition:

```text
family: mudo-prod-alloy
launch type: EC2
network mode: host
cpu: 128
memoryReservation: 256 MiB
memory hard limit: 512 MiB
host volume: /var/run/docker.sock → /var/run/docker.sock read-only
host volume: /var/lib/mudo-alloy → /var/lib/alloy
Alloy 자체 로그: awslogs → /mudo/prod/monitoring
```

Docker Socket 접근은 사실상 호스트 수준의 강한 권한이므로 Alloy 외 Task에는 절대 마운트하지 않는다.

ECS → Cluster → Services → Create:

1. Task Definition → `mudo-prod-alloy`
2. Service name → `mudo-prod-alloy-daemon`
3. Scheduling strategy → Daemon
4. Launch type → EC2
5. Load balancer → 없음
6. Deployment circuit breaker → 지원 범위에서 활성화
7. Create

새 ECS EC2가 Cluster에 들어오면 Alloy Task가 한 개 자동으로 실행되는지 확인한다. Alloy 예약 CPU·메모리는 EC2 배치 가능량 계산에서 호스트 예약 자원으로 뺀다.

### 21-12. CloudWatch 수집 항목

AWS 기본 지표:

- ALB RequestCount, TargetResponseTime, HTTPCode_Target_5XX_Count
- WAF AllowedRequests, BlockedRequests
- ECS CPUUtilization, MemoryUtilization, RunningTaskCount, PendingTaskCount
- CapacityProviderReservation
- EC2 CPU, Network, StatusCheckFailed
- RDS CPUUtilization, DatabaseConnections, FreeableMemory, FreeStorageSpace
- RDS ReadLatency, WriteLatency, ReadIOPS, WriteIOPS

Container Insights는 비용을 확인하고 활성화한다. Prometheus 지표와 중복되는 고해상도 컨테이너 지표를 무조건 모두 켜지 말고, AWS 자동 확장과 장애 대응에 필요한 항목을 우선한다.

모니터링 EC2에는 CloudWatch Agent를 설치해 다음을 전송한다.

```text
mem_used_percent
disk_used_percent
disk_free
swap_used_percent
Docker 서비스 상태 또는 프로세스 상태
```

### 21-13. 애플리케이션 Prometheus 지표

Spring Boot Actuator와 Micrometer가 제공하는 지표에 프로젝트 전용 Meter를 추가한다.

```text
TenantRequestCount
TenantErrorCount
TenantResponseTime
TenantActiveUsers
TenantHeavyJobCount
TenantStorageBytes
TenantPlanUsage
TenantCalculatedCharge
TenantRateLimitRejectedCount
TenantTomcatThreadsBusy
TenantTomcatThreadsMax
TenantHikariConnectionsActive
TenantHikariConnectionsPending
TenantHikariTimeoutCount
TenantAsyncExecutorActive
TenantAsyncExecutorQueueSize
TenantAsyncRejectedCount
TenantHeavyJobsActive
TenantWebSocketSessions
```

Label은 `tenant`, `plan`, `environment`, `service`처럼 값 범위가 제한된 것만 사용한다. 사용자 ID, requestId, URL 전체 문자열을 Metric Label로 만들지 않는다.

### 21-14. Grafana 대시보드 구성

#### 경영·원장용

- 서비스 정상 여부
- 최근 24시간 요청 수
- 평균·95백분위 응답시간
- 오류율
- 활성 사용자 수
- 플랜 사용량
- 내부 정책으로 계산한 월 예상 청구액

#### 개발·운영용

- ECS Task CPU·메모리와 EC2 예약량
- 학원별 Tomcat busy/max, Hikari active/max/pending
- 비동기 Executor active/queue/rejected와 무거운 작업 동시 수
- 학원별 WebSocket 세션 수
- Pending Task와 EC2 증설
- 학원별 요청·오류·Rate Limit
- RDS Cell별 CPU, DB Load, 연결 수, 지연시간
- WAF 차단 요청
- Loki 최근 ERROR 로그
- 배포 SHA와 배포 성공·실패

Grafana Dashboard 변수는 `tenant`, `plan`, `environment`, `service`를 사용한다. 한 학원을 선택하면 Prometheus 패널과 Loki 로그 패널이 같은 학원으로 필터링되게 구성한다.

### 21-15. 알림 경로

CloudWatch Alarm → SNS `mudo-prod-infra-alerts` → Amazon Q Developer in chat applications → Slack:

- ALB 5xx와 TargetResponseTime 증가
- ECS PendingTaskCount 지속, Task 반복 재시작
- ASG Max size 도달
- RDS CPU·연결·메모리·스토리지 임계치
- WAF BlockedRequests 급증
- 모니터링 EC2 StatusCheckFailed
- 모니터링 EC2 CPU·메모리·디스크 임계치
- 배포 실패 EventBridge 이벤트

Prometheus Rule → Alertmanager → Slack:

- 학원별 Hikari pending/timeout
- Tomcat busy/max 80% 이상 지속
- 비동기 Executor 대기열 80% 이상 또는 rejected
- 학원별 오류율·Rate Limit 거부 급증
- Alloy scrape/remote write 실패

Slack에는 JWT, 사용자 개인정보, 전체 로그 본문을 보내지 않는다. Alertmanager와 CloudWatch에 같은 알람을 중복 생성하지 않고 소유 경로를 표로 관리한다.

### 21-16. 백업과 복구

1. Grafana Dashboard, 데이터 소스, Prometheus Rule, Alloy와 Loki 설정은 Git에 저장한다.
2. 렌더링된 시크릿 파일은 Git에 저장하지 않는다.
3. 모니터링 EBS Snapshot을 매일 생성하고 보존 기간을 설정한다.
4. Prometheus 시계열은 초기에는 장기 백업 대상이 아니라 재수집 가능한 운영 데이터로 취급한다.
5. Loki 로그가 감사·법적 보존 자료라면 로컬 EBS만 사용하지 말고 S3 감사 저장소를 별도로 둔다.
6. 분기마다 Snapshot으로 새 테스트 EC2를 만들고 Grafana·Loki 복구를 시험한다.

모니터링 EC2 장애 시 CloudWatch Alarm은 계속 동작해야 한다. 대시보드가 열리지 않는 상황에서 CloudWatch 콘솔과 ECS/RDS 콘솔로 핵심 장애를 확인하는 절차를 Runbook에 남긴다.

### 21-17. 확장 기준

다음 조건이 반복되면 단일 노드 구성을 확장한다.

- 모니터링 EC2 CPU 또는 메모리 70% 이상 지속
- EBS 사용률 70% 이상 또는 예상 보존 기간 미달
- Prometheus 쿼리·Rule 평가 지연
- Loki ingest 오류·검색 지연
- RDS/ECS Cell이 2개 이상으로 증가
- 모니터링 중단이 계약상 SLA에 영향을 줌
- 장기 감사 로그 보관 필요

확장 순서:

```text
1. 불필요한 Metric·Log와 고카디널리티 Label 제거
2. 모니터링 EC2와 EBS 사양 상향
3. Loki storage를 S3 객체 저장소로 이전
4. 메트릭 장기 보관·HA가 필요하면 Thanos 또는 Mimir 중 하나 선택
5. Grafana를 stateless로 만들고 필요 시 2대 이상 구성
```

Thanos와 Mimir를 동시에 기본 도입하지 않는다. Prometheus 장기 보관·전역 조회 중심이면 Thanos, 대규모 remote write와 수평 확장 중심이면 Mimir를 검토한다.

---

## 22. 새 학원 온보딩 절차

학원 추가는 다음 순서로 진행한다.

1. `tenant-code` 중복 확인
2. 결제 플랜과 허용할 유료 엔드포인트·쿼터 결정
3. `infra/tenants.yml`에 후보 학원을 추가하고 배치 Cell 지정
4. 입점 검사 스크립트로 ECS CPU·메모리, RDS 정상 Pool·배포 Surge, 서비스 Quota 확인
5. 용량 검사를 통과한 경우에만 PR 승인
6. 학원 DB와 app/migrator 사용자 생성
7. Flyway Migration 실행
8. SSM 학원별 파라미터 생성
9. S3 Prefix와 학원별 Task Role 생성
10. Google Shared Drive와 서비스 계정 준비
11. 모든 학원에 `shared-default` 런타임 프로필을 적용해 Task Definition 생성
12. Target Group 생성
13. ECS Service 생성
14. ALB Host rule 생성
15. DNS wildcard로 접근 확인
16. WAF와 앱 Rate Limit 적용 확인
17. Alloy가 새 Task 내부 8080의 `/actuator/prometheus`와 stdout 로그를 발견하는지 확인
18. Grafana Dashboard 변수에 학원 표시 확인
19. Smoke Test, 공통 동시성 설정과 결제 플랜별 유료 엔드포인트 허용·거부 확인
20. 고객에게 URL 전달

온보딩 완료 조건:

- 다른 학원 DB 접근 실패
- 다른 학원 S3 Prefix 접근 실패
- 자신의 Drive만 조회
- 공통 CPU·메모리·Tomcat·비동기 Executor·DB Pool 반영
- 결제 플랜별 유료 엔드포인트 허용·거부와 호출량 정책 반영
- 요청 제한과 파일 용량 제한 동작
- 로그와 지표에 TenantId 포함
- Prometheus `up{tenant="<tenant-code>"}=1`
- Loki에서 해당 tenant의 JSON 로그 조회

---

## 23. 자동 확장 검증 시나리오

### 23-1. EC2 추가 검증

1. 현재 인스턴스에 들어갈 수 없는 예약량의 테스트 Task를 실행
2. Task가 Pending이 되는지 확인
3. `CapacityProviderReservation` 확인
4. ASG Desired capacity가 증가하는지 확인
5. 새 EC2가 Cluster에 등록되는지 확인
6. Pending Task가 새 EC2에서 Running이 되는지 확인
7. 새 EC2에 Alloy Daemon Task가 자동 실행되는지 확인
8. 새 학원 Task 지표·로그가 중앙 Prometheus·Loki에 들어오는지 확인
9. 테스트 Task 제거
10. Managed draining 후 빈 EC2와 Alloy Task가 안전하게 종료되는지 확인

실제 사용량이 낮다는 이유만으로 예약량보다 많은 Task를 강제로 넣지 않는다.

### 23-2. 배포 여유 검증

1. 인스턴스가 거의 찬 상태에서 새 이미지 배포
2. 새 Task를 위한 용량이 부족한지 확인
3. 필요하면 새 EC2가 추가되는지 확인
4. 새 Task Healthy 후 기존 Task 종료 확인
5. 빈 EC2가 즉시 과도하게 종료되지 않는지 확인

### 23-3. 공격 방어 검증

운영 데이터가 없는 테스트 환경에서만 수행한다.

1. 동일 IP에서 테스트 API 반복 호출
2. WAF Count/Block 확인
3. 인증 사용자로 API 한도 초과
4. 429 응답 확인
5. 무거운 작업 동시 제한 확인
6. 학원별 Hikari Pool 상한 확인
7. 다른 학원 응답시간 영향 측정
8. ASG와 RDS가 최대 상한을 넘지 않는지 확인

### 23-4. 스레드·연결 포화 검증

운영 데이터가 없는 부하 테스트 환경에서 `shared-default` 런타임 프로필과 무거운 유료 엔드포인트를 구분해 수행한다.

1. DB를 사용하지 않는 가벼운 API와 DB 사용 API를 분리해 부하를 준다.
2. Tomcat busy/max와 응답시간을 확인한다.
3. Hikari active/max/pending과 connection timeout을 확인한다.
4. Tomcat 스레드 60개와 Hikari 10개처럼 서로 다른 상한이 의도대로 작동하는지 확인한다.
5. 비동기 대기열을 채워 rejected 처리와 429/503 응답을 확인한다.
6. 무거운 유료 엔드포인트가 공통 동시 실행 상한과 학원별 쿼터를 넘지 않는지 확인한다.
7. 한 학원을 포화시켜도 같은 Cell의 다른 학원 p95 응답시간이 허용 범위인지 확인한다.
8. Task 메모리에 스레드 스택과 Direct buffer 여유가 남는지 확인한다.
9. 결과에 따라 `runtime-profiles.yml` 값을 변경하고 근거·시험 일자를 함께 기록한다.

---

## 24. RDS Cell 2 추가 기준과 절차

다음 중 하나가 반복되면 Cell 2를 검토한다.

- 쿼리 개선 후에도 피크 RDS CPU가 지속적으로 높다.
- DB Load가 vCPU 용량을 지속적으로 초과한다.
- 연결 예산 때문에 신규 학원을 넣을 수 없다.
- 한 학원의 보고서 작업이 다른 학원 지연시간에 반복적으로 영향을 준다.
- 단일 RDS 장애 시 영향을 받는 학원 수가 사업적으로 너무 커졌다.
- 더 큰 RDS 한 대보다 두 Cell의 비용·운영 위험이 유리하다.

절차:

1. `mudo-prod-rds-cell-2`와 별도 서브넷 그룹·알람 생성
2. Cell 2의 app/migrator 사용자 정책 준비
3. 신규 학원을 우선 Cell 2에 배치
4. 기존 학원 이관 시 점검 시간과 데이터 동기화 방식 결정
5. 학원의 SSM DB_URL을 Cell 2로 변경
6. 새 Task를 띄워 연결 확인
7. 쓰기 차단·최종 동기화 후 전환
8. 이전 DB는 검증 기간 동안 보존
9. 롤백 가능 기간이 지난 후 제거

RDS Cell 2는 같은 DB에 쓰는 두 번째 writer가 아니다. 서로 다른 학원 그룹을 담당하는 독립 DB 서버다.

---

## 25. 장애 대응 Runbook

### ECS Task 반복 종료

1. ECS Service Events 확인
2. Grafana Loki에서 종료 전 애플리케이션 로그 확인
3. OOMKilled 여부 확인
4. 새 Revision 문제면 이전 Revision으로 롤백
5. 메모리 부족이면 원인 분석 후 공통 런타임 프로필 또는 Task 메모리 조정

### EC2 장애

1. ASG가 대체 인스턴스를 생성하는지 확인
2. ECS Pending Task 확인
3. Capacity Provider Max 도달 여부 확인
4. 새 인스턴스 ECS Agent 등록 확인
5. ALB Target Healthy 확인

### RDS 과부하

1. 학원별 요청량과 Hikari 사용량 확인
2. WAF와 앱 Rate Limit 확인
3. 무거운 쿼리와 Lock 확인
4. 공격 학원 일시 제한
5. 필요하면 해당 ECS Service desired count를 0으로 내려 격리
6. 다른 학원의 정상 여부 확인
7. 사양 상향 또는 Cell 분리 여부 결정

### 앱 요청 스레드·Hikari Pool 포화

1. 해당 학원의 Tomcat busy/max와 Hikari active/max/pending을 같은 시간축으로 비교
2. Hikari pending만 높으면 느린 쿼리·Lock·연결 누수부터 확인
3. Tomcat과 Hikari가 모두 높으면 Rate Limit과 무거운 작업 동시 수 확인
4. 비동기 대기열이 높으면 일반 요청과 무거운 작업 Executor가 분리됐는지 확인
5. 단순히 Pool을 키우기 전에 RDS Cell의 남은 연결 예산 확인
6. 상향이 필요하면 `runtime-profiles.yml` 수정, 용량 검사, 부하 테스트, 순차 배포 순으로 진행
7. 특정 학원만 반복 포화되면 결제 플랜과 무관하게 Rate Limit 강화, 전용 Task 증설 또는 별도 Cell 이관 검토

### 모니터링 EC2 또는 대시보드 장애

1. CloudWatch의 `StatusCheckFailed`, CPU, 메모리, 디스크 알람 확인
2. ALB `mudo-prod-tg-monitoring` Health 상태 확인
3. Session Manager로 접속해 Docker와 Compose 서비스 상태 확인
4. EBS가 정상 마운트됐는지와 디스크 100% 여부 확인
5. 설정 문제면 Git의 이전 검증 버전으로 되돌려 Compose 재기동
6. 인스턴스 하드웨어 문제면 EC2 Recover 또는 새 인스턴스에 Data EBS 연결
7. 복구 중 AWS 상태는 CloudWatch, ECS, RDS 콘솔에서 직접 확인

### Alloy·Prometheus·Loki 수집 중단

1. ECS의 `mudo-prod-alloy-daemon`이 각 Container Instance마다 1개인지 확인
2. `/mudo/prod/monitoring` CloudWatch 로그에서 Alloy 오류 확인
3. `monitoring.mudo.internal` DNS와 9090/3100 Security Group 확인
4. Prometheus remote write queue와 Loki write 오류 확인
5. Loki 또는 Prometheus 디스크 사용률과 retention 동작 확인
6. Docker Socket Mount와 Alloy 이미지 Digest가 변경되지 않았는지 확인
7. 수집 공백 시간과 영향을 기록하고 필수 감사 이벤트는 RDS/S3 원장으로 대조

### 악성 학원·사용자

1. 토큰과 세션 폐기
2. 학원/사용자 Rate Limit 강화 또는 계정 정지
3. WAF IP·경로 규칙 적용
4. 실행 중인 무거운 DB 쿼리 종료 검토
5. 증거 로그 보존
6. 피해 학원과 영향 범위 기록

---

## 26. 구축 완료 체크리스트

### 계정

- [ ] 공유 IAM 사용자 Access Key 0개
- [ ] 공유 IAM 사용자 MFA 사용 여부 확인 또는 조회 불가 제약 기록
- [ ] 기존 AWS 서비스 연결 역할 미수정 확인
- [ ] MUDO 전용 역할 생성 가능 여부 검증
- [ ] Ops/Admin 역할 분리 불가 위험과 향후 개선 과제 기록
- [ ] 변경 이슈 기록 절차 마련

### 네트워크

- [ ] ALB만 public subnet
- [ ] ECS EC2는 private app subnet
- [ ] RDS는 private DB subnet
- [ ] SSH 미개방
- [ ] RDS 인터넷 미공개
- [ ] 모니터링 EC2 Public IP 없음
- [ ] Grafana 3000은 ALB에서만 허용
- [ ] Prometheus 9090·Loki 3100은 ECS SG에서만 허용

### ECS

- [ ] Capacity Provider managed scaling
- [ ] targetCapacity 85
- [ ] managed draining/protection
- [ ] ASG Max 설정
- [ ] binpack memory
- [ ] 학원별 Task CPU·메모리 제한
- [ ] 모든 학원에 동일한 Tomcat·비동기 Executor 상한과 유한 대기열
- [ ] 결제 플랜별 유료 엔드포인트 Entitlement와 호출량 제한
- [ ] 스레드 포화 시 거부 정책과 429/503 처리
- [ ] Deployment Circuit Breaker와 rollback

### 데이터

- [ ] 학원별 DB/User
- [ ] 앱과 migrator 계정 분리
- [ ] 학원별 DB Pool 상한
- [ ] RDS 정상 Pool 합 + 신규 Pool + 순차 배포 Surge Pool 용량 검사
- [ ] Hikari connection timeout과 쿼리·트랜잭션 timeout
- [ ] 암호화·백업·삭제 방지
- [ ] 실제 복구 시험

### 보안

- [ ] WAF ALB 연결
- [ ] Managed Rules Count 검증 후 Block
- [ ] IP Rate Limit
- [ ] 학원·사용자 Rate Limit
- [ ] 무거운 작업 동시 제한
- [ ] 자동 확장 최대값

### 배포

- [ ] develop Docker Hub 게시
- [ ] main ECR SHA 게시
- [ ] GitHub OIDC main 브랜치 제한
- [ ] ECS 순차 배포
- [ ] Migration Task
- [ ] 자동·수동 롤백 시험

### 관측성

- [ ] 모니터링 전용 EC2와 암호화 gp3 EBS
- [ ] Grafana·Prometheus·Loki·Alertmanager 버전/Digest 고정
- [ ] Alloy ECS Daemon Service가 모든 ECS 호스트에서 1개씩 실행
- [ ] ALB `/actuator/prometheus` 403과 Alloy 내부 Scrape 성공
- [ ] CloudWatch AWS 기본 지표와 모니터링 EC2 독립 알람
- [ ] Prometheus 학원별 커스텀 지표
- [ ] Loki JSON 로그와 저카디널리티 Label
- [ ] Tomcat·Hikari·비동기 Executor·WebSocket 포화 지표
- [ ] Grafana Prometheus·Loki·CloudWatch 데이터 소스
- [ ] 비즈니스·운영 대시보드 분리
- [ ] Alertmanager·CloudWatch Slack 알림 경로 분리
- [ ] EBS Snapshot과 실제 복구 시험
- [ ] Prometheus 15일·Loki 30일 보존 검증

---

## 27. 공식 참고 문서

- [Amazon ECS Capacity Provider 생성](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/create-capacity-provider-console-v2.html)
- [ECS Cluster Auto Scaling](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/cluster-auto-scaling.html)
- [ECS Managed Scaling 동작](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/managed-scaling-behavior.html)
- [ECS Task Placement와 binpack](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task-placement-strategies.html)
- [ECS와 Application Load Balancer](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/alb.html)
- [ECS Deployment Circuit Breaker](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/deployment-circuit-breaker.html)
- [ECS Parameter Store 시크릿 주입](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/secrets-envvar-ssm-paramstore.html)
- [ECS Task Execution Role](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task_execution_IAM_role.html)
- [Application Load Balancer 생성](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/create-application-load-balancer.html)
- [ACM Public Certificate 생성](https://docs.aws.amazon.com/acm/latest/userguide/acm-public-certificates.html)
- [Route 53에서 ALB로 라우팅](https://docs.aws.amazon.com/Route53/latest/DeveloperGuide/routing-to-elb-load-balancer.html)
- [AWS WAF Rate-based aggregation](https://docs.aws.amazon.com/waf/latest/developerguide/waf-rule-statement-type-rate-based-aggregation-options.html)
- [RDS Storage Autoscaling](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/USER_PIOPS.Autoscaling.html)
- [RDS Read Replica](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/USER_ReadRepl.html)
- [RDS Proxy 다중 사용자 고려사항](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/rds-proxy-best-practices.workload-considerations.html)
- [Spring Boot 공통 Application Properties](https://docs.spring.io/spring-boot/appendix/application-properties/index.html)
- [MySQL 서버 연결 처리 스레드](https://dev.mysql.com/doc/extending-mysql/8.0/en/mysql-threads.html)
- [CloudWatch Container Insights for ECS](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/deploy-container-insights-ECS-cluster.html)
- [ECS EC2 Task Definition 로그 드라이버](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task_definition_parameters_ec2.html)
- [Grafana Alloy Docker Service Discovery](https://grafana.com/docs/alloy/latest/reference/components/discovery/discovery.docker/)
- [Grafana Alloy Docker 로그 수집](https://grafana.com/docs/alloy/latest/reference/components/loki/loki.source.docker/)
- [Grafana Alloy Prometheus Scrape](https://grafana.com/docs/alloy/latest/reference/components/prometheus/prometheus.scrape/)
- [Grafana Loki 시작하기](https://grafana.com/docs/loki/latest/get-started/)
- [Prometheus 로컬 저장소](https://prometheus.io/docs/prometheus/latest/storage/)
- [GitHub OIDC IAM Role 구성](https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles_create_for-idp_oidc.html)
- [S3 Multi-tenant Bucket Pattern](https://docs.aws.amazon.com/AmazonS3/latest/userguide/common-bucket-patterns.html)

---

## 28. 최종 설계 결정 요약

| 영역 | 결정 |
|---|---|
| 외부 진입 | Route 53 + ACM + WAF + ALB |
| 앱 실행 | ECS on EC2 |
| EC2 확장 | ASG Capacity Provider Managed Scaling |
| 배치 | 학원별 Service/Task + memory binpack |
| CPU | 컨테이너 CPU share, 유휴 CPU 버스트 허용 |
| 메모리 | reservation + hard limit |
| 앱 동시성 | 모든 학원에 동일한 Tomcat·bounded Executor·무거운 작업 상한 |
| 운영 배포 | main → ECR SHA → ECS Rolling Update |
| 로컬 연동 | develop → Docker Hub |
| DB | 공유 RDS Cell, 학원별 Database/User |
| DB 확장 | 쿼리 개선 → 사양 상향 → Read Replica → Cell 추가 |
| DB 보호 | 학원별 Pool·배포 Surge 예산·동시 작업·쿼리 시간 제한 |
| 파일 | S3 두 버킷 + 학원별 Prefix·Task Role |
| 공용 Drive | 학원별 Shared Drive/연동 정보 |
| 공격 방어 | WAF + 앱 Rate Limit + DB Connection Budget |
| AWS 기본 지표 | CloudWatch |
| 앱 지표 | Alloy → Prometheus |
| 앱 로그 | Alloy → Loki |
| 대시보드 | 전용 모니터링 EC2의 Self-hosted Grafana |
| Grafana 로그인 | 공유 로컬 계정, Anonymous/Sign-up 비활성화, SSM 비밀번호 관리 |
| 알림 | Prometheus → Alertmanager → Slack, CloudWatch → SNS → Slack |
| 초기 모니터링 저장소 | 단일 EC2 gp3, Prometheus 15일·Loki 30일 |
| 관측성 확장 | Loki S3 전환, 필요 시 Thanos 또는 Mimir 중 하나 선택 |
| Redis | 운영 WebSocket 사용 시 초기 도입, 그 외에는 Task 다중화·분산 제한 시 도입 |
| 결제 플랜 | 유료 엔드포인트·기능·쿼터를 애플리케이션 Entitlement로 판정 |
| 설정 원본 | `runtime-profiles.yml` + `tenants.yml` + `cells.yml`에서 Task Definition 생성 |

이 구조는 유휴 자원을 줄이면서도 학원별 자원 정책과 장애 범위를 관리하는 초기 SaaS Cell 아키텍처다. 규모가 커지면 EC2와 RDS를 무한히 크게 만드는 것이 아니라 동일한 Cell을 추가해 학원 그룹을 분산한다.
