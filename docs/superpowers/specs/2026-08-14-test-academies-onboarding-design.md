# 테스트 학원(academy-b, academy-c) 온보딩 리허설 설계

> **작업 유형**: 신규 기능 아님 — 운영 AWS 인프라에 실제 리소스를 추가하는 인프라 작업.
> **관련 문서**: `docs/2026-08-03-academy-groupware-multi-tenant-aws-infra-guide.md` 22장(새 학원 온보딩 절차), 23장(자동 확장 검증 시나리오).

## 1. 목표

`docs/2026-08-03-...` 22장의 신규 학원 온보딩 절차를 실제로 두 번(academy-b, academy-c) 수행해서 다음을 검증한다.

1. **바인팩 배치**: ECS Capacity Provider가 새 학원 Task를 기존 EC2 인스턴스에 실제로 몰아서 배치하는지.
2. **설정 반영**: `runtime-profiles.yml`의 `shared-default` 값(CPU/메모리/Tomcat/Hikari/비동기 Executor 상한)이 새 Task Definition에 실제로 주입되는지.
3. **S3/RDS 테넌트 격리**: academy-b가 academy-a/c의 S3 Prefix·DB에 접근할 수 없는지, 반대도 마찬가지인지.

이번 작업은 **실제 신규 고객 온보딩이 아니라 리허설**이다. 향후 실 학원이 들어오기 전, 그리고 학원을 6~7개까지 늘리는 확장을 준비하기 위한 첫 단계로, academy-b/c 2개로 먼저 검증하고 문제가 없으면 같은 절차를 반복해서 확장한다. **검증 후에도 academy-b/c는 삭제하지 않고 유지**한다 (확장의 발판으로 사용).

## 2. 현재 상태 확인 (조사 결과)

- 기존 테넌트는 `academy-a` 1개뿐이며 `cell-1`에서 운영 중이다.
- 로컬 저장소(`Team1-MUDO-BackEnd`, 브랜치 `fix/inject-app-frontend-url`)가 `origin/develop` 대비 `infra/` 변경만 16개 커밋 뒤처져 있어서, 이 작업은 **`origin/develop` 기준 새 워크트리**(`test-academies-onboarding`, 경로 `C:/lecture/module06/Team1-MUDO-BackEnd-test-academies`)에서 진행한다.
- `develop` 기준 `tenants.yml`은 학원마다 `finance_s3_bucket` 필드가 필수이고, `platform_dashboard_host: true`인 테넌트가 정확히 1개 있어야 한다(현재 academy-a).
- **라이브 ECS 상태(AWS CLI로 직접 조회, 2026-08-14 기준)**:
  - `mudo-prod-asg`: Desired=2, Min=1, Max=4 (인스턴스 2대 이미 등록됨, `mudo-prod-ai-asg`는 별도의 AI 관련 ASG로 무관)
  - 인스턴스별 등록 자원: 각 cpu 2048 / mem 1913 (t3.small 기준, `cells.yml`과 일치)
  - 인스턴스 1: 잔여 cpu 1420 / mem 1017 (academy-a Task cpu500/mem640 + Alloy Daemon cpu128/mem256 배치됨)
  - 인스턴스 2: 잔여 cpu 1920 / mem 1657 (거의 비어 있음)
  - `mudo-prod-capacity-provider`: Managed Scaling `ENABLED` (targetCapacity 85%, minStep 1, maxStep 10000) — 용량 부족 시 ASG가 자동으로 스케일아웃하는 구조가 이미 살아있음을 확인.
