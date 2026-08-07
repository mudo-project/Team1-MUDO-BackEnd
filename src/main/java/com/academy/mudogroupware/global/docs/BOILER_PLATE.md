# 🧰 Global Retention Scheduler 보일러플레이트

> 업데이트: 2026-08-06 · 최초 작성. 아직 구현되지 않은 **설계 가이드/보일러플레이트**입니다 — 이 문서에 있는 코드는 실제 코드베이스에 없습니다. 실제 삭제성 배치 Job이 필요해지는 시점에 이 문서를 그대로 따라 구현하면 됩니다.

## 🎯 언제 쓰는가

여러 도메인의 **삭제·정리 배치 작업**을 하나의 스케줄러가 공통으로 실행하고, 실제 정책과 데이터 처리는 각 도메인이 책임지는 구조입니다.

```text
Global Scheduler
  → RetentionJob 목록 자동 주입
    → 도메인별 RetentionJob 구현체
      → 도메인 RetentionService
        → Port / Repository
          → DB 삭제 처리
```

### ✅ 적용 대상 예시

- 소프트 삭제 데이터의 일정 기간 후 Hard Delete
- 만료된 알림 삭제
- 탈퇴 회원 데이터 삭제
- 만료된 신고 그룹 삭제
- 종료된 채팅방·메시지 삭제

### ❌ 적용하지 않는 대상 예시

- PT 시작 전 알림 발송
- 결제 만료 처리
- 특정 도메인만 사용하는 실시간성 높은 작업
- **상태 전환 배치(삭제가 아닌 배치)** — 예: 업무 자동 지연 처리(`WorkspaceTaskDelayScheduler`, `workspace` 도메인). 이 패턴은 `RetentionJobResult`가 `deletedChildCount`/`deletedParentCount` 등 **삭제 건수 중심**으로 설계되어 있어, 삭제가 아닌 상태 변경 배치를 억지로 여기에 태우면 필드 의미가 왜곡됩니다. `workspace` 도메인의 업무 자동 지연 처리는 이 이유로 Global 패턴을 쓰지 않고 도메인 전용 `@Scheduled`를 그대로 유지했습니다 — 실제 판단 사례는 [workspace/docs/REVISION.md](../../workspace/docs/REVISION.md)의 "업무 자동 지연 스케줄러" 관련 항목을 참고해주세요.

위 "적용하지 않는 대상"은 실행 주기와 정책이 독립적이므로, 각 도메인 Scheduler로 유지하는 편이 더 자연스럽습니다.

## 📐 설계 원칙

| 구분 | 책임 |
| --- | --- |
| Global | 스케줄 활성화, 공통 실행 시각 생성, Job 순회, 실패 격리, 공통 결과 로그 |
| Domain Job | Global 계약 구현, 도메인 Service 호출 |
| Domain Service | 보관 기간 계산, 후보 조회, 삭제 순서, 트랜잭션 |
| Domain Properties | 보관 기간·배치 크기 정책, 값 검증 |
| Port / Adapter | DB 조회·삭제 구현 |

핵심 원칙:

- Global은 도메인 테이블·정책을 알지 않는다.
- 도메인은 `@Scheduled`를 직접 선언하지 않는다.
- Job 하나의 실패가 다른 Job 실행을 막으면 안 된다.
- 트랜잭션은 Global Scheduler가 아닌 도메인 Service에 둔다.
- 삭제 기간과 배치 크기는 비밀값이 아니므로, 도메인 코드 기본값으로 관리한다.
- 공통 실행 시각은 `Clock`으로 한 번만 만들고 모든 Job에 전달한다.

## 🗂️ 권장 패키지 구조

```text
global
├── infrastructure
│   └── config
│       └── SchedulingConfig.java        # 이미 존재 — 새로 만들지 않고 재사용
└── scheduler
    ├── RetentionJob.java
    ├── RetentionJobResult.java
    └── GlobalRetentionScheduler.java
{domain}
├── application
│   ├── retention
│   │   ├── {Domain}RetentionJob.java
│   │   ├── {Domain}RetentionService.java
│   │   └── {Domain}RetentionProperties.java
│   └── port
│       └── out
│           └── {Domain}RetentionPort.java
└── infrastructure
    └── persistence
        └── {Domain}RetentionAdapter.java
```

도메인 내부 구조가 이미 `adapter/in/scheduler`를 사용하는 경우에는 Job만 해당 위치에 두어도 됩니다.

