# planquota

학원(테넌트)의 무료/유료 플랜에 따라 직원수·학생수·S3·RDS·SMS·AI토큰·메일 7개 리소스에
실제 한도를 걸어 초과 시 요청을 차단하는 도메인이다.

이 도메인 자체는 리소스를 직접 사용하지 않는다. 각 도메인(users, student, file,
payroll, rollcall, approval, dataimport)이 이 도메인이 제공하는 공용 컴포넌트를
호출해 자기 자리에서 체크한다.

## 핵심 컴포넌트

- `Plan`/`PlanLimits`: 플랜(FREE/PAID)과 플랜별 한도값. `PlanLimits.of(Plan)`으로
  정적 매핑을 조회한다.
- `CurrentPlanProvider`: `app.instance.plan` 환경변수(`InstanceMetadataProperties`)를
  `Plan`으로 해석하는 유일한 지점. 알 수 없는 값은 `FREE`로 취급한다.
- `PlanLimitExceededException`(+`PlanLimitErrorCode`): 한도 초과 시 던지는 공용
  예외. HTTP 429(Too Many Requests), 응답 메시지에 플랜명+리소스명이 포함된다.
- `DatabaseQuotaAspect`: RDS 저장용량 — 모든 쓰기 트랜잭션(`@Transactional(readOnly
  = false)`) 진입 시 `CurrentTenantDatabaseUsagePort`로 현재 DB 용량을 확인한다.
  포인트컷(`..service..`)이 앱 전체 서비스 계층에 걸리므로 **`@Order(HIGHEST_PRECEDENCE
  + 1)`로 대상 메서드의 트랜잭션이 열리기 전에 반드시 먼저 실행되도록 고정**돼
  있다 — 순서를 풀면 한도 체크 쿼리가 대상 트랜잭션의 첫 쿼리가 되어 MySQL
  REPEATABLE READ 스냅샷을 조기 고정시키고, 그 트랜잭션 안에서 비관적 락을 쓰는
  다른 도메인(예: `timetable` 교실 중복 예약 방지)의 동시성 방어가 무력화된다.
  자세한 재현 과정은 REVISION.md 참고.
- `TenantS3UsagePort`/`TenantS3UsageAdapter`: 이 테넌트 자신의 S3 사용량만 항상
  조회 가능한 전용 어댑터(`platform.StorageUsagePort`와 달리
  `platform.dashboard.enabled` 플래그에 의존하지 않는다).
- `S3QuotaReconciliationScheduler`: 매일 1회 S3 실측 스캔과 `resourceusage`
  이벤트 합계를 대조해 드리프트를 보정하는 안전장치(판단 기준은 어디까지나
  실시간 합계).

## 리소스별 집행 지점

| 리소스 | 집행 위치 | 방식 |
| --- | --- | --- |
| 직원수 | `users.CreateAccountService` | 생성 직전 `UserRepository.countActiveUsers()` 체크 |
| 학생수 | `student.CreateStudentService` | 생성 직전 `StudentRepository.countAll()` 체크 |
| RDS 저장용량 | 전역 AOP(`DatabaseQuotaAspect`) | 모든 쓰기 트랜잭션 진입 시 체크 |
| S3 저장용량 | `file.RegisterFileService` | 등록 직전 `resourceusage`(S3_STORAGE) 합계 체크 |
| SMS | `rollcall.SendAttendanceMessagesService` | 배치 시작 시점 이번 달 합계 체크 |
| AI 토큰 | `approval`(2곳)/`dataimport`(1곳) Gemini 어댑터 | 호출 직전 이번 달 합계 체크 |
| 메일 | `payroll.PayrollStatementEmailProcessor` | 발송 직전 이번 달 합계 체크(예외 대신 skip 처리) |

## 범위 밖(Out of Scope)

- SMS 발송(`rollcall`)/AI 토큰 호출(`approval`, `dataimport`) 실패 시의 재시도 로직은
  각 도메인 고유의 정책을 따른다. 이 도메인은 한도 체크만 담당한다.
- 파일 삭제 시 S3 사용량 감소 로직: 삭제 기능이 아직 없어 이번 범위에 포함하지
  않았다. 삭제 기능이 생기면 감소 이벤트를 추가해야 한다.
- 메일 발송의 범도메인 공용화: 지금은 급여명세서 발송(`payroll`)이 유일한 메일
  케이스라 별도 공용 포트를 만들지 않았다.
- Plan 값을 환경변수에서 DB로 이관하는 작업: 플랜 변경 시 재배포가 필요한 현재
  구조를 그대로 유지하기로 했다.
