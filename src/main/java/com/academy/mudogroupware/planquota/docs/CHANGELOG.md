# planquota Changelog

## 2026-08-15 - 플랜별 리소스 한도 집행 기능 신설

- `Plan`/`PlanLimits`/`CurrentPlanProvider`/`PlanLimitErrorCode`/`PlanLimitExceededException` 추가.
- RDS 저장용량: 모든 쓰기 트랜잭션 진입 시 체크하는 `DatabaseQuotaAspect` 추가.
- S3 저장용량: `resourceusage`의 `S3_STORAGE` 이벤트 타입 재사용, 전용
  `TenantS3UsagePort`/`TenantS3UsageAdapter` + `S3QuotaReconciliationScheduler`(일 1회
  드리프트 보정) 추가.
- 직원수/학생수/SMS/AI토큰/메일 각 도메인에 생성·발송·호출 직전 한도 체크를
  연결했다(자세한 지점은 README.md 참고).
- 새 마이그레이션 없음(`resource_type` 컬럼이 `VARCHAR`라 enum 값만 추가).
