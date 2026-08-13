# 캘린더/시간표 동시성·정합성 보완 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 교실 중복 예약 레이스 컨디션을 비관적 락으로 막고, `CalendarEvent`/`TimetableSlot`/`TimetableSet`에 낙관적 락(`@Version`)을 추가하고, 시간표 세트 목록 조회의 N+1 쿼리를 고친다.

**Architecture:** 슬롯 생성/수정은 부모 `TimetableSet` row를 `SELECT ... FOR UPDATE`로 잠가 같은 세트 안의 쓰기를 직렬화한다(이미 이 코드베이스의 `RoleJpaRepository` 등에서 쓰는 `@Lock(LockModeType.PESSIMISTIC_WRITE)` 패턴을 그대로 따른다). 낙관적 락은 세 엔티티에 `@Version`을 추가하고, `users` 도메인의 기존 `OptimisticLockingFailureException` → 도메인 Conflict 예외 패턴을 재사용한다. N+1은 `@Fetch(FetchMode.SUBSELECT)` 한 줄로 해결한다. 세 가지 모두 도메인 모델(`CalendarEvent`/`TimetableSlot`/`TimetableSet`)은 건드리지 않고 인프라(엔티티/어댑터)와 애플리케이션 서비스 레이어에서만 처리한다.

**Tech Stack:** Java 17, Spring Boot, Spring Data JPA(Hibernate), MySQL 8, Flyway, JUnit 5, AssertJ, Mockito, Testcontainers, Gradle.

**설계 문서:** `docs/superpowers/specs/2026-08-14-timetable-calendar-concurrency-design.md`

---

### Task 1: 마이그레이션 — `version` 컬럼 3개 추가

**Files:**
- Create: `src/main/resources/db/migration/be5/V5.1.13__add_version_columns_for_optimistic_lock.sql`

- [ ] **Step 1: 마이그레이션 파일 작성**

먼저 `origin/develop`의 `src/main/resources/db/migration/be5/` 최신 버전을 확인한다. 이 계획을 쓴 시점 기준으로는 `V5.1.11`까지 있고, 별도로 진행 중인 시간표 색상 PR이 `V5.1.12`를 예약해뒀다. 그 PR이 이미 머지됐으면 아래 파일명을 그대로 쓰고, 아직이면 `V5.1.12`로 낮춰서 쓴다.

```sql
ALTER TABLE calendar_events ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE timetable_slot ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE timetable_set ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
```

- [ ] **Step 2: 빈 DB 기준 전체 마이그레이션 검증**

Run: `./gradlew test --tests "com.academy.mudogroupware.FlywayFreshDatabaseMigrationTest"`
Expected: PASS.

- [ ] **Step 3: 커밋**

```bash
git add src/main/resources/db/migration/be5/V5.1.13__add_version_columns_for_optimistic_lock.sql
git commit -m "feat: calendar_events/timetable_slot/timetable_set에 version 컬럼 추가"
```

---

### Task 2: `TimetableSet` 비관적 락 — 교실 중복 예약 레이스 컨디션 해결

**Files:**
- Modify: `src/main/java/com/academy/mudogroupware/timetable/domain/repository/TimetableSetRepository.java`
- Modify: `src/main/java/com/academy/mudogroupware/timetable/infrastructure/persistence/TimetableSetJpaRepository.java`
- Modify: `src/main/java/com/academy/mudogroupware/timetable/infrastructure/persistence/TimetableSetPersistenceAdapter.java`
- Modify: `src/main/java/com/academy/mudogroupware/timetable/application/service/CreateTimetableSlotService.java`
- Modify: `src/main/java/com/academy/mudogroupware/timetable/application/service/UpdateTimetableSlotService.java`
- Test: `src/test/java/com/academy/mudogroupware/timetable/application/service/CreateTimetableSlotConcurrencyTest.java` (신규)

- [ ] **Step 1: `TimetableSetRepository`에 포트 메서드 추가**

```java
package com.academy.mudogroupware.timetable.domain.repository;

import java.util.List;
import java.util.Optional;

import com.academy.mudogroupware.timetable.domain.model.TimetableSet;

public interface TimetableSetRepository {

    TimetableSet save(TimetableSet timetableSet);

    Optional<TimetableSet> findById(Long id);

    Optional<TimetableSet> findByIdForUpdate(Long id);

    List<TimetableSet> findAll();

    void deleteById(Long id);
}
```

- [ ] **Step 2: `TimetableSetJpaRepository`에 비관적 락 쿼리 추가**

이 코드베이스의 기존 관례(`RoleJpaRepository.findWithPermissionsByIdForUpdate` 참고)를 그대로 따른다.

```java
package com.academy.mudogroupware.timetable.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface TimetableSetJpaRepository extends JpaRepository<TimetableSetEntity, Long> {

    List<TimetableSetEntity> findAllByOrderByStartDateDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TimetableSetEntity t where t.id = :id")
    Optional<TimetableSetEntity> findByIdForUpdate(Long id);
}
```

- [ ] **Step 3: `TimetableSetPersistenceAdapter`에 구현 추가**

`findById` 메서드 바로 아래에 추가:

```java
    @Override
    public Optional<TimetableSet> findByIdForUpdate(Long id) {
        return timetableSetJpaRepository.findByIdForUpdate(id).map(this::toDomain);
    }
```

- [ ] **Step 4: `CreateTimetableSlotService`가 락을 걸도록 수정**

`findById` 호출을 `findByIdForUpdate`로 바꾼다(그 외 로직은 그대로):

