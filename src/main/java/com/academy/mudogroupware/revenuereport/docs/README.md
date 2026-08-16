# revenuereport 모듈

학원의 월간 매출/지출/순이익을 집계해서 AI(mudo-ai-server)로 서술 텍스트를 받아 저장하고, 원장이 조회할 수 있게 한다.

## 책임과 범위

- `Payment`: 수납 거래 기록. **더미데이터 전용**이며 생성/수정 API는 없다(로컬 DB에 직접 INSERT). row 하나 = 독립된 거래 이벤트(`PAID`/`PARTIAL`/`REFUNDED` 각각 별개 row, 환불이 어느 결제를 참조하는지는 추적하지 않음).
- `RevenueReport`: AI가 생성한 서술 텍스트 + 집계 당시 숫자 스냅샷(`data_snapshot`)을 저장한다. `read_at`이 리포트 자체에 있어 별도 알림 인프라 없이 안읽음 여부를 표현한다.

## 공개 UseCase

- `GenerateRevenueReportUseCase` — 월간 배치가 호출. 이미 해당 달 리포트가 있으면 스킵(멱등성).
- `ManualGenerateRevenueReportUseCase` — `PLATFORM:SUPER_ADMIN`이 API로 임의의 월을 지정해 즉시 호출. 존재 여부를 먼저 확인해 있으면 예외(409)로 명확히 알리고, 없으면 `GenerateRevenueReportUseCase`에 그대로 위임(생성 로직 자체는 배치와 완전히 동일).
- `ListRevenueReportsUseCase`
- `GetRevenueReportUseCase` — 조회 시 읽음 처리 부수효과.
- `CountUnreadRevenueReportsUseCase`

## 이 모듈이 정의한 크로스 도메인 Port (다른 모듈이 구현)

- `LectureRevenuePort` — `lecture.infrastructure.persistence.LectureRevenuePortAdapter`가 구현. 강의별 가격·강사명.
- `ActiveEnrollmentCountPort` — `student.infrastructure.persistence.ActiveEnrollmentCountPortAdapter`가 구현. 강의별 활성 등록 수.
- `EnrollmentLectureLookupPort` — `student.infrastructure.persistence.EnrollmentLectureLookupPortAdapter`가 구현. `payment.enrollmentId → lectureId` 매핑(강의별 실 매출 집계용).
- `ExpenseSummaryPort` — `corporatecard.infrastructure.persistence.ExpenseSummaryPortAdapter`가 구현. 기간별 법인카드 지출 합계+카테고리별 breakdown.
- `RevenueReportAiPort` — 이 모듈 자체 `infrastructure/external/fastapi/FastApiRevenueReportClient`가 구현. mudo-ai-server 호출.

## 계산 vs 서술 경계

숫자 계산은 전부 `RevenueSnapshotCalculator`(순수 함수, DB/외부 호출 없음)가 담당한다. AI(`RevenueReportAiPort`)는 이미 계산된 `RevenueSnapshot`을 받아 서술 텍스트만 반환한다 — AI가 직접 집계·계산하지 않는다.

## 전월 대비 비교

`RevenueSnapshot.previousMonth`에 top-line 3개 지표(실 매출/실 지출/실 순이익)의 전월 대비 증감이 담긴다. 과거 데이터를 다시 집계하지 않고, 전월 리포트 생성 당시 저장해둔 `data_snapshot`을 그대로 되읽어 비교한다(원장에게 실제로 보여줬던 숫자와 일치시키기 위함). 첫 리포트는 `available: false`.

## 주의사항

- `payment`는 더미데이터 전용 테이블이다 — 실제 수납 기능(생성/수정 API)은 없다. 로컬 e2e/시연 목적으로만 직접 INSERT한다.
- `@Scheduled` 배치의 크론은 `app.revenue-report.batch-cron` 프로퍼티(기본값 매달 1일 새벽 0시 30분)로 분리돼 있다 — 이 코드베이스에서 처음 도입된 크론 외부화 패턴이다. `@Scheduled` 어노테이션에 프로퍼티와 동일한 기본값을 인라인으로 둬서, 이 프로퍼티가 없는 테스트 컨텍스트(`src/test/resources/application.yaml`)에서도 컨텍스트 로드가 깨지지 않는다.
- AI 호출(`REVENUE_REPORT_AI_BASE_URL` 등)은 `mudo-ai-server`가 별도로 구현해야 동작한다(Plan 2/2, 별도 저장소). 그 전까지 배치는 매번 `revenue_report_batch_실패` 로그를 남기고 조용히 실패한다(fallback 없음 — 서술형 리포트는 AI 없이는 대체 불가).
- 이 도메인의 리포트는 `ACADEMY:OWNER` 합성 권한(새 권한 코드 아님, `accountType=ADMIN && adminScope=ACADEMY` 계정에 로그인 시 자동 부여)으로만 접근 가능하다.
- 읽음 상태가 계정별이 아니라 리포트 레코드 자체에 있다 — `ACADEMY:OWNER` 계정이 여러 개면 한 명이 읽으면 다른 계정에서도 읽음으로 보인다(학원당 원장 계정 1개 전제로 단순화).
- 수동 생성(`POST /api/revenue-reports/generate`)은 당월/미래월 검증을 의도적으로 하지 않는다 — `PLATFORM:SUPER_ADMIN`에게 유연성을 우선 부여했다. 이 도메인은 이미 완전 단일 테넌트(`academy_id` 없음)라 "여러 학원 중 하나를 선택"하는 개념 자체가 API에 없다.

## 문서

- [API.md](API.md)
- [REVISION.md](REVISION.md)
- [CHANGELOG.md](CHANGELOG.md)
