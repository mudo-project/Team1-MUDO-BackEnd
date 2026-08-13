# 캘린더/시간표 동시성·정합성 보완 설계

기준일: 2026-08-14

## 목표

`calendar`/`timetable` 도메인 코드 리뷰에서 찾은 세 가지 동시성/정합성 문제를 고친다.

1. 같은 교실·시간대에 수업 슬롯이 동시에 두 번 등록될 수 있는 레이스 컨디션
2. `CalendarEvent`/`TimetableSlot`/`TimetableSet`에 낙관적 락이 없어 동시 수정 시 한쪽이 조용히 덮어써지는 문제
3. 시간표 세트 목록 조회 시 강의실 구성이 N+1 쿼리로 조회되는 문제

## 배경

- **레이스 컨디션**: `CreateTimetableSlotService`/`UpdateTimetableSlotService`가 SELECT로 같은 강의실의 기존 슬롯을 조회해 겹침을 검사한 뒤 INSERT/UPDATE한다. 이 SELECT는 락을 잡지 않으므로, 두 요청이 동시에 들어오면 둘 다 "안 겹침"으로 판단하고 커밋해 이중 배정이 생길 수 있다.
- **낙관적 락 부재**: `users` 도메인은 이미 `V4.1.9`에서 `version` 컬럼과 `@Version`을 도입해 동시 수정 시 `OptimisticLockingFailureException` → 도메인 Conflict 예외로 변환하는 패턴을 쓰고 있다(`UserRepositoryImpl.updateProfile`). `calendar`/`timetable`은 아직 이 패턴이 없다.
- **N+1**: `TimetableSetEntity.classrooms`가 `@ElementCollection(fetch = FetchType.EAGER)`이고 배치 로딩 설정이 없어서, `GetTimetableSetsService`가 세트 목록을 조회할 때마다 세트 개수만큼 추가 쿼리가 나간다. 목록 응답(`TimetableSetSummaryResponse`)은 classrooms를 쓰지도 않는다.

## 범위

- `TimetableSetRepository`/`TimetableSetJpaRepository`/`TimetableSetPersistenceAdapter`에 비관적 락 조회 메서드 추가
- `CreateTimetableSlotService`/`UpdateTimetableSlotService`가 그 메서드를 쓰도록 변경
- `CalendarEventEntity`/`TimetableSlotEntity`/`TimetableSetEntity`에 `@Version` 추가 + 각 PersistenceAdapter의 update 경로에 Conflict 예외 매핑 추가
- `TimetableSetEntity.classrooms`에 `@Fetch(FetchMode.SUBSELECT)` 추가
- 위 세 가지를 검증하는 테스트 추가(동시성 테스트 포함)

## 제외 범위

- `google` 도메인, `chat`/`messenger` 등 다른 도메인의 동시성 문제는 이번 범위가 아니다.
- 낙관적 락 충돌을 프론트에 노출하는 UX(재시도 버튼, 병합 화면 등)는 이번 범위가 아니다 — 서버는 409만 내려주고 프론트 대응은 별도.
- `TimetableSlot` 자체에 대한 비관적 락은 걸지 않는다 — 부모 `TimetableSet` 락으로 충분하다(아래 참고).
- `DeleteTimetableSlotService`/`DeleteTimetableSetService`는 겹침 검사가 없으므로 락 대상에서 제외한다.

## 1. 교실 중복 예약 레이스 컨디션 — `TimetableSet` 비관적 락

### 설계

수업 슬롯 생성/수정은 항상 하나의 `TimetableSet` 안에서 겹침을 검사한다. 그 부모 `TimetableSet` row를 트랜잭션 시작 시 `SELECT ... FOR UPDATE`로 잠그면, 같은 세트에 대한 슬롯 쓰기(생성/수정)가 자연스럽게 직렬화된다. 다른 세트끼리는 서로 영향을 주지 않는다.

### 코드 변경

`TimetableSetRepository`(도메인 포트)에 메서드 추가:

```java
Optional<TimetableSet> findByIdForUpdate(Long id);
```