```java
    @Override
    public Long createSlot(CreateTimetableSlotCommand command) {
        TimetableSet set = timetableSetRepository.findByIdForUpdate(command.timetableSetId())
                .orElseThrow(TimetableSetNotFoundException::new);

        TimetableSlot candidate = TimetableSlot.create(
                set.getId(), command.classType(), command.dayOfWeek(), command.classroomCode(),
                command.startTime(), command.endTime(), command.grade(), command.teacherName(),
                command.subjectName(), set.getStartDate(), set.getEndDate());

        boolean conflicts = timetableSlotRepository
                .findAllByTimetableSetIdAndClassroomCode(set.getId(), command.classroomCode()).stream()
                .anyMatch(candidate::overlaps);
        if (conflicts) {
            throw new ClassroomTimeConflictException();
        }

        return timetableSlotRepository.save(candidate).getId();
    }
```

- [ ] **Step 5: `UpdateTimetableSlotService`도 동일하게 수정**

```java
    @Override
    public void updateSlot(UpdateTimetableSlotCommand command) {
        if (command.scope() != UpdateScope.ALL) {
            throw new UnsupportedSlotScopeException();
        }

        timetableSetRepository.findByIdForUpdate(command.timetableSetId())
                .orElseThrow(TimetableSetNotFoundException::new);

        TimetableSlot slot = timetableSlotRepository.findById(command.timetableSlotId())
                .filter(found -> found.getTimetableSetId().equals(command.timetableSetId()))
                .orElseThrow(TimetableSlotNotFoundException::new);

        TimetableSlot candidate = TimetableSlot.create(
                slot.getTimetableSetId(), command.classType(), command.dayOfWeek(), command.classroomCode(),
                command.startTime(), command.endTime(), command.grade(), command.teacherName(),
                command.subjectName(), slot.getEffectiveFrom(), slot.getEffectiveUntil());

        List<TimetableSlot> othersInClassroom = timetableSlotRepository
                .findAllByTimetableSetIdAndClassroomCode(slot.getTimetableSetId(), command.classroomCode()).stream()
                .filter(other -> !other.getId().equals(slot.getId()))
                .toList();
        boolean conflicts = othersInClassroom.stream().anyMatch(candidate::overlaps);
        if (conflicts) {
            throw new ClassroomTimeConflictException();
        }

        slot.applyFullUpdate(command.classType(), command.dayOfWeek(), command.classroomCode(),
                command.startTime(), command.endTime(), command.grade(), command.teacherName(),
                command.subjectName());

        timetableSlotRepository.save(slot);
    }
```

- [ ] **Step 6: 컴파일 확인**

Run: `./gradlew compileTestJava`
Expected: BUILD SUCCESSFUL. `CreateTimetableSlotServiceTest`/`UpdateTimetableSlotServiceTest`는 `timetableSetRepository.findById(...)`를 모킹하고 있었는데, 서비스가 이제 `findByIdForUpdate`를 호출하므로 그 두 테스트 파일의 모킹 대상도 바꿔야 컴파일은 되지만 테스트가 통과한다. 두 파일에서 `when(timetableSetRepository.findById(...))`를 전부 `when(timetableSetRepository.findByIdForUpdate(...))`로 바꾼다(다른 로직은 손대지 않는다). 예를 들어 `CreateTimetableSlotServiceTest.java`의 세 테스트 모두, `UpdateTimetableSlotServiceTest.java`의 관련 테스트 전부.

- [ ] **Step 7: 기존 단위 테스트로 회귀 확인**

Run: `./gradlew test --tests "com.academy.mudogroupware.timetable.application.service.CreateTimetableSlotServiceTest" --tests "com.academy.mudogroupware.timetable.application.service.UpdateTimetableSlotServiceTest"`
Expected: 전부 PASS.

- [ ] **Step 8: 실제 레이스 컨디션이 막히는지 증명하는 동시성 테스트 작성(TDD — 먼저 락 없이 재현)**

새 파일 `CreateTimetableSlotConcurrencyTest.java`를 작성한다. Testcontainers MySQL을 쓰는 `@SpringBootTest` 기반 테스트다(H2는 InnoDB row lock을 정확히 재현하지 않으므로 반드시 실제 MySQL로 검증한다).

```java
package com.academy.mudogroupware.timetable.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import com.academy.mudogroupware.timetable.application.command.CreateTimetableSetCommand;
import com.academy.mudogroupware.timetable.application.command.CreateTimetableSlotCommand;
import com.academy.mudogroupware.timetable.application.usecase.CreateTimetableSetUseCase;
import com.academy.mudogroupware.timetable.application.usecase.CreateTimetableSlotUseCase;
import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.Grade;
import com.academy.mudogroupware.timetable.domain.model.TimetableClassroom;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class CreateTimetableSlotConcurrencyTest {

    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired private CreateTimetableSetUseCase createTimetableSetUseCase;
    @Autowired private CreateTimetableSlotUseCase createTimetableSlotUseCase;

    @Test
    void onlyOneOfTwoConcurrentCreatesForTheSameClassroomAndTimeSucceeds() throws Exception {
        Long timetableSetId = createTimetableSetUseCase.createTimetableSet(new CreateTimetableSetCommand(
                "동시성 테스트 세트", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31),
                LocalTime.of(8, 0), LocalTime.of(22, 0), Set.of(DayOfWeek.MONDAY), 30,
                List.of(new TimetableClassroom("6층", "601"))));

        int attempts = 2;
        CountDownLatch readyLatch = new CountDownLatch(attempts);
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();

        try {
            List<CompletableFuture<Void>> futures = List.of(1, 2).stream()
                    .map(i -> CompletableFuture.runAsync(() -> {
                        readyLatch.countDown();
                        awaitUninterruptibly(startLatch);
                        try {
                            createTimetableSlotUseCase.createSlot(new CreateTimetableSlotCommand(
                                    timetableSetId, ClassType.CLASS, DayOfWeek.MONDAY, "601",
                                    LocalTime.of(9, 0), LocalTime.of(11, 0), Grade.HIGH_3, "정T", "미적분"));
                            successCount.incrementAndGet();
                        } catch (com.academy.mudogroupware.timetable.domain.exception.ClassroomTimeConflictException e) {
                            conflictCount.incrementAndGet();
                        }
                    }, executor))
                    .toList();

            readyLatch.await();
            startLatch.countDown();
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
        } finally {
            executor.shutdown();
        }

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);
    }

    private static void awaitUninterruptibly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
```