> ⚠️ **`SchedulingConfig`는 프로젝트 전체에 한 번만 선언합니다.** 다른 도메인이 이미 `@EnableScheduling`을 선언했는지 먼저 확인하세요(예: `workspace.infrastructure.scheduler` 관련 설정 — grep `@EnableScheduling`으로 전체 소스를 확인). 이미 있다면 그 클래스를 재사용하고 새로 만들지 않습니다. `@Scheduled`가 붙은 메서드는 서로 다른 컴포넌트에 몇 개든 독립적으로 존재할 수 있으며, `@EnableScheduling`은 그 처리 기능을 켜는 스위치 하나일 뿐입니다.

## 📜 Global 공통 계약

### `RetentionJob`

```java
public interface RetentionJob {
    // 작업 식별 역할
    String name();
    // 기준 시각을 받아 도메인 정리 작업을 실행하는 역할
    RetentionJobResult run(LocalDateTime now);
}
```

- Spring은 `RetentionJob` 구현체를 모두 찾아 `List<RetentionJob>`으로 주입합니다.
- 새 도메인 Job을 추가해도 Global Scheduler 수정이 필요 없습니다.
- `now`를 Global에서 전달하므로, 같은 실행 주기 안의 모든 Job이 동일한 기준 시각을 사용합니다.

### `RetentionJobResult`

```java
public record RetentionJobResult(
        String jobName,
        int candidateCount,
        int deletedChildCount,
        int deletedParentCount
) {
    public static RetentionJobResult empty(String jobName) {
        return new RetentionJobResult(jobName, 0, 0, 0);
    }
}
```

| 필드 | 의미 |
| --- | --- |
| `jobName` | 실행한 도메인 작업 이름 |
| `candidateCount` | 삭제 후보 수 |
| `deletedChildCount` | 자식 데이터 삭제 수 |
| `deletedParentCount` | 부모 데이터 삭제 수 |

자식 테이블이 없는 작업은 `deletedChildCount`를 `0`으로 둡니다. 이 결과 객체를 공통으로 두면 모든 도메인이 동일한 형식으로 로그를 남길 수 있습니다.

### `GlobalRetentionScheduler`

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class GlobalRetentionScheduler {
    private final List<RetentionJob> retentionJobs;
    private final Clock clock;

    @Scheduled(
            cron = "${app.scheduler.retention.cron}",
            zone = "${app.scheduler.retention.zone}"
    )
    public void runRetentionJobs() {
        LocalDateTime now = LocalDateTime.now(clock);
        if (retentionJobs.isEmpty()) {
            log.debug("event=retention_jobs_empty");
            return;
        }
        for (RetentionJob job : retentionJobs) {
            runJob(job, now);
        }
    }

    private void runJob(RetentionJob job, LocalDateTime now) {
        try {
            RetentionJobResult result = job.run(now);
            log.info(
                    "event=retention_job_succeeded jobName={} candidateCount={} deletedChildCount={} deletedParentCount={}",
                    result.jobName(), result.candidateCount(),
                    result.deletedChildCount(), result.deletedParentCount());
        } catch (Exception exception) {
            // 한 도메인 실패가 다른 도메인 작업을 중단하지 않도록 격리하는 역할
            log.error("event=retention_job_failed jobName={}", job.name(), exception);
        }
    }
}
```

핵심 포인트:

- `List<RetentionJob>`: 구현체 자동 수집
- `Clock`: 테스트 가능한 실행 시각 생성 — `global`의 기존 `TimeConfig.clock()` 빈을 그대로 주입받아 재사용합니다(새로 만들지 않습니다).
- `try/catch`: 작업별 실패 격리
- `@Scheduled`: 단 하나의 공통 Cron 진입점
- Global에는 `@Transactional`을 붙이지 않습니다 — 하나의 Job 실패가 전체 실행 단위를 롤백하거나, 트랜잭션 범위가 과도하게 커질 수 있습니다.

### `SchedulingConfig`

```java
@Configuration
@EnableScheduling
public class SchedulingConfig {}
```

`src/main/java/com/academy/mudogroupware/global/infrastructure/config/SchedulingConfig.java`에 이미 존재합니다(업무 자동 지연 스케줄러가 최초 선언). 프로젝트 전체에서 한 번만 선언하므로 새로 만들지 말고 이 파일을 그대로 재사용합니다(위 경고 참고).

## 🏗️ 도메인별 구현 예시

### 도메인 정책 클래스 — `{Domain}RetentionProperties`

```java
@Component
public class NotificationRetentionProperties {
    private static final long DEFAULT_PERIOD_DAYS = 7L;
    private static final int DEFAULT_BATCH_SIZE = 500;
    private final long periodDays;
    private final int batchSize;