- `infra/cells.yml`의 `ecs_registered_cpu`/`ecs_registered_memory_mib`(2048/1913)는 주석상 **"t3.small 1대 기준"** — 실제 라이브 인스턴스는 2대인데 값이 갱신되지 않은 상태. 이 값을 그대로 두면 `deploy_production.py`의 용량 검사가 학원 3개(a+b+c) 배포를 차단한다(§3-2 계산 참고).
- **S3**: `mudo-prod-staff-635249349258`만 실존. `mudo-prod-finance-635249349258`는 아직 생성되지 않았지만, 현재 운영 중인 academy-a Task(`mudo-prod-tenant-academy-a:13`, 활성)에는 이미 이 존재하지 않는 버킷명이 `AWS_S3_FINANCE_BUCKET_NAME` 환경변수로 주입돼 있다. 이 버킷을 실제로 쓰는 도메인 코드가 아직 없어(전자결재/법인카드 등에 S3 연동 코드 없음) 지금은 무해하지만, academy-a 때부터 있던 기존 갭이다.
- **DNS/인증서**: `*.ieum.store` 와일드카드 ACM 인증서(ISSUED)와 Route53 와일드카드 A 레코드가 이미 존재 — 새 서브도메인 추가 DNS/ACM 작업 불필요.
- **WAF**: `mudo-prod-web-acl` 존재, ALB 레벨에 연결되어 있어 새 Host 규칙에도 자동 적용됨(테넌트별 추가 설정 불필요).
- **Google Drive 연동(가이드 18-4절)**: 코드베이스 전체에 Shared Drive/서비스 계정 관련 구현이 없다. `docs/2026-08-07-s3-direct-upload-architecture.md`(8/7, 가이드보다 나중 작성)에서 파일 저장을 S3 직접 업로드 방식으로 확정하면서 사실상 폐기된 설계로 판단 — 이번 온보딩에서 **제외**한다.

## 3. 설계 결정

### 3-1. 테넌트 정체성

| 필드 | academy-b | academy-c |
|---|---|---|
| `code` | `academy-b` | `academy-c` |
| `billing_plan` | `basic` | `basic` |
| `runtime_profile` | `shared-default` | `shared-default` |
| `cell` | `cell-1` | `cell-1` |
| `service` | `mudo-prod-svc-academy-b` | `mudo-prod-svc-academy-c` |
| `task_family` | `mudo-prod-tenant-academy-b` | `mudo-prod-tenant-academy-c` |
| `health_url` | `https://academy-b.ieum.store/actuator/health` | `https://academy-c.ieum.store/actuator/health` |
| `s3_bucket` | `mudo-prod-staff-635249349258` (공용, prefix로 격리) | 동일 |
| `finance_s3_bucket` | `mudo-prod-finance-635249349258` (공용, prefix로 격리, §3-3에서 신규 생성) | 동일 |
| `platform_dashboard_host` | 미설정(false) | 미설정(false) |

### 3-2. `cells.yml` 용량 값 갱신

현재 값(cpu 2048/mem 1913, 1대 기준)으로 학원 3개(a+b+c, 전부 `shared-default`)를 검증하면:

```
normal_cpu = 500*3 = 1500,  required_cpu = 1500 + 500 = 2000
normal_mem = 640*3 = 1920,  required_mem = 1920 + 640 = 2560
cpu_budget = 2048*0.80 = 1638   → 2000 > 1638 → 검증 실패
mem_budget = 1913*0.80 = 1530   → 2560 > 1530 → 검증 실패
```

실제 라이브 인스턴스 2대 기준(cpu 4096/mem 3826)으로 갱신하면:

```
cpu_budget = 4096*0.80 = 3276   → 2000 ≤ 3276 통과
mem_budget = 3826*0.80 = 3060   → 2560 ≤ 3060 통과
```

→ **새 EC2를 미리 늘릴 필요 없이, `cells.yml`의 등록 자원 값만 실제 값으로 갱신하면 3개 테넌트가 안전하게 들어간다.** RDS 연결 예산(`rds_max_connections: 144 * 0.70 = 100`)은 3개 테넌트 기준 `hikari_max_pool_size`(6) 합 + surge = 24로 여유 있게 통과하므로 별도 조정 불필요.

### 3-3. finance 버킷 신규 생성 (옵션 1 채택)

`mudo-prod-finance-635249349258` 버킷을 가이드 18-1절 스펙대로 생성한다.

- Block all public access 활성화
- Versioning 활성화
- Default encryption(SSE-S3 또는 SSE-KMS) 활성화
- HTTPS가 아닌 요청을 거부하는 Bucket Policy
- (Lifecycle은 이번 리허설 범위에서는 기본값 유지, 실제 학원 데이터가 들어가기 전에 별도 결정)

이 버킷은 academy-a/b/c가 공용으로 쓰며, 18-2/18-3절대로 `tenants/<tenant-code>/...` Prefix와 Task Role의 `s3:prefix` 조건으로 격리한다.

### 3-4. 22장 온보딩 절차 매핑