- [ ] **Step 9: 락을 걸기 전 상태로 잠깐 되돌려서 RED 확인(선택, 강력 추천)**

`CreateTimetableSlotService`의 `findByIdForUpdate`를 일시적으로 `findById`로 되돌리고 이 테스트를 실행하면, 두 스레드 다 성공(`successCount == 2`)하거나 결과가 불안정해야 한다(레이스 컨디션이 재현됨). 확인했으면 다시 `findByIdForUpdate`로 되돌린다. Docker가 없는 환경이면 이 단계는 건너뛰고 Step 10으로 간다(테스트 자체가 `disabledWithoutDocker = true`라 스킵된다).

- [ ] **Step 10: 락을 건 상태로 테스트 실행 — GREEN 확인**

Run: `./gradlew test --tests "com.academy.mudogroupware.timetable.application.service.CreateTimetableSlotConcurrencyTest"`
Expected: PASS(Docker 있는 환경 기준). `successCount == 1`, `conflictCount == 1`.

- [ ] **Step 11: 커밋**

```bash
git add src/main/java/com/academy/mudogroupware/timetable/domain/repository/TimetableSetRepository.java \
        src/main/java/com/academy/mudogroupware/timetable/infrastructure/persistence/TimetableSetJpaRepository.java \
        src/main/java/com/academy/mudogroupware/timetable/infrastructure/persistence/TimetableSetPersistenceAdapter.java \
        src/main/java/com/academy/mudogroupware/timetable/application/service/CreateTimetableSlotService.java \
        src/main/java/com/academy/mudogroupware/timetable/application/service/UpdateTimetableSlotService.java \
        src/test/java/com/academy/mudogroupware/timetable/application/service/CreateTimetableSlotServiceTest.java \
        src/test/java/com/academy/mudogroupware/timetable/application/service/UpdateTimetableSlotServiceTest.java \
        src/test/java/com/academy/mudogroupware/timetable/application/service/CreateTimetableSlotConcurrencyTest.java
git commit -m "fix: 교실 중복 예약 레이스 컨디션을 TimetableSet 비관적 락으로 해결"
```

## 참고: Task 3~5는 같은 패턴을 세 엔티티에 반복 적용

`TimetableSet`/`TimetableSlot`/`CalendarEvent` 각각에 `@Version` 추가 + 어댑터에서 `OptimisticLockingFailureException` 잡아서 도메인 Conflict 예외로 변환. 순서는 상관없다.

---

### Task 3: `TimetableSetEntity` 낙관적 락

**Files:**
- Modify: `src/main/java/com/academy/mudogroupware/timetable/domain/exception/TimetableErrorCode.java`
- Create: `src/main/java/com/academy/mudogroupware/timetable/domain/exception/TimetableSetUpdateConflictException.java`
- Modify: `src/main/java/com/academy/mudogroupware/timetable/infrastructure/persistence/TimetableSetEntity.java`
- Modify: `src/main/java/com/academy/mudogroupware/timetable/infrastructure/persistence/TimetableSetPersistenceAdapter.java`
- Test: `src/test/java/com/academy/mudogroupware/timetable/infrastructure/persistence/TimetableSetPersistenceAdapterDataJpaTest.java`

- [ ] **Step 1: `TimetableErrorCode`에 항목 추가**

`INVALID_CLASSROOM` 다음 줄에 추가(세미콜론 위치 주의):

```java
    INVALID_CLASSROOM(HttpStatus.BAD_REQUEST, "TIMETABLE_400_9", "강의실 정보가 올바르지 않습니다."),
    SET_UPDATE_CONFLICT(HttpStatus.CONFLICT, "TIMETABLE_409_2", "다른 요청이 먼저 정보를 수정했습니다. 다시 조회한 뒤 시도해주세요.");
```

- [ ] **Step 2: 예외 클래스 작성**

```java
package com.academy.mudogroupware.timetable.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.ConflictException;

public class TimetableSetUpdateConflictException extends ConflictException {

    public TimetableSetUpdateConflictException(Throwable cause) {
        super(TimetableErrorCode.SET_UPDATE_CONFLICT, TimetableErrorCode.SET_UPDATE_CONFLICT.getMessage(), cause);
    }
}
```

- [ ] **Step 3: `TimetableSetEntity`에 `@Version` 추가**

`slotUnitMinutes` 필드 다음, `classrooms` 필드 앞에 추가:

```java
    @Column(name = "slot_unit_minutes", nullable = false)
    private int slotUnitMinutes;

    @Version
    @Column(nullable = false)
    private Long version;

    @ElementCollection(fetch = FetchType.EAGER)
```

import 추가: `import jakarta.persistence.Version;`

`@Builder` 생성자와 `update(...)` 메서드는 손대지 않는다 — `version`은 JPA가 관리하므로 애플리케이션 코드가 값을 넣거나 읽을 필요가 없다.

- [ ] **Step 4: `TimetableSetPersistenceAdapter.save(...)`가 낙관적 락 충돌을 잡도록 수정**

```java
    @Override
    public TimetableSet save(TimetableSet timetableSet) {
        if (timetableSet.getId() != null) {
            TimetableSetEntity entity = updateExisting(timetableSet);
            try {
                timetableSetJpaRepository.flush();
            } catch (OptimisticLockingFailureException exception) {
                throw new TimetableSetUpdateConflictException(exception);
            }
            return toDomain(entity);
        }
        return toDomain(timetableSetJpaRepository.save(toEntity(timetableSet)));
    }
```

import 추가: `import org.springframework.dao.OptimisticLockingFailureException;`, `import com.academy.mudogroupware.timetable.domain.exception.TimetableSetUpdateConflictException;`

- [ ] **Step 5: 낙관적 락 충돌 테스트는 `@DataJpaTest`가 아니라 Mockito 단위 테스트로 작성한다**

`@DataJpaTest`에서 `entityManager.clear()`로 "두 세션"을 흉내 내는 방식은 실제로는 동작하지 않는다 — `clear()` 이후 `getReferenceById()`가 새로 프록시를 만들면서 그 시점의 최신(이미 첫 번째 수정이 반영된) row를 기준으로 버전을 잡기 때문에, 두 번째 저장도 조용히 성공해버린다(같은 트랜잭션/영속성 컨텍스트 안에서 `clear()`로 다른 세션을 재현하려는 시도 자체가 1차 캐시 동일성 보장을 깨뜨린다). 실제 프로덕션에서는 요청 하나 = 트랜잭션 하나 = 영속성 컨텍스트 하나라 이 문제가 없다.

대신 이 코드베이스의 기존 동일 패턴(`UserRepositoryImplTest.convertsOptimisticLockConflictOnUpdateProfileToProfileUpdateConflictException`, `src/test/java/com/academy/mudogroupware/users/infrastructure/persistence/UserRepositoryImplTest.java`)을 그대로 따라간다 — JPA 리포지토리를 Mockito로 모킹하고 `flush()`가 `OptimisticLockingFailureException`을 던지도록 스텁한 뒤, 어댑터가 그걸 도메인 Conflict 예외로 바꾸는지만 검증한다. 실제 DB로 Hibernate의 낙관적 락 자체를 다시 증명할 필요는 없다(그건 라이브러리가 이미 보장하는 동작이다).

`TimetableSetPersistenceAdapterDataJpaTest.java`는 이번 태스크에서 아예 건드리지 않는다. 새 파일을 만든다:

`src/test/java/com/academy/mudogroupware/timetable/infrastructure/persistence/TimetableSetPersistenceAdapterTest.java`

```java
package com.academy.mudogroupware.timetable.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;

import com.academy.mudogroupware.timetable.domain.exception.TimetableSetUpdateConflictException;
import com.academy.mudogroupware.timetable.domain.model.TimetableClassroom;
import com.academy.mudogroupware.timetable.domain.model.TimetableSet;

class TimetableSetPersistenceAdapterTest {

    @Test
    void convertsOptimisticLockConflictOnUpdateToTimetableSetUpdateConflictException() {
        TimetableSetJpaRepository jpaRepository = mock(TimetableSetJpaRepository.class);
        TimetableSetPersistenceAdapter adapter = new TimetableSetPersistenceAdapter(jpaRepository);
        TimetableSetEntity entity = TimetableSetEntity.builder()
                .id(1L)
                .name("이름")
                .startDate(LocalDate.of(2026, 7, 20))
                .endDate(LocalDate.of(2026, 8, 16))
                .operatingStartTime(LocalTime.of(8, 30))
                .operatingEndTime(LocalTime.of(22, 0))
                .operatingDays("MONDAY")
                .slotUnitMinutes(30)
                .classrooms(List.of(new TimetableClassroomEmbeddable("6층", "601")))
                .build();
        when(jpaRepository.getReferenceById(1L)).thenReturn(entity);
        OptimisticLockingFailureException conflict = new OptimisticLockingFailureException("concurrent update");
        doThrow(conflict).when(jpaRepository).flush();

        TimetableSet domain = TimetableSet.restore(
                1L, "수정된 이름", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16),
                LocalTime.of(8, 30), LocalTime.of(22, 0), Set.of(DayOfWeek.MONDAY), 30,
                List.of(new TimetableClassroom("6층", "601")), null, null);

        assertThatThrownBy(() -> adapter.save(domain))
                .isInstanceOf(TimetableSetUpdateConflictException.class)
                .hasCause(conflict);
    }
}
```

이 코드는 실제 `TimetableSetEntity`/`TimetableClassroomEmbeddable`/`TimetableSet`의 정확한 생성자/빌더 시그니처를 다시 한 번 확인하고 필요하면 맞춰서 고친다(패턴과 검증하려는 동작 자체는 그대로 유지).

- [ ] **Step 6: 테스트 실행**