    public NotificationRetentionProperties() {
        this(DEFAULT_PERIOD_DAYS, DEFAULT_BATCH_SIZE);
    }

    public NotificationRetentionProperties(long periodDays, int batchSize) {
        if (periodDays <= 0) {
            throw new IllegalArgumentException("Retention periodDays는 1 이상이어야 합니다.");
        }
        if (batchSize <= 0 || batchSize > 500) {
            throw new IllegalArgumentException("Retention batchSize는 1 이상 500 이하여야 합니다.");
        }
        this.periodDays = periodDays;
        this.batchSize = batchSize;
    }

    public long periodDays() { return periodDays; }
    public int batchSize() { return batchSize; }

    // 삭제 기준 시각 계산 역할
    public LocalDateTime threshold(LocalDateTime now) {
        return now.minusDays(periodDays);
    }
}
```

| 값 | 권장 위치 | 이유 |
| --- | --- | --- |
| Cron, Zone | `application.yml` 또는 환경 변수 | 운영 환경별 실행 시각 변경 가능 |
| 보관 기간 | 도메인 Properties 기본값 | 도메인 정책 |
| 배치 크기 | 도메인 Properties 기본값 | DB 부하 제어 정책 |
| API Key, DB 비밀번호 | 환경 변수 또는 Secret | 민감 정보 |

보관 기간과 배치 크기는 비밀값이 아니므로, 모든 값을 GitHub Secrets나 배포 환경 변수로 빼지 않습니다.

### 도메인 Job 구현체 — `{Domain}RetentionJob`

```java
@Component
@RequiredArgsConstructor
public class NotificationRetentionJob implements RetentionJob {
    private final NotificationRetentionService notificationRetentionService;

    @Override
    public String name() {
        return NotificationRetentionService.JOB_NAME;
    }

    @Override
    public RetentionJobResult run(LocalDateTime now) {
        return notificationRetentionService.hardDeleteExpiredNotifications(now);
    }
}
```

Job 구현체의 책임은 최소화합니다: `Global Scheduler → Domain Job → Domain Service`. 정책 계산 금지, Repository 직접 호출 금지, 트랜잭션 선언 금지 — 단순 위임만 수행합니다.

### 도메인 Service 구현 — `{Domain}RetentionService`

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationRetentionService {
    public static final String JOB_NAME = "notification_retention";
    private final NotificationRetentionProperties properties;
    private final NotificationRetentionPort notificationRetentionPort;

    @Transactional
    public RetentionJobResult hardDeleteExpiredNotifications(LocalDateTime now) {
        LocalDateTime threshold = properties.threshold(now);
        List<Long> candidateIds =
                notificationRetentionPort.findHardDeleteCandidateIds(threshold, properties.batchSize());

        if (candidateIds.isEmpty()) {
            log.info("event=notification_retention_empty threshold={} periodDays={} batchSize={}",
                    threshold, properties.periodDays(), properties.batchSize());
            return RetentionJobResult.empty(JOB_NAME);
        }

        int deletedCount = notificationRetentionPort.hardDeleteByIds(candidateIds, threshold);

        return new RetentionJobResult(JOB_NAME, candidateIds.size(), 0, deletedCount);
    }
}
```

도메인 Service 책임: ① 기준 시각으로 삭제 임계값 계산 → ② 배치 크기만큼 후보 ID 조회 → ③ 연관 데이터가 있으면 자식부터 삭제 → ④ 부모 데이터 삭제 → ⑤ 실행 결과 반환.

### 삭제 Port 예시

```java
public interface NotificationRetentionPort {
    List<Long> findHardDeleteCandidateIds(LocalDateTime threshold, int batchSize);
    int hardDeleteByIds(List<Long> candidateIds, LocalDateTime threshold);
}
```

권장 방식: ① 삭제 후보 ID를 제한된 개수만 조회 → ② 자식 테이블이 있으면 자식부터 삭제 → ③ 부모 테이블 삭제 → ④ 삭제 조건에 threshold를 한 번 더 포함. 마지막 삭제 쿼리에 기준 시각 조건을 다시 넣으면, 후보 조회와 삭제 사이에 데이터 상태가 바뀌는 경우를 한 번 더 방어할 수 있습니다.

