# 매출 리포트(revenuereport) 리비전 로그

> 작성일: 2026-08-12
> 상태: 🚧 Spring 쪽(집계·저장·조회·배치) 완료. mudo-ai-server 쪽(AI 서술 생성 엔드포인트) 완료. 리포트 생성 시 알림함 연동 완료. 슈퍼어드민 수동 생성 API 완료.

## 🎯 변경 목적

원장이 매달 학원의 매출/지출/순이익 현황을 AI가 서술한 리포트로 받아볼 수 있게 한다. 짐짝(Gym-Jjak)의 "트레이너 시장동향 리포트"(월간 배치로 AI가 생성, 인앱에서 조회) 구조를 참고했다.

---

## ✅ 2026-08-17 · 슈퍼어드민 수동 매출 리포트 생성 API

### 배경

배치(매달 1일 00:30)로만 리포트가 생성돼, 배치가 실패했거나 아직 한 번도 안 돈 상태(신규 배포 직후 등)에서는 수동으로 대응할 방법이 없었다. 발표/가이드 문서용 스크린샷 데이터가 당장 필요하기도 했다.

### 확정된 정책

- 기존 배치 전용 `GenerateRevenueReportUseCase`/`GenerateRevenueReportService`는 손대지 않았다. 신규 `ManualGenerateRevenueReportService`가 `revenueReportRepository.findByTargetMonth`로 존재 여부만 먼저 확인해서, 있으면 `RevenueReportAlreadyExistsException`(409)을 던지고 없으면 기존 `GenerateRevenueReportUseCase.generate()`에 그대로 위임한다 — 배치는 계속 조용히 스킵, 수동 API는 409로 명확히 알림.
- `targetMonth`는 요청에서 명시적으로 받는다(배치처럼 항상 직전월 고정이 아님). 스케줄러 장애 대응뿐 아니라, 여러 달을 순차 생성해 전월 대비 비교치를 만들어내는 용도도 겸한다(`GenerateRevenueReportService.fetchPreviousSnapshot`이 직전월 리포트가 있어야 비교치를 채우므로).
- 당월/미래월 검증 없음 — 슈퍼어드민 유연성 우선. 미완성 데이터로 리포트가 만들어질 수 있는 트레이드오프는 감수한다.
- 이 도메인은 이미 완전 단일 테넌트(`academy_id` 없음, `academy` 테이블 자체가 없음)라, "여러 학원 중 하나를 선택"하는 개념이 API에 없다.
- 성공 응답은 `204 No Content`(바디 없음)로 통일했다 — 이 코드베이스는 body 없는 성공 응답에 `GlobalApiResponse`를 쓰지 않고 `ResponseEntity.noContent().build()`만 반환하는 기존 관례(`UserController.changeUserStatus` 등)를 그대로 따른다. 생성된 내용 확인은 기존 상세조회 API로 분리했다.

### 검토했다가 제외한 대안

- 기존 리포트 강제 삭제 후 재생성(`force` 플래그) — 이번 스코프 밖(YAGNI). 필요해지면 별도 스펙으로.

### 로컬 e2e 검증

- 인증/인가: 미인증 401, `ACADEMY:OWNER`(비-슈퍼어드민) 403, `PLATFORM:SUPER_ADMIN` 통과 — 전부 실제 curl로 확인.
- 입력 검증: `targetMonth="july"` → 400(`COMMON_400_1`) 실제 확인.
- 로직 도달 확인: 실제로 기존 배치 로직(집계 → AI 호출)까지 정상 도달함을 확인. 로컬에 `mudo-ai-server`(FastAPI, 실제 Gemini 연동 완료된 별도 저장소)를 직접 띄우고 `REVENUE_REPORT_AI_BASE_URL=http://localhost:8000`으로 연결.
- **AI 호출 자체는 이번 검증 시점에 Gemini 프리페이먼트 크레딧이 소진된 상태(`429 RESOURCE_EXHAUSTED`)라 실제 리포트 생성(204)까지는 로컬에서 끝내지 못했다.** 대신 그 상황에서 `502 REVENUE_REPORT_502_1`이 fallback 없이 정확히 전파되는 것까지 확인했다 — 이는 코드/설계가 의도대로 동작함을 보여주는 유효한 검증 결과다. 409(중복 생성)는 실제 리포트가 하나도 저장되지 못해 재현하지 못했고, 컨트롤러/서비스 단위 테스트로 검증을 대체했다.
- 2026-06 → 2026-07 순차 생성으로 전월 대비 비교치를 실제로 확보하는 것(원래 목적)은 Gemini 크레딧 재충전 후 별도로 진행 필요.