Run: `./gradlew test --tests "com.academy.mudogroupware.timetable.infrastructure.persistence.TimetableSetPersistenceAdapterTest" --tests "com.academy.mudogroupware.timetable.infrastructure.persistence.TimetableSetPersistenceAdapterDataJpaTest"`
Expected: 전부 PASS. `TimetableSetPersistenceAdapterDataJpaTest`는 이번 태스크에서 손대지 않았으므로 기존 그대로 통과해야 한다(엔티티에 `@Version`이 추가된 상태에서도 정상 동작하는지 확인하는 회귀 검증 겸).

- [ ] **Step 7: 커밋**

```bash
git add src/main/java/com/academy/mudogroupware/timetable/domain/exception/TimetableErrorCode.java \
        src/main/java/com/academy/mudogroupware/timetable/domain/exception/TimetableSetUpdateConflictException.java \
        src/main/java/com/academy/mudogroupware/timetable/infrastructure/persistence/TimetableSetEntity.java \
        src/main/java/com/academy/mudogroupware/timetable/infrastructure/persistence/TimetableSetPersistenceAdapter.java \
        src/test/java/com/academy/mudogroupware/timetable/infrastructure/persistence/TimetableSetPersistenceAdapterTest.java
git commit -m "feat: TimetableSetEntity에 낙관적 락(@Version) 추가"
```

---

### Task 4: `TimetableSlotEntity` 낙관적 락

**Files:**
- Modify: `src/main/java/com/academy/mudogroupware/timetable/domain/exception/TimetableErrorCode.java`
- Create: `src/main/java/com/academy/mudogroupware/timetable/domain/exception/TimetableSlotUpdateConflictException.java`
- Modify: `src/main/java/com/academy/mudogroupware/timetable/infrastructure/persistence/TimetableSlotEntity.java`
- Modify: `src/main/java/com/academy/mudogroupware/timetable/infrastructure/persistence/TimetableSlotPersistenceAdapter.java`
- Test: `src/test/java/com/academy/mudogroupware/timetable/infrastructure/persistence/TimetableSlotPersistenceAdapterDataJpaTest.java`

- [ ] **Step 1: `TimetableErrorCode`에 항목 추가**

Task 3에서 이미 `SET_UPDATE_CONFLICT`를 `TIMETABLE_409_2`로 추가했으므로, 이번엔 그 다음 줄에 추가한다(세미콜론이 마지막 항목으로 옮겨감):

```java
    SET_UPDATE_CONFLICT(HttpStatus.CONFLICT, "TIMETABLE_409_2", "다른 요청이 먼저 정보를 수정했습니다. 다시 조회한 뒤 시도해주세요."),
    SLOT_UPDATE_CONFLICT(HttpStatus.CONFLICT, "TIMETABLE_409_3", "다른 요청이 먼저 정보를 수정했습니다. 다시 조회한 뒤 시도해주세요.");
```

- [ ] **Step 2: 예외 클래스 작성**

```java
package com.academy.mudogroupware.timetable.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.ConflictException;

public class TimetableSlotUpdateConflictException extends ConflictException {

    public TimetableSlotUpdateConflictException(Throwable cause) {
        super(TimetableErrorCode.SLOT_UPDATE_CONFLICT, TimetableErrorCode.SLOT_UPDATE_CONFLICT.getMessage(), cause);
    }
}
```

- [ ] **Step 3: `TimetableSlotEntity`에 `@Version` 추가**

`effectiveUntil` 필드 다음에 추가:

```java
    @Column(name = "effective_until", nullable = false)
    private LocalDate effectiveUntil;

    @Version
    @Column(nullable = false)
    private Long version;
```

import 추가: `import jakarta.persistence.Version;`. `@Builder` 생성자와 `update(...)`는 손대지 않는다.

- [ ] **Step 4: `TimetableSlotPersistenceAdapter.save(...)` 수정**

```java
    @Override
    public TimetableSlot save(TimetableSlot slot) {
        if (slot.getId() != null) {
            TimetableSlotEntity entity = updateExisting(slot);
            try {
                timetableSlotJpaRepository.flush();
            } catch (OptimisticLockingFailureException exception) {
                throw new TimetableSlotUpdateConflictException(exception);
            }
            return toDomain(entity);
        }
        return toDomain(timetableSlotJpaRepository.save(toEntity(slot)));
    }
```

import 추가: `import org.springframework.dao.OptimisticLockingFailureException;`, `import com.academy.mudogroupware.timetable.domain.exception.TimetableSlotUpdateConflictException;`

- [ ] **Step 5: 낙관적 락 충돌 테스트는 Task 3과 같은 이유로 `@DataJpaTest`가 아니라 Mockito 단위 테스트로 작성한다**

Task 3에서 확인했듯 `@DataJpaTest` + `entityManager.clear()`로 두 세션을 흉내 내는 방식은 실제로 충돌을 재현하지 못한다(1차 캐시 동일성이 깨지면서 `getReferenceById()`가 항상 최신 row를 기준으로 프록시를 만든다). `TimetableSlotPersistenceAdapterDataJpaTest.java`는 이번 태스크에서 건드리지 않는다. `UserRepositoryImplTest`/Task 3의 `TimetableSetPersistenceAdapterTest`와 동일한 패턴으로 새 파일을 만든다:

`src/test/java/com/academy/mudogroupware/timetable/infrastructure/persistence/TimetableSlotPersistenceAdapterTest.java`