## ⚙️ 설정 예시

```yaml
app:
  scheduler:
    retention:
      cron: "${APP_SCHEDULER_RETENTION_CRON:0 0 3 * * *}"
      zone: "${APP_SCHEDULER_RETENTION_ZONE:Asia/Seoul}"
```

기본 예시는 매일 오전 3시 KST 실행입니다. 환경별 실행 시각 변경이 필요하면 환경 변수만 변경하고, Job별 보관 기간·배치 크기는 도메인 코드의 기본 정책을 유지합니다.

## ⏰ `Clock` 공통 설정

이미 `global`에 있습니다 — 새로 만들지 않고 그대로 주입받아 씁니다. 세부 내용은 [API.md](API.md)의 "시간 정책" 절을 참고해주세요.

```java
@Configuration
public class TimeConfig {
    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    @Bean
    public Clock clock() {
        return Clock.system(SERVICE_ZONE);
    }
}
```

`LocalDateTime.now()` 직접 호출 대신 `LocalDateTime.now(clock)`을 사용합니다. 서버 OS 시간대 변화 영향이 줄고, 테스트에서 `Clock.fixed(...)` 주입이 가능하며, 모든 Job이 동일한 기준 시각을 공유할 수 있습니다.

## 🆕 새 도메인 추가 절차

1. `{Domain}RetentionProperties` 생성
2. `{Domain}RetentionPort` 생성
3. Adapter / Repository 삭제 쿼리 구현
4. `{Domain}RetentionService` 생성
5. `{Domain}RetentionJob implements RetentionJob` 구현
6. Service 단위 테스트 작성
7. Global Scheduler 수정 없이 Spring Bean 등록 확인

새 Job을 추가할 때 Global Scheduler에 `if`, `switch`, 수동 등록 코드를 추가하지 않습니다.

## ✅ 테스트 기준

### Domain Service 테스트

- 기준 시각은 `Clock.fixed(...)` 또는 메서드 인자로 고정합니다.
- 후보가 없는 경우 `RetentionJobResult.empty(...)` 반환을 확인합니다.
- 후보가 있는 경우 후보 수·삭제 수 반환을 확인합니다.
- 배치 크기 전달을 확인합니다.
- 자식 삭제 → 부모 삭제 순서를 확인합니다.

### Global Scheduler 테스트

- 등록된 모든 `RetentionJob`의 `run(now)` 호출을 확인합니다.
- 첫 번째 Job 실패 후 다음 Job도 호출되는지 확인합니다.
- 빈 Job 목록에서 예외 없이 종료되는지 확인합니다.

## 🚨 운영 주의사항

**단일 인스턴스**: 현재 구조는 단일 애플리케이션 인스턴스에 적합합니다.

**다중 인스턴스**: 서버를 여러 대로 확장하면 모든 인스턴스가 같은 Cron을 실행할 수 있습니다. 그때만 아래 중 하나를 추가 검토합니다.

- ShedLock
- DB 기반 분산 락
- 별도 배치 서버
- Quartz Cluster

초기 프로젝트에서 단일 인스턴스라면 분산 락을 미리 넣지 않습니다.

**긴 작업**: 한 번의 배치가 오래 걸리면 배치 크기 축소 → 인덱스 점검 → 삭제 대상 ID 기반 처리 → 실행 시간·삭제 건수 모니터링 순서로 개선합니다.

## 📏 최종 규칙

```text
공통 실행 흐름과 실패 격리 → Global
도메인별 삭제 정책과 데이터 처리 → Domain
DB 접근과 쿼리 구현 → Infrastructure
```

이 구조의 목표는 모든 스케줄러를 한곳에 몰아넣는 것이 아니라, **실행 방식은 공통화하고 도메인 정책은 각 도메인에 남기는 것**입니다.

## 📝 문서 정보

- 업데이트일: `2026-08-06`
- 상태: 설계 가이드(미구현) — 실제 삭제성 배치 Job이 필요해지면 이 문서를 그대로 따라 구현합니다.
- 관련 판단 기록: [workspace/docs/REVISION.md](../../workspace/docs/REVISION.md) — 업무 자동 지연 스케줄러가 이 패턴 대신 도메인 전용 스케줄러를 선택한 이유