### 완료 기준

- [x] `ManualGenerateRevenueReportUseCase`/`Service`(TDD)
- [x] `RevenueReportAlreadyExistsException`(`409 REVENUE_REPORT_409_1`)
- [x] `GenerateRevenueReportRequest` DTO(TDD)
- [x] `RevenueReportController.generate()` 핸들러(`PLATFORM:SUPER_ADMIN`, TDD)
- [x] 로컬 e2e: 인증/인가/입력검증/AI-실패-전파 확인
- [ ] 로컬 e2e: 실제 리포트 생성 성공(204) + 전월 대비 비교치 확보 — Gemini 크레딧 문제로 보류
- [x] 문서 갱신(API.md/README.md/CHANGELOG.md/REVISION.md)

---

## ✅ 2026-08-13 · 리포트 생성 시 알림함 연동

### 배경

리포트 기능을 처음 만들 당시(2026-08-12) MUDO엔 영속 알림함이 없어서, 별도 알림 인프라 대신 리포트 테이블 자체의 `read_at`으로만 안읽음을 표현하기로 했었다(`docs/superpowers/specs/2026-08-12-ai-revenue-report-design.md` 참고). 이후 다른 팀원이 `notification` 도메인(이벤트 구독 기반 영속 알림함)을 만들어 develop에 병합했다.

### 확정된 정책

- `revenuereport/domain/event/RevenueReportGeneratedEvent(recipientUserId, reportId, targetMonth)` 신설. `GenerateRevenueReportService`가 저장 성공 직후 발행한다(approval 도메인의 `ApprovalLineActivatedEvent` 발행 패턴과 동일하게 `ApplicationEventPublisher.publishEvent()`를 서비스에서 직접 호출).
- 수신자는 이벤트 발행 시점에 `revenuereport` 도메인이 직접 조회해 이벤트에 담는다(다른 알림 이벤트들과 동일한 컨벤션 — notification 리스너가 아니라 발행하는 도메인이 수신자를 안다). 신규 `AcademyOwnerLookupPort`(원장 userId 조회)를 `users` 도메인이 `AcademyOwnerLookupAdapter`로 구현한다.
- 원장 계정이 아직 없으면(부트스트랩 전 등) `Optional.empty()`로 조용히 건너뛴다 — 알림 발행 실패/스킵이 리포트 생성 자체의 성공 여부에 영향을 주지 않는다.
- `notification` 도메인의 `NotificationCreationListener`에 `handle(RevenueReportGeneratedEvent)` 케이스 추가, `NotificationType.REVENUE_REPORT_GENERATED` 신규.
- 기존 `read_at`/안읽음-카운트 API는 그대로 유지한다 — 목록 화면 안읽음 표시용으로는 여전히 유효하고, 이번 알림함 연동은 벨 아이콘 등 별도 알림 UI에 뜨게 하는 걸 추가하는 것뿐이다.

### 로컬 검증

- `GenerateRevenueReportServiceTest`: 원장 존재 시 이벤트 발행(수신자/reportId/targetMonth 검증), 원장 부재 시 이벤트 미발행 검증.
- `NotificationCreationListenerTest`: 이벤트 수신 시 올바른 `CreateNotificationCommand`(수신자/타입/targetId/문구) 생성 검증.
- `UserRepositoryImplTest`: `findAcademyOwnerId()` 존재/부재 케이스 검증.
- 로컬 `bootRun`으로 전체 빈 그래프(신규 포트/어댑터/이벤트 리스너 포함) 정상 기동 확인.

---

## ✅ 2026-08-12 · 매출 리포트 신규 도메인 구축