```java
package com.academy.mudogroupware.timetable.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;

import com.academy.mudogroupware.timetable.domain.exception.TimetableSlotUpdateConflictException;
import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.Grade;
import com.academy.mudogroupware.timetable.domain.model.TimetableSlot;

class TimetableSlotPersistenceAdapterTest {

    @Test
    void convertsOptimisticLockConflictOnUpdateToTimetableSlotUpdateConflictException() {
        TimetableSlotJpaRepository jpaRepository = mock(TimetableSlotJpaRepository.class);
        TimetableSlotPersistenceAdapter adapter = new TimetableSlotPersistenceAdapter(jpaRepository);
        TimetableSlotEntity entity = TimetableSlotEntity.builder()
                .id(1L)
                .timetableSetId(1L)
                .classType(ClassType.CLASS)
                .dayOfWeek(DayOfWeek.MONDAY)
                .classroomCode("601")
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(11, 0))
                .grade(Grade.HIGH_3)
                .teacherName("정T")
                .subjectName("미적분")
                .effectiveFrom(LocalDate.of(2026, 7, 20))
                .effectiveUntil(LocalDate.of(2026, 8, 16))
                .build();
        when(jpaRepository.getReferenceById(1L)).thenReturn(entity);
        OptimisticLockingFailureException conflict = new OptimisticLockingFailureException("concurrent update");
        doThrow(conflict).when(jpaRepository).flush();

        TimetableSlot domain = TimetableSlot.restore(
                1L, 1L, ClassType.SPECIAL, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "수정됨", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16), null, null);

        assertThatThrownBy(() -> adapter.save(domain))
                .isInstanceOf(TimetableSlotUpdateConflictException.class)
                .hasCause(conflict);
    }
}
```

이 코드는 실제 `TimetableSlotEntity`/`TimetableSlot`의 정확한 생성자/빌더 시그니처를 다시 한 번 확인하고 필요하면 맞춰서 고친다(패턴과 검증하려는 동작 자체는 그대로 유지).

- [ ] **Step 6: 테스트 실행**

Run: `./gradlew test --tests "com.academy.mudogroupware.timetable.infrastructure.persistence.TimetableSlotPersistenceAdapterTest" --tests "com.academy.mudogroupware.timetable.infrastructure.persistence.TimetableSlotPersistenceAdapterDataJpaTest"`
Expected: 전부 PASS. `TimetableSlotPersistenceAdapterDataJpaTest`는 이번 태스크에서 손대지 않았으므로 기존 그대로 통과해야 한다.

- [ ] **Step 7: 커밋**

```bash
git add src/main/java/com/academy/mudogroupware/timetable/domain/exception/TimetableErrorCode.java \
        src/main/java/com/academy/mudogroupware/timetable/domain/exception/TimetableSlotUpdateConflictException.java \
        src/main/java/com/academy/mudogroupware/timetable/infrastructure/persistence/TimetableSlotEntity.java \
        src/main/java/com/academy/mudogroupware/timetable/infrastructure/persistence/TimetableSlotPersistenceAdapter.java \
        src/test/java/com/academy/mudogroupware/timetable/infrastructure/persistence/TimetableSlotPersistenceAdapterTest.java
git commit -m "feat: TimetableSlotEntity에 낙관적 락(@Version) 추가"
```

---

### Task 5: `CalendarEventEntity` 낙관적 락

**Files:**
- Modify: `src/main/java/com/academy/mudogroupware/calendar/domain/exception/CalendarErrorCode.java`
- Create: `src/main/java/com/academy/mudogroupware/calendar/domain/exception/CalendarEventUpdateConflictException.java`
- Modify: `src/main/java/com/academy/mudogroupware/calendar/infrastructure/persistence/CalendarEventEntity.java`
- Modify: `src/main/java/com/academy/mudogroupware/calendar/infrastructure/persistence/CalendarEventPersistenceAdapter.java`
- Test: `src/test/java/com/academy/mudogroupware/calendar/infrastructure/persistence/CalendarEventPersistenceAdapterDataJpaTest.java` (신규)

- [ ] **Step 1: `CalendarErrorCode`에 항목 추가**

```java
    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "CALENDAR_404_1", "일정을 찾을 수 없습니다."),
    EVENT_UPDATE_CONFLICT(HttpStatus.CONFLICT, "CALENDAR_409_1", "다른 요청이 먼저 정보를 수정했습니다. 다시 조회한 뒤 시도해주세요.");
```

- [ ] **Step 2: 예외 클래스 작성**

```java
package com.academy.mudogroupware.calendar.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.ConflictException;

public class CalendarEventUpdateConflictException extends ConflictException {

    public CalendarEventUpdateConflictException(Throwable cause) {
        super(CalendarErrorCode.EVENT_UPDATE_CONFLICT, CalendarErrorCode.EVENT_UPDATE_CONFLICT.getMessage(), cause);
    }
}
```

- [ ] **Step 3: `CalendarEventEntity`에 `@Version` 추가**

`createdBy` 필드 다음에 추가:

```java
    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Version
    @Column(nullable = false)
    private Long version;
```

import 추가: `import jakarta.persistence.Version;`. `@Builder` 생성자와 `update(...)`는 손대지 않는다.

- [ ] **Step 4: `CalendarEventPersistenceAdapter.save(...)` 수정**

```java
    @Override
    public CalendarEvent save(CalendarEvent calendarEvent) {
        if (calendarEvent.getId() != null) {
            CalendarEventEntity entity = updateExisting(calendarEvent);
            try {
                calendarEventJpaRepository.flush();
            } catch (OptimisticLockingFailureException exception) {
                throw new CalendarEventUpdateConflictException(exception);
            }
            return toDomain(entity);
        }
        return toDomain(calendarEventJpaRepository.save(toEntity(calendarEvent)));
    }
```

