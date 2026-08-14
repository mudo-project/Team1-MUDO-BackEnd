# planquota Revision

## ✅ 2026-08-14 · DatabaseQuotaAspect가 다른 도메인의 비관적 락을 무력화하던 버그

### 배경

PR(플랜별 리소스 한도 집행)의 CI에서 이 PR과 무관해 보이는 `CreateTimetableSlotConcurrencyTest`(같은 교실·시간에 슬롯 생성 동시 요청 시 정확히 1건만 성공해야 하는 테스트)가 간헐적으로 실패했다. 처음엔 이 PR이 `timetable` 도메인 파일을 전혀 건드리지 않았다는 이유로 "무관한 기존 플레이키 테스트"로 오판했으나, 로컬에서 직접 재현·이분 탐색한 결과 이 PR이 추가한 `DatabaseQuotaAspect` 때문임을 실증적으로 확인했다.

### 근본 원인

`DatabaseQuotaAspect.checkBeforeWrite()`의 포인트컷(`execution(* com.academy.mudogroupware..service..*(..))`)은 앱 전체 서비스 계층에 걸리고, `@Order`가 없어 이 어드바이스가 대상 메서드의 `@Transactional` 트랜잭션이 **이미 시작된 이후**(트랜잭션 안쪽)에 실행됐다. 그 결과:

1. 한도 체크 쿼리(`information_schema.tables` 집계)가 해당 트랜잭션의 **첫 번째 실제 쿼리**가 되어버림.
2. MySQL InnoDB의 기본 격리수준(REPEATABLE READ)은 트랜잭션 내 첫 쿼리 실행 시점에 읽기 스냅샷을 고정한다 — 이후 그 트랜잭션의 일반 `SELECT`는 전부 이 스냅샷만 본다.
3. `CreateTimetableSlotService.createSlot()`는 `findByIdForUpdate()`(비관적 락, `SELECT ... FOR UPDATE`)로 최신 상태를 잠근 뒤, 겹치는 슬롯이 있는지 **일반 SELECT**(`findAllByTimetableSetIdAndClassroomCode`)로 확인하는 구조다.
4. 한도 체크 쿼리가 락보다 먼저 실행되며 스냅샷을 조기 고정시킨 탓에, 락 자체는 정상적으로 직렬화되는데(대기 스레드가 실제로 블로킹됨을 로그로 확인) 그 이후의 충돌 확인 쿼리가 **락 획득 이전 시점의 옛 스냅샷**만 보게 됐다. 먼저 커밋한 트랜잭션이 만든 슬롯을 뒤에 락을 얻은 트랜잭션이 못 보고 "안 겹침"으로 오판 → 같은 교실·시간에 슬롯이 중복 생성됨.
5. 진단용 로그로 두 스레드 모두 `checkBeforeWrite` 진입 시점에 이미 `txActive=true`임을 확인해 위 가설을 검증했다(락 대기 55ms 관측 — 락 자체는 정상 동작).

즉 "락이 안 걸린 것"이 아니라 **락은 정상 작동했지만, 락이 지키려던 최신 데이터 조회 자체가 락보다 먼저 고정된 스냅샷을 보게 되어 락의 목적이 무의미해진 것**이다. 이 Aspect가 포인트컷 범위를 좁혔어도(quota 체크가 실제로 필요한 서비스만으로 한정) 그 범위 안에 비관적 락 기반 서비스가 하나라도 있었으면 동일하게 재현됐을 것이다 — 근본 원인은 범위가 아니라 순서다.

### 확정된 정책