| 단계 | 내용 | 처리 |
|---|---|---|
| 1 | tenant-code 중복 확인 | `tenants.yml` 확인, `academy-b`/`academy-c` 미사용 확인됨 |
| 2 | 결제 플랜 결정 | `basic` (§3-1) |
| 3 | `tenants.yml`에 추가 + Cell 지정 | `cell-1` (§3-1) |
| 4 | 입점 검사 스크립트로 용량 확인 | `deploy_production.py --validate-only`, §3-2 갱신 후 실행 |
| 5 | 용량 검사 통과 시 PR 승인 | 팀 PR 프로세스 그대로 따름 |
| 6 | 학원 DB·app/migrator 계정 생성 | RDS `mudo-prod-rds-cell-1`에 학원별 DB+계정 2세트 생성 |
| 7 | Flyway Migration 실행 | `deploy_production.py`의 migration task 실행 경로 사용 |
| 8 | SSM 학원별 파라미터 생성 | academy-a와 동일 키셋 19개 × 2세트 (§4 참고) |
| 9 | S3 Prefix·Task Role 생성 | `tenants/academy-b/*`, `tenants/academy-c/*` 각각 staff+finance 버킷 |
| 10 | Google Shared Drive 준비 | **제외** (§2 근거) |
| 11 | Task Definition 생성(`shared-default`) | `deploy_production.py`가 자동 렌더링 |
| 12 | Target Group 생성 | `mudo-prod-tg-academy-b`, `-c` |
| 13 | ECS Service 생성 | `mudo-prod-svc-academy-b`, `-c` |
| 14 | ALB Host rule 생성 | `academy-b.ieum.store`, `academy-c.ieum.store` |
| 15 | DNS wildcard로 접근 확인 | 이미 존재, 접근만 확인 |
| 16 | WAF·Rate Limit 적용 확인 | 기존 Web ACL이 ALB 레벨이라 자동 적용, 확인만 |
| 17 | Alloy가 새 Task 발견하는지 확인 | Daemon Task라 자동, 확인만 |
| 18 | Grafana 대시보드 변수 확인 | 테넌트 라벨 기반 자동 인식 여부 확인 |
| 19 | Smoke Test, 동시성/유료 엔드포인트 확인 | `deploy_production.py`의 smoke test + 수동 확인 |
| 20 | 고객에게 URL 전달 | 리허설이므로 **제외** |

## 4. 검증 계획 (사용자가 명시한 3가지)

1. **바인팩**: `aws ecs describe-tasks`/`describe-container-instances`로 academy-b/c Task가 어느 EC2에 배치됐는지 확인 — 기존 인스턴스(특히 잔여 자원이 적어 몰아 담기 유리한 쪽)에 쌓이는지, 불필요하게 새 인스턴스를 유발하지 않는지 확인.
2. **설정 반영**: 새로 등록된 Task Definition의 `cpu`/`memoryReservation`/`memory`와 컨테이너 환경변수(`SERVER_TOMCAT_THREADS_MAX`, `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE` 등)가 `shared-default` 프로필 값과 정확히 일치하는지 확인. 실행 중인 Task에 실제로 그 값으로 동작하는지(actuator 등) 확인.
3. **S3/RDS 격리**:
   - S3: academy-b의 Task Role로 academy-a/c의 Prefix에 접근 시도 → 거부되는지 확인 (`AccessDenied`).
   - RDS: academy-b의 DB 계정으로 academy-a/c의 DB에 접근 시도 → 거부되는지 확인.

## 5. 사후 처리

- 검증 통과 시 academy-b/c는 **유지**한다 (제거하지 않음). 이후 6~7개까지 확장할 때 동일 절차를 반복하는 기반이 된다.
- 검증 중 문제가 발견되면(예: 바인팩이 기대대로 안 되거나, 격리가 뚫리는 경우) 즉시 원인 분석 후 수정, 필요시 해당 학원 ECS Service/DB/SSM만 롤백(제거)하고 재시도.

## 6. 범위 밖

- RDS Cell 2 신설 (가이드 24장 — 이번엔 불필요, cell-1 하나로 충분)
- Google Drive 연동 (§2, §3-4 단계 10)
- 실 고객 URL 전달 (§3-4 단계 20)
- 자동 확장 시나리오 중 "새 EC2 실제로 뜨는 것"까지의 검증 — academy-b/c 2개 추가만으로는 기존 2대 용량 안에서 소화되므로(§3-2), 이번 리허설로는 확인되지 않음. 6~7개로 확장할 때 다시 계산해서 필요하면 그때 검증한다.