import 추가: `import org.springframework.dao.OptimisticLockingFailureException;`, `import com.academy.mudogroupware.calendar.domain.exception.CalendarEventUpdateConflictException;`

- [ ] **Step 5: 낙관적 락 충돌은 Mockito 단위 테스트로, 저장/조회는 `@DataJpaTest`로 — 두 파일 다 신규 작성**

Task 3/4에서 확인했듯 `@DataJpaTest` + `entityManager.clear()`로 낙관적 락 충돌 자체를 재현하려는 시도는 실패한다. 그래서 이번 태스크는 신규 파일을 2개로 나눠 만든다: 저장/조회 같은 일반 동작은 `@DataJpaTest`로(이 도메인엔 아직 이런 테스트가 없다), 충돌 변환 로직은 `UserRepositoryImplTest`/`TimetableSetPersistenceAdapterTest`와 같은 Mockito 패턴으로.

`src/test/java/com/academy/mudogroupware/calendar/infrastructure/persistence/CalendarEventPersistenceAdapterDataJpaTest.java`(신규):

```java
package com.academy.mudogroupware.calendar.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.academy.mudogroupware.calendar.domain.model.CalendarEvent;
import com.academy.mudogroupware.global.infrastructure.config.TimeConfig;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({TimeConfig.class, CalendarEventPersistenceAdapter.class})
class CalendarEventPersistenceAdapterDataJpaTest {

    @Autowired
    private CalendarEventPersistenceAdapter adapter;

    private CalendarEvent newEvent() {
        return CalendarEvent.create(
                "동시성 테스트 일정", "내용", LocalDateTime.of(2026, 8, 20, 9, 0),
                LocalDateTime.of(2026, 8, 20, 10, 0), false, "FFCC00", 1L);
    }

    @Test
    void savesAndFindsEvent() {
        CalendarEvent saved = adapter.save(newEvent());

        Optional<CalendarEvent> found = adapter.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("동시성 테스트 일정");
    }

    @Test
    void updatesEventFields() {
        CalendarEvent saved = adapter.save(newEvent());
        CalendarEvent loaded = adapter.findById(saved.getId()).orElseThrow();

        loaded.update("수정된 제목", "수정된 내용", loaded.getEventStartAt(), loaded.getEventEndAt(),
                loaded.isAllDay(), loaded.getColor());
        adapter.save(loaded);

        CalendarEvent reloaded = adapter.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getTitle()).isEqualTo("수정된 제목");
    }
}
```

`src/test/java/com/academy/mudogroupware/calendar/infrastructure/persistence/CalendarEventPersistenceAdapterTest.java`(신규, Mockito 단위 테스트):

```java
package com.academy.mudogroupware.calendar.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;

import com.academy.mudogroupware.calendar.domain.exception.CalendarEventUpdateConflictException;
import com.academy.mudogroupware.calendar.domain.model.CalendarEvent;

class CalendarEventPersistenceAdapterTest {

    @Test
    void convertsOptimisticLockConflictOnUpdateToCalendarEventUpdateConflictException() {
        CalendarEventJpaRepository jpaRepository = mock(CalendarEventJpaRepository.class);
        CalendarEventPersistenceAdapter adapter = new CalendarEventPersistenceAdapter(jpaRepository);
        CalendarEventEntity entity = CalendarEventEntity.builder()
                .id(1L)
                .title("제목")
                .content("내용")
                .eventStartAt(LocalDateTime.of(2026, 8, 20, 9, 0))
                .eventEndAt(LocalDateTime.of(2026, 8, 20, 10, 0))
                .allDay(false)
                .color("FFCC00")
                .createdBy(1L)
                .build();
        when(jpaRepository.getReferenceById(1L)).thenReturn(entity);
        OptimisticLockingFailureException conflict = new OptimisticLockingFailureException("concurrent update");
        doThrow(conflict).when(jpaRepository).flush();

        CalendarEvent domain = CalendarEvent.restore(
                1L, "수정된 제목", "내용", LocalDateTime.of(2026, 8, 20, 9, 0),
                LocalDateTime.of(2026, 8, 20, 10, 0), false, "FFCC00", 1L, null, null);

        assertThatThrownBy(() -> adapter.save(domain))
                .isInstanceOf(CalendarEventUpdateConflictException.class)
                .hasCause(conflict);
    }
}
```

이 코드는 실제 `CalendarEventEntity`/`CalendarEvent`의 정확한 생성자/빌더 시그니처를 다시 한 번 확인하고 필요하면 맞춰서 고친다(패턴과 검증하려는 동작 자체는 그대로 유지).

- [ ] **Step 6: 테스트 실행**

Run: `./gradlew test --tests "com.academy.mudogroupware.calendar.infrastructure.persistence.CalendarEventPersistenceAdapterDataJpaTest" --tests "com.academy.mudogroupware.calendar.infrastructure.persistence.CalendarEventPersistenceAdapterTest"`
Expected: 전부 PASS.

- [ ] **Step 7: 커밋**

