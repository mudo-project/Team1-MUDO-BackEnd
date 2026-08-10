# calendar 모듈

## 책임과 범위

학원 공용 캘린더(일정) 기능을 담당한다. 학원 구성원 전체가 조회할 수 있는 일정을 만들고, 조회(월별 목록/일별/상세)·수정·삭제한다. 개인 메모(`memo` 모듈)와 달리 학원 전체(academy) 단위로 공유되는 데이터다.

## 담당자

(팀 확인 필요)

## 소유하는 주요 데이터와 상태

- `CalendarEvent` — DB 테이블 `calendar_events` (academy_id, title(필수, 200자), content(nullable), event_start_at, event_end_at(nullable — 종료 시각 없으면 순간 일정), is_all_day, color(nullable), created_by, created_at, updated_at)
- `created_at`/`updated_at`은 `memo`/`approval`과 달리 `global.infrastructure.persistence.BaseTimeEntity`(JPA Auditing)를 상속해 자동 관리한다 — 재생성 후 merge하는 특이 케이스가 없는 단순 CRUD라 Auditing을 그대로 써도 문제없다고 판단했다(팀 확인 필요, 다른 판단이면 변경).

## 외부에 공개하는 Application API

- `CreateCalendarEventUseCase` — 일정 작성
- `CalendarEventQueryUseCase` — 일정 목록/상세 조회 (기간 범위 쿼리로 월별 목록과 일별 조회를 겸함)
- `UpdateCalendarEventUseCase` — 일정 수정
- `DeleteCalendarEventUseCase` — 일정 삭제

## 다른 모듈 또는 외부 시스템에 요청하는 의존성

- 현재 없음. 작성자 이름 등 타 도메인 조인이 필요한 응답이 없어 조회 Port가 필요하지 않다.

## 발행·소비하는 Event

- 현재 없음.

## 변경 시 주의 사항

- 작성/수정/삭제는 `CALENDAR:MANAGE` 권한(원장 + 원장이 위임한 구성원)을 보유해야 가능하다. `CalendarController`의 `createEvent`/`updateEvent`/`deleteEvent`에 `@PreAuthorize("hasAuthority('CALENDAR:MANAGE')")`가 적용되어 있으며, 권한 시드는 `V5.1.2__add_calendar_manage_permission.sql`에서 관리한다. 자세한 내용은 [CALENDAR_PERMISSIONS.md](CALENDAR_PERMISSIONS.md) 참고.
- 도메인 규칙 위반은 `calendar.domain.exception.CalendarErrorCode` + 에러별 이름이 드러나는 예외 클래스(`CalendarEventNotFoundException` 등, 공통 예외의 `protected ErrorCode` 생성자 사용, `approval.domain.exception` 패키지 위치와 동일하게 `domain.model`이 아니라 `domain.exception`에 둔다)로 던진다. `memo`/`approval`이 쓰는 단일 `<Domain>Exception` 방식이 아니라 `docs/ERROR_HANDLING.md`에 문서화된 신규 패턴을 처음부터 따른다(신규 도메인이라 구식 패턴을 새로 만들 이유가 없음).
- 마이그레이션 담당자번호는 `be5`.
- 영속성 구현체 이름은 `CalendarEventRepositoryImpl`이 아니라 **`CalendarEventPersistenceAdapter`**를 쓴다. 대다수 도메인(approval/notice/attendance/memo/messenger/users)은 자기 자신의 데이터를 저장하는 구현체에도 `RepositoryImpl` 접미사를 쓰지만, `workspace`는 `ARCHITECTURE.md`의 헥사고날 Port/Adapter 용어를 충실히 따라 `WorkspacePersistenceAdapter`라는 이름을 쓴다. calendar는 workspace 선례를 따라 `Adapter` 명명을 채택한다(다만 workspace처럼 별도 MapStruct 매퍼로 변환 로직을 빼지는 않고, 다수 도메인과 동일하게 `toEntity`/`toDomain`을 어댑터 클래스 안에 손으로 둔다).
- **개발 진행 방식**: 이 도메인은 파일 하나를 완성할 때마다(코드 전체를 먼저 텍스트로 제시) 담당자 승인을 받은 뒤에 실제로 파일에 쓰고, 승인받은 다음 파일로 넘어가는 방식으로 진행한다. 테스트 코드는 구현 파일이 전부 승인·작성된 뒤 마지막에 한 번에 작성한다(다른 도메인의 TDD 선행 방식과 다름, calendar 한정 규칙). 문서(`README.md`/`API.md`) 갱신은 이 승인 절차 대상이 아니며, 각 규칙에 따라 알아서 갱신한다.
- **API 단위 진행**: 5개 엔드포인트를 한 번에 다 만들지 않고, API 하나씩(예: 일정 생성 → 일정 조회 → ...) 구현 → 이슈(`feature_request.yml` 형식) + PR(`pull_request_template.md` 형식) 문서 작성 → develop 머지 → 다음 API용 브랜치로 진행한다. 단, `git push`/PR 생성/머지는 AGENTS.md 절대 규칙("GitHub 직접 push 금지")에 따라 작업자가 직접 수행하고, 텍스트로 준비만 한다. 도메인 모델·예외·엔티티·영속성처럼 모든 API가 공유하는 기반 코드는 첫 API(일정 생성) 이슈/PR에 포함한다.

## 세부 문서

- [BUSINESS_RULES.md](BUSINESS_RULES.md) — 도메인 정책과 접근 권한, 검증 규칙
- [CALENDAR_API.md](CALENDAR_API.md) — 엔드포인트별 요청·응답·에러 코드
- [CALENDAR_API_FLOW.md](CALENDAR_API_FLOW.md) — API별 호출 흐름 다이어그램
- [CHANGELOG.md](CHANGELOG.md) — 사용자 관점의 기능 변경 이력
- [REVISION.md](REVISION.md) — 개발자 관점의 정책·구현 결정 기록