- **`DatabaseQuotaAspect`에 `@Order(Ordered.HIGHEST_PRECEDENCE + 1)`을 명시**해 트랜잭션 어드바이저(기본값 `LOWEST_PRECEDENCE`)보다 반드시 먼저(더 바깥쪽에서) 실행되도록 고정한다. 이제 한도 체크는 대상 메서드의 트랜잭션이 열리기 전에 별도의 자동커밋 쿼리로 완전히 끝나고, 그 트랜잭션의 첫 쿼리는 다시 대상 메서드 자신의 쿼리(예: `findByIdForUpdate`)가 된다.
- **`HIGHEST_PRECEDENCE` 그 자체는 쓰지 않는다.** Spring의 `ExposeInvocationInterceptor`가 이미 `HIGHEST_PRECEDENCE`를 쓰고 있어서, 동일 값이면 이 어드바이스가 그보다 먼저 실행돼 `JoinPoint` 매칭에 필요한 `MethodInvocation` 컨텍스트가 없는 채로 실행되어 `IllegalStateException: No MethodInvocation found`가 즉시 재현된다(실제로 겪음). 한 칸 양보한 `HIGHEST_PRECEDENCE + 1`을 쓴다.
- 포인트컷 범위(`..service..` 전역)는 이번엔 좁히지 않는다 — `@Order` 수정만으로 근본 원인이 해결되며, 범위 축소는 오버헤드 감소 정도의 부차적 이득만 있고 정합성과는 무관하다(위 "근본 원인" 참고).
- CodeRabbit이 별도로 지적한 **`DatabaseQuotaAspect`의 경계값 버그**(`current > limit` → RDS 사용량이 한도와 정확히 같을 때 쓰기를 통과시킴)도 같은 파일을 고치는 김에 반영: `>=`로 변경.

### 완료 기준

- [x] `@Order(Ordered.HIGHEST_PRECEDENCE + 1)` 추가
- [x] `current > limit` → `current >= limit` 수정 + 경계값 테스트(`blocksWriteTransactionWhenUsageExactlyEqualsLimit`) 추가
- [x] 로컬에서 `DatabaseQuotaAspect`를 임시로 비활성화 → `CreateTimetableSlotConcurrencyTest` 통과 확인(원인 격리)
- [x] `@Order` 수정 후 같은 테스트 재통과 확인(수정 검증)
- [x] 임시 진단 로그(`checkBeforeWrite`/`createSlot`의 스레드·트랜잭션 상태 출력)로 근본 원인 실증 확인 후 원복
- [x] 전체 테스트 스위트 통과 확인

### 🧩 영향 범위

| 계층 | 변경 내용 |
| --- | --- |
| Infrastructure(planquota) | `DatabaseQuotaAspect`에 `@Order` 추가, 경계값 비교 연산자 수정 |
| 영향받는 다른 도메인 | `timetable`(교실 중복 예약 방지) — 코드 변경 없음, `DatabaseQuotaAspect` 실행 순서 수정만으로 간접 해결 |
| 테스트 | `DatabaseQuotaAspectTest` 경계값 케이스 추가, `CreateTimetableSlotConcurrencyTest`(기존 테스트) 재현·재검증에 사용 |
| Migration | 없음 |

---

## ✅ 2026-08-14 · CodeRabbit 리뷰 반영 — 한도 체크와 사용량 기록 사이 경쟁 조건 보강

### 배경

PR(플랜별 리소스 한도 집행)에 대한 CodeRabbit 리뷰 22건 중, "한도를 확인(check)하는 시점과 실제로 자원을 소비/기록(record)하는 시점이 분리되어 있어 동시 요청·배치 처리 시 한도를 초과할 수 있다"는 TOCTOU(Time-of-check to time-of-use) 패턴 지적이 여러 도메인에서 반복됐다. 이 중 AI 토큰(Gemini 호출)·S3 동시 등록 레이스는 발생 빈도와 초과 폭이 작아 후순위로 미뤘지만, 메일과 SMS는 **정상적인 운영 패턴 자체가 이 버그를 트리거**하는 조건이라 우선 반영했다.

### 확정된 정책