```bash
git add src/main/java/com/academy/mudogroupware/calendar/domain/exception/CalendarErrorCode.java \
        src/main/java/com/academy/mudogroupware/calendar/domain/exception/CalendarEventUpdateConflictException.java \
        src/main/java/com/academy/mudogroupware/calendar/infrastructure/persistence/CalendarEventEntity.java \
        src/main/java/com/academy/mudogroupware/calendar/infrastructure/persistence/CalendarEventPersistenceAdapter.java \
        src/test/java/com/academy/mudogroupware/calendar/infrastructure/persistence/CalendarEventPersistenceAdapterDataJpaTest.java \
        src/test/java/com/academy/mudogroupware/calendar/infrastructure/persistence/CalendarEventPersistenceAdapterTest.java
git commit -m "feat: CalendarEventEntity에 낙관적 락(@Version) 추가"
```

---

### Task 6: N+1 쿼리 — `TimetableSetEntity.classrooms`에 `@Fetch(FetchMode.SUBSELECT)`

**Files:**
- Modify: `src/main/java/com/academy/mudogroupware/timetable/infrastructure/persistence/TimetableSetEntity.java`
- Test: `src/test/java/com/academy/mudogroupware/timetable/infrastructure/persistence/TimetableSetPersistenceAdapterDataJpaTest.java`

- [ ] **Step 1: 실패하는 테스트 먼저 작성(쿼리 수 측정)**

`TimetableSetPersistenceAdapterDataJpaTest`에 Hibernate 통계 기반 쿼리 수 테스트를 추가한다. `@DataJpaTest`는 기본적으로 Hibernate 통계를 켜지 않으므로, 이 테스트 클래스 상단의 `@DataJpaTest` 어노테이션에 통계 활성화 속성을 추가해야 한다:

```java
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
@Import({TimeConfig.class, TimetableSetPersistenceAdapter.class})
class TimetableSetPersistenceAdapterDataJpaTest {
```

파일 맨 아래에 테스트 추가:

```java
    @Test
    void findAllDoesNotIssueOneQueryPerSetForClassrooms() {
        for (int i = 0; i < 3; i++) {
            TimetableSet set = TimetableSet.create(
                    "세트 " + i, LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16),
                    LocalTime.of(8, 30), LocalTime.of(22, 0), Set.of(DayOfWeek.MONDAY), 30,
                    List.of(new TimetableClassroom("6층", "601-" + i), new TimetableClassroom("5층", "501-" + i)));
            adapter.save(set);
        }
        entityManager.flush();
        entityManager.clear();

        org.hibernate.Session session = entityManager.unwrap(org.hibernate.Session.class);
        org.hibernate.stat.Statistics statistics = session.getSessionFactory().getStatistics();
        statistics.clear();

        List<TimetableSet> found = adapter.findAll();

        assertThat(found).hasSize(3);
        assertThat(statistics.getQueryExecutionCount()).isLessThanOrEqualTo(2);
    }
```

import 추가 필요: `import java.util.List;`(이미 있음), 나머지는 완전 정규화된 이름을 그대로 썼으므로 추가 import 불필요.

- [ ] **Step 2: 실패 확인**

Run: `./gradlew test --tests "com.academy.mudogroupware.timetable.infrastructure.persistence.TimetableSetPersistenceAdapterDataJpaTest.findAllDoesNotIssueOneQueryPerSetForClassrooms"`
Expected: FAIL — 지금은 세트 목록 쿼리 1번 + 세트당 classrooms 쿼리 3번 = 총 4번이라 `getQueryExecutionCount() <= 2`를 만족 못 한다.

- [ ] **Step 3: `TimetableSetEntity.classrooms`에 `@Fetch(FetchMode.SUBSELECT)` 추가**

```java
    @ElementCollection(fetch = FetchType.EAGER)
    @org.hibernate.annotations.Fetch(org.hibernate.annotations.FetchMode.SUBSELECT)
    @CollectionTable(name = "timetable_set_classroom", joinColumns = @JoinColumn(name = "timetable_set_id"))
    private List<TimetableClassroomEmbeddable> classrooms = new ArrayList<>();
```

(완전 정규화된 이름을 인라인으로 썼다 — import를 따로 추가하고 싶으면 `import org.hibernate.annotations.Fetch;`, `import org.hibernate.annotations.FetchMode;`를 추가하고 어노테이션을 `@Fetch(FetchMode.SUBSELECT)`로 짧게 써도 된다. 어느 쪽이든 동작은 같다.)

- [ ] **Step 4: 테스트 재실행 — GREEN 확인**

Run: `./gradlew test --tests "com.academy.mudogroupware.timetable.infrastructure.persistence.TimetableSetPersistenceAdapterDataJpaTest"`
Expected: 전부 PASS(새 테스트 포함, 기존 테스트도 회귀 없이 통과).

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/com/academy/mudogroupware/timetable/infrastructure/persistence/TimetableSetEntity.java \
        src/test/java/com/academy/mudogroupware/timetable/infrastructure/persistence/TimetableSetPersistenceAdapterDataJpaTest.java
git commit -m "perf: TimetableSetEntity.classrooms N+1 쿼리 제거(SUBSELECT 배치 로딩)"
```

---

### Task 7: 전체 검증

**Files:** 없음(검증 전용)

- [ ] **Step 1: 전체 테스트 실행**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL, 실패 0건(Docker 없는 환경이면 `CreateTimetableSlotConcurrencyTest`는 자동 스킵되고 나머지는 전부 통과해야 한다).

- [ ] **Step 2: Flyway 빈 DB 마이그레이션 재확인**

Run: `./gradlew test --tests "com.academy.mudogroupware.FlywayFreshDatabaseMigrationTest"`
Expected: PASS.

- [ ] **Step 3: 남은 커밋 없는지 확인**

```bash
git status --short
```

Expected: 빈 출력.