### 배경

설계 과정에서 두 가지 제약을 확인했다:

1. **MUDO엔 실제 매출/결제 데이터가 없었다.** `student/docs/*`에 결제·POS연동·환불이 영구 제외라고 명시돼 있고, 코드 전체에 결제 성격 테이블이 없었다(`lecture.feeAmount`는 가격 카탈로그일 뿐). 신규 `payment`(수납) 테이블을 만들어 더미데이터로 채우는 것으로 해결했다 — 실제 수납 기능은 만들지 않고, "이 기능으로 확장할 준비가 돼 있다"는 것만 보여준다.
2. **MUDO엔 영속 알림함이 없다.** `workspace`의 WebSocket 알림은 실시간 접속 중일 때만 동작(DB 미저장), `approval`의 Web Push는 이미 폐기됐다. 별도 알림 인프라 대신 리포트 자체에 `read_at`을 저장하는 방식으로 해결했다.

### 확정된 정책

- 계산은 전부 Spring(`RevenueSnapshotCalculator`)이 확정해서 숫자로 넘기고, AI는 서술만 한다.
- 5개 지표: 예상 매출액, 실 매출액, 실 지출, 실 순이익, 예상 순이익. `byLecture`/`byTeacher` breakdown 포함.
- **전월 대비 top-line 3개 지표 비교**(실 매출/실 지출/실 순이익)를 추가했다. 처음엔 `Enrollment.reactivate()`가 과거 등록 이력을 덮어써서 과거 데이터로 추이를 재구성할 수 없다는 이유로 스코프에서 뺐었는데, "이번 달에 계산한 값 vs 저번 달에 이미 저장해둔 값"을 비교하는 데는 과거 상태 재구성이 필요 없다는 걸 확인하고 다시 넣었다. 전월 리포트의 `data_snapshot`을 그대로 되읽어서 비교하며(재집계 안 함, 원장에게 실제로 보여줬던 숫자와 일치시키기 위함), 강의별/강사별 breakdown까지 전월 대비하는 건 강의 구성이 매달 바뀔 수 있어(신규 개설/폐강) 스코프 밖으로 남겼다.
- `payment`는 마이그레이션으로 스키마만 만들고, 더미데이터는 로컬 DB에 직접 INSERT한다.
- `revenue_report.target_month`에 UNIQUE 제약을 걸어 멱등성을 DB 레벨에서도 방어한다.
- 배치 크론은 `app.revenue-report.batch-cron` 프로퍼티로 분리했다(이 코드베이스에서 처음 도입된 패턴 — 기존 `WorkspaceTaskDelayScheduler`는 하드코딩 크론이었음).
- 권한은 새 코드를 만들지 않고 기존 합성 권한 `ACADEMY:OWNER`를 재사용한다.

### 신규 도입

- `revenuereport` 도메인 전체: `Payment`/`RevenueReport` 도메인 모델, 두 리포지토리, `RevenueSnapshotCalculator`(순수 계산), `FastApiRevenueReportClient`(dataimport의 `FastApiImportAnalysisClient` 패턴 재사용), `GenerateRevenueReportService`(오케스트레이션+멱등성), `RevenueReportBatchScheduler`, 목록/상세/안읽음카운트 서비스 3개, `RevenueReportController`.
- 크로스 도메인 Port 4개: `LectureRevenuePort`(lecture 구현), `ActiveEnrollmentCountPort`/`EnrollmentLectureLookupPort`(student 구현), `ExpenseSummaryPort`(corporatecard 구현).
- `LectureRepository.findAll()` 신규 추가(기존엔 페이지네이션 버전만 있었음).
- `CardExpenseJpaRepository.sumAmountByCategoryAndApprovedAtBetween` 신규 집계 쿼리 — 이 코드베이스에 `SUM`/`GROUP BY` 패턴이 지금까지 전무해서 처음 작성했다.

### 계획 대비 구현 중 발견/수정한 것