- **S3 고아 객체 정리 (`RegisterFileService`)**: 한도 초과 검사가 S3 업로드가 이미 끝난 뒤(`headObjectSize`)에만 이뤄져서, 거부되어도 이미 올라간 객체가 영구히 S3에 남아 실제 비용은 발생하지만 내부 집계엔 안 잡히는 문제가 있었다(동시성과 무관하게 매번 재현). 한도 초과 시 `fileStoragePort.delete(command.objectKey())`로 즉시 정리하도록 수정.
- **SMS 배치 중 한도 소진 (`SendAttendanceMessagesService`)**: 배치 시작 시점에 1회만 한도를 확인하고 이후 요청받은 학생 전원에게 순차 발송해서, 한 번의 정상 호출만으로도 배치 크기(학생 수)만큼 한도를 초과할 수 있었다(동시성 문제가 아니라 배치 로직 자체의 구조적 결함). `AtomicLong`으로 잔여 한도를 추적하며 발송 직전마다 재확인하도록 수정 — 소진되면 그 이후 학생은 실제 SMS 발송 없이 "한도 도달" 사유로 건너뛴다. 스트림이 순차 처리(비병렬)라 원자적 카운터 없이도 정확하지만, 향후 병렬화에 대비해 `AtomicLong`을 사용했다.
- **메일 발송 동시성 (`PayrollStatementEmailProcessor`)**: 급여명세서가 직원 수만큼 개별 `@Async` 이벤트로 큐잉되어 월말 일괄 발송 시 다수의 스레드가 동시에 "한도 미만"을 읽고 통과할 수 있었다. 이 프로젝트는 **학원(테넌트)마다 별도 프로세스 하나로 배포**되는 구조(`planquota/docs/README.md` 참고 — `app.instance.plan` 환경변수로 인스턴스당 플랜을 고정)라 이 JVM 안의 스레드 경합만 막으면 충분하다고 판단, `ReentrantLock`으로 한도 확인-준비-발송-기록 전 구간을 직렬화했다. DB 락이나 분산 락 없이 애플리케이션 레벨 락만으로 충분한 이유는 이 배포 전제 때문이다 — 향후 학원당 다중 인스턴스로 스케일아웃하게 되면 이 가정이 깨지므로 재검토 필요.
- **CHANGELOG 날짜 오기 수정**: `approval`/`rollcall`/`planquota` 세 도메인의 CHANGELOG에 다음날 날짜(2026-08-15)가 잘못 적혀 있던 것을 실제 작업일(2026-08-14)로 수정.
- **범위 밖(의도적으로 보류)**: AI 토큰(Gemini) 3개 어댑터와 S3 동시 등록의 TOCTOU 레이스는 실제 발생 빈도가 낮고(같은 학원 안에서 정확히 같은 순간에 트리거해야 함) 초과 폭도 한도 대비 1% 미만으로 추정되어, 공용 원자적 예약 서비스를 새로 설계하는 "heavy lift" 작업 대비 실익이 낮다고 판단해 후순위로 미룸. S3 정합성 스케줄러(`S3QuotaReconciliationScheduler`)의 다중 인스턴스 중복 보정, 감소분 미반영 문제는 아직 미검토.

### 완료 기준

- [x] `RegisterFileService`: 한도 초과 시 S3 객체 삭제 + 테스트(`deletesOrphanedS3ObjectWhenUploadWouldExceedLimit`) 추가
- [x] `SendAttendanceMessagesService`: 배치 중 잔여 한도 재확인 로직 + 테스트(`stopsSendingOnceMonthlyLimitIsExhaustedMidBatch`) 추가
- [x] `PayrollStatementEmailProcessor`: `ReentrantLock`으로 직렬화 + 동시성 테스트(`동시에_처리되는_두_발송_중_한도를_넘기는_한_건은_건너뛴다`, 실제 스레드 2개로 검증) 추가
- [x] `approval`/`rollcall`/`planquota` CHANGELOG 날짜 수정
- [x] 전체 테스트 스위트 통과 확인

### 🧩 영향 범위

| 계층 | 변경 내용 |
| --- | --- |
| Application(file) | `RegisterFileService` — 한도 초과 시 S3 객체 삭제 |
| Application(rollcall) | `SendAttendanceMessagesService` — 배치 중 잔여 한도 추적 |
| Application(payroll) | `PayrollStatementEmailProcessor` — `ReentrantLock` 필드 추가, `process()` 구조 변경(락 구간 확대) |
| 문서 | `approval`/`rollcall`/`planquota` CHANGELOG.md 날짜 수정 |
| Migration | 없음 |
