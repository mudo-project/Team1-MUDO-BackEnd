# planquota Changelog

## 2026-08-14 - 플랜별 리소스 한도 집행 기능 신설

- `Plan`/`PlanLimits`/`CurrentPlanProvider`/`PlanLimitErrorCode`/`PlanLimitExceededException` 추가.
- RDS 저장용량: 모든 쓰기 트랜잭션 진입 시 체크하는 `DatabaseQuotaAspect` 추가.
- S3 저장용량: `resourceusage`의 `S3_STORAGE` 이벤트 타입 재사용, 전용
  `TenantS3UsagePort`/`TenantS3UsageAdapter` + `S3QuotaReconciliationScheduler`(일 1회
  드리프트 보정) 추가.
- 직원수/학생수/SMS/AI토큰/메일 각 도메인에 생성·발송·호출 직전 한도 체크를
  연결했다(자세한 지점은 README.md 참고).
- 새 마이그레이션 없음(`resource_type` 컬럼이 `VARCHAR`라 enum 값만 추가).

## 2026-08-14 - 코드 리뷰 반영: 락 무력화 버그 수정 + 경계값/누락 방어 보강

- **`DatabaseQuotaAspect`가 다른 도메인의 비관적 락을 무력화하던 버그 수정.** 자세한 배경은 REVISION.md 참고.
- `DatabaseQuotaAspect`: RDS 사용량이 한도와 **정확히 같을 때** 쓰기를 통과시키던 경계값 버그 수정(`>` → `>=`).
- `RegisterFileService`: S3 업로드 후 한도 초과로 등록이 거부되면 이미 올라간 객체가 고아로 영구히 남던 문제 수정 — 거부 시 즉시 삭제.
- `SendAttendanceMessagesService`: 배치 시작 시점 1회 체크만으로는 배치 크기(학생 수)만큼 한도를 초과해 발송할 수 있던 문제 수정 — 발송 직전마다 잔여 한도를 다시 확인.
- `PayrollStatementEmailProcessor`: 급여명세서 일괄 발송(직원 수만큼 동시 비동기 처리)이 메일 한도 체크와 기록 사이의 경쟁 조건으로 한도를 초과할 수 있던 문제 수정 — 학원당 단일 프로세스 배포 구조를 전제로 애플리케이션 레벨 락으로 검사-발송-기록 구간을 직렬화.