- **`@DataJpaTest`에서 `hibernate.dialect`를 MySQL로 강제 지정하면 안 된다.** 계획서 초안에 `@TestPropertySource(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect")`가 있었는데, `@DataJpaTest`는 임베디드 H2를 쓰므로 실제 커넥션(H2)과 강제 지정한 dialect(MySQL)가 어긋나 "Table not found" 에러가 났다. 기존 코드베이스 관례대로 dialect 지정 없이 Spring Boot의 H2 자동 감지에 맡기도록 고쳤다.
- **`@CreatedDate`/`@LastModifiedDate`(`BaseTimeEntity`)가 채워지려면 `@DataJpaTest`에 `TimeConfig`(`@EnableJpaAuditing`)를 같이 `@Import`해야 한다.** 안 하면 `created_at NOT NULL` 위반으로 insert가 실패한다. 기존 `LectureRepositoryImplDataJpaTest`가 이미 이 패턴을 쓰고 있었는데 처음엔 놓쳤다가, 실제 테스트 실패로 발견하고 고쳤다.
- **`@Scheduled` cron 프로퍼티에 인라인 기본값이 없으면 전체 컨텍스트 로드 테스트가 깨진다.** `src/test/resources/application.yaml`엔 `app:` 블록 자체가 없어서, `${app.revenue-report.batch-cron}`(기본값 없음)를 해석하지 못해 `MudoGroupwareApplicationTests`를 비롯해 전체 스프링 컨텍스트를 로드하는 통합 테스트 22개가 한꺼번에 깨졌다. `@Scheduled(cron = "${app.revenue-report.batch-cron:0 30 0 1 * *}", ...)`처럼 운영 기본값과 동일한 인라인 기본값을 추가해서 해결했다.
- `LectureRepository`에 페이지네이션 없는 `findAll()`이 없어서 새로 추가했다(계획서에 이미 예견된 보강 사항).
- `EnrollmentJpaRepository`에 새 파생 쿼리를 추가하려 했으나, `JpaRepository`가 기본 제공하는 `findAllById(Iterable<ID>)`로 충분해서 새 메서드 없이 재사용했다.
- 지출 집계 쿼리는 계획서가 `CorporateCardTransactionJpaRepository`에 추가하도록 가정했지만, 쿼리의 FROM 엔티티가 `CardExpenseJpaEntity`라 기존 `CardExpenseJpaRepository.findForUpdate`와 같은 조인 패턴을 따라 그쪽에 추가했다.

### 로컬 e2e 검증

- Flyway 마이그레이션 정상 적용(`payment`/`revenue_report` 테이블 생성 확인).
- 더미 `payment` 2건(결제 30만원 + 환불 5만원, 2026-07) INSERT 후 배치를 수동으로(크론을 임박 시각으로 바꿔서) 트리거:
  - `event=revenue_report_batch_실패 targetMonth=2026-07-01`, `REVENUE_REPORT_AI_BASE_URL is not configured` — 설계한 그대로(AI 서버 미구축 상태에서 fallback 없이 명확히 실패)
  - `revenue_report` 테이블에 저장된 행 0개 — 실패 시 부분 저장 없음 확인
- `ACADEMY:OWNER` 계정으로 로그인해 목록(빈 배열)/안읽음카운트(0)/상세-404/무인증-401 4개 응답 전부 확인.
- 전체 테스트 스위트(1370여 개) 통과, `./gradlew build` 성공.

### 완료 기준

- [x] `payment` 테이블 로컬 더미데이터 준비
- [x] 리포트 저장 테이블 마이그레이션
- [x] 매출/지출/순이익 집계 로직 + 전월 대비 top-line 비교
- [x] FastAPI 호출 어댑터(Port/Adapter) — mudo-ai-server 실제 엔드포인트는 별도 작업 대기
- [x] `@Scheduled` 배치 + 프로퍼티화된 크론
- [x] 목록/상세/안읽음-카운트 API + `ACADEMY:OWNER` 권한
- [x] 로컬 e2e 검증(배치 수동 트리거 → 예상된 실패 확인 → 목록/상세/안읽음 카운트 확인)
- [x] Spring 쪽 문서 갱신
- [ ] mudo-ai-server 쪽 구현(Plan 2/2, 별도 저장소) 및 통합 재검증