`TimetableSetJpaRepository`:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select t from TimetableSetEntity t where t.id = :id")
Optional<TimetableSetEntity> findByIdForUpdate(@Param("id") Long id);
```

`TimetableSetPersistenceAdapter`에 `findByIdForUpdate` 구현 추가(기존 `toDomain` 재사용).

`CreateTimetableSlotService.createSlot(...)`, `UpdateTimetableSlotService.updateSlot(...)` 둘 다 맨 앞의:

```java
timetableSetRepository.findById(command.timetableSetId())
```

를:

```java
timetableSetRepository.findByIdForUpdate(command.timetableSetId())
```

로 교체한다. `orElseThrow(TimetableSetNotFoundException::new)`는 그대로 유지.

이 한 줄 교체 외에는 겹침 검사·저장 로직을 손대지 않는다 — 이미 같은 트랜잭션(`@Transactional`) 안에서 실행되고 있으므로, 락을 먼저 잡기만 하면 나머지는 자동으로 안전해진다.

## 2. 낙관적 락(`@Version`)

### 설계

`users` 도메인의 기존 패턴(`OptimisticLockingFailureException` → 도메인 Conflict 예외)을 `calendar`/`timetable`에도 동일하게 적용한다. API 요청/응답에 `version`을 노출하지 않는다 — 순수 서버 내부 방어이며, 클라이언트는 그냥 409를 받는다.

### 엔티티 변경

`CalendarEventEntity`, `TimetableSlotEntity`, `TimetableSetEntity` 각각에:

```java
@Version
@Column(nullable = false)
private Long version;
```

추가(Lombok `@Builder` 생성자에는 포함하지 않는다 — `version`은 항상 0으로 시작하고 JPA가 관리하므로 신규 생성 시 애플리케이션이 값을 넣을 필요가 없다).

### 예외 클래스

각 도메인에 `ConflictException`을 상속하는 Conflict 예외를 하나씩 추가한다(패턴은 `ProfileUpdateConflictException`과 동일):

- `calendar/domain/exception/CalendarEventUpdateConflictException.java` — `CalendarErrorCode`에 새 항목 추가
- `timetable/domain/exception/TimetableSlotUpdateConflictException.java` — `TimetableErrorCode`에 새 항목 추가
- `timetable/domain/exception/TimetableSetUpdateConflictException.java` — `TimetableErrorCode`에 새 항목 추가

### PersistenceAdapter 변경

각 어댑터의 `save`(update 경로, 즉 `updateExisting`을 호출하는 분기)에서 `flush()` 후 `OptimisticLockingFailureException`을 잡아 해당 도메인 Conflict 예외로 변환한다. `CalendarEventPersistenceAdapter`/`TimetableSlotPersistenceAdapter`/`TimetableSetPersistenceAdapter` 모두 지금은 `save()` 안에서 JPA `save()` 호출 뒤 바로 반환하는 구조라, `updateExisting()` 분기에서 명시적으로 `flush()`를 호출하고 그 지점에서 예외를 잡도록 바꿔야 한다(`save()` 자체는 flush를 즉시 강제하지 않을 수 있으므로).

## 3. N+1 쿼리 — `@Fetch(FetchMode.SUBSELECT)`

`TimetableSetEntity.classrooms` 필드에 Hibernate 어노테이션 하나만 추가한다:

```java
@ElementCollection(fetch = FetchType.EAGER)
@Fetch(FetchMode.SUBSELECT)
@CollectionTable(name = "timetable_set_classroom", joinColumns = @JoinColumn(name = "timetable_set_id"))
private List<TimetableClassroomEmbeddable> classrooms = new ArrayList<>();
```

세트 목록 N개를 조회할 때 기존에는 classrooms 조회 쿼리가 N번 추가로 나갔는데, 이제는 "세트 목록 1번 + 전체 세트의 classrooms를 한 번에 가져오는 서브쿼리 1번"으로 줄어든다. 도메인 모델(`TimetableSet.restore()`가 classrooms를 필수로 받는 구조)과 상세 조회(`getTimetableSet`, 세트 1개만 조회하므로 원래도 영향 없음)는 그대로 둔다.

## 마이그레이션

`src/main/resources/db/migration/be5/V5.1.13__add_version_columns_for_optimistic_lock.sql`

> **(해결됨 — 최종 브랜치 리뷰에서 확인)** 별도로 진행 중이던 시간표 색상 PR이 `V5.1.12`를 먼저 예약해둔 상태였다. 구현 시점에는 그 PR이 아직 `origin/develop`에 머지되지 않아 이 파일을 `V5.1.12`로 만들었는데, 이후 그 색상 PR(`V5.1.12__add_color_to_timetable_slot.sql`)이 이 브랜치보다 먼저 `origin/develop`에 머지되면서 버전 번호가 충돌했다. 최종 홀리스틱 리뷰에서 이를 발견해 SQL 내용은 그대로 두고 파일명만 `V5.1.13`으로 재변경해 해결했다.

```sql
ALTER TABLE calendar_events ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE timetable_slot ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE timetable_set ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
```

기존 행은 전부 `version = 0`으로 시작한다(정상 동작 — 첫 수정 시점에 0→1로 증가).

## 다른 도메인 영향

- 없음. `calendar`/`timetable` 도메인 내부로 닫혀 있다. `users`의 기존 Conflict 예외 패턴을 참고만 하고 코드는 공유하지 않는다(각 도메인이 자기 `ErrorCode`/예외 클래스를 갖는 기존 컨벤션을 따름).

## 테스트 전략

- **레이스 컨디션**: 실제 DB(Testcontainers) 기준으로 같은 `TimetableSet`·같은 강의실·겹치는 시간대에 두 스레드가 동시에 슬롯 생성을 시도하는 테스트를 추가한다. `CountDownLatch`로 두 스레드가 동시에 트랜잭션을 시작하도록 맞추고, 하나는 성공(id 반환)하고 다른 하나는 `ClassroomTimeConflictException`으로 실패하는지 확인한다. H2 등 인메모리 DB는 InnoDB 락 동작을 정확히 재현하지 못하므로 반드시 Testcontainers MySQL로 검증한다.
- **낙관적 락**: `@DataJpaTest` 또는 `@SpringBootTest`에서 같은 행을 두 번 조회(영속성 컨텍스트를 `EntityManager.clear()`로 분리)한 뒤, 첫 번째 수정을 커밋하고 두 번째 수정을 시도하면 각 도메인의 Conflict 예외가 발생하는지 확인한다. 세 엔티티(calendar/slot/set) 각각에 대해 작성한다.
- **N+1**: 강의실이 여러 개인 세트를 여러 개 만든 뒤 목록 조회 시 실행되는 SQL 쿼리 수를 세는 테스트를 추가한다(Hibernate `Statistics` API로 `getPrepareStatementCount()` 확인, 또는 기존 프로젝트에 datasource-proxy류 도구가 있으면 그걸 재사용). 초안에서는 `getQueryExecutionCount()`를 검토했으나, 이 메서드는 명시적 HQL/JPQL 쿼리 실행만 집계하고 EAGER 컬렉션을 엔티티별로 초기화할 때 발생하는 묵시적 select(N+1의 원인)는 집계하지 않아 N+1이 있어도 값이 항상 1로 남는 것이 확인되어, DB로 나가는 실제 SQL 왕복 횟수를 재는 `getPrepareStatementCount()`로 교체했다.
- 기존 회귀 테스트(도메인/서비스/컨트롤러)는 전부 그대로 통과해야 한다 — 락/버전 추가가 기존 성공 경로의 동작을 바꾸지 않는지 확인한다.

## 성공 기준

- 같은 교실·시간대에 동시 슬롯 생성을 시도하면 하나만 성공하고 나머지는 `409 TIMETABLE_409_1`(`ClassroomTimeConflictException`)로 거절된다.
- `CalendarEvent`/`TimetableSlot`/`TimetableSet`을 동시에 수정하면 늦게 커밋되는 쪽이 조용히 덮어쓰지 않고 409로 거절된다.
- 시간표 세트 목록 조회 시 발생하는 쿼리 수가 세트 개수에 비례해 늘지 않는다(2번 고정).
- 전체 Flyway 마이그레이션과 기존 테스트 스위트가 깨지지 않는다.
