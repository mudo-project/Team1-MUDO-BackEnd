# Lecture Timetable Field Alignment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 강의 등록 API와 lecture 저장 모델을 시간표 슬롯 기준 필드(`classType`, `classroomCode`, `teacherName`, `subjectName`)에 맞추고, 선택값은 null을 허용한다.

**Architecture:** `lecture` 도메인이 `timetable` 패키지를 직접 참조하지 않도록 lecture 전용 `ClassType` enum을 만든다. API 요청은 평평한 시간표 슬롯 형태를 받지만, 내부 일정 표현은 기존 `LectureSchedule` 목록을 유지해 학생 수강 조회와 기존 import 흐름을 보존한다.

**Tech Stack:** Java 17, Spring Boot, Spring MVC Bean Validation, Spring Data JPA, Flyway, JUnit 5, AssertJ, Mockito, Gradle.

## Global Constraints

- 답변과 작업 요약은 한글로 작성한다.
- GitHub 원격 저장소로 직접 push하지 않는다.
- 현재 작업과 무관한 file 모듈 변경과 `infra/scripts/__pycache__/`는 건드리지 않는다.
- `timetable` 도메인 코드는 수정하지 않는다.
- `lecture`는 `timetable.domain.model.ClassType`을 import하지 않고 자기 enum을 소유한다.
- production DB에 직접 쿼리하지 않는다. 마이그레이션 파일만 작성한다.
- 모든 구현 변경은 실패하는 테스트를 먼저 작성하고 확인한 뒤 적용한다.

---

## File Structure

- Create: `src/main/java/com/academy/mudogroupware/lecture/domain/model/ClassType.java`
  - lecture 전용 수업 종류 enum. 값은 timetable과 동일하게 유지한다.
- Modify: `src/main/java/com/academy/mudogroupware/lecture/presentation/api/request/CreateLectureRequest.java`
  - 신규 등록 API 입력을 시간표 슬롯 형태로 변경한다.
- Modify: `src/main/java/com/academy/mudogroupware/lecture/application/command/CreateLectureCommand.java`
  - 신규 입력과 기존 내부 호출을 모두 받을 수 있는 Command 계약을 제공한다.
- Modify: `src/main/java/com/academy/mudogroupware/lecture/domain/model/Lecture.java`
  - `classType`, `classroomCode`, `teacherName`, `subjectName`을 보관하고 기존 ID 필드를 nullable로 허용한다.
- Modify: `src/main/java/com/academy/mudogroupware/lecture/infrastructure/persistence/LectureEntity.java`
  - 새 DB 컬럼과 nullable 기존 컬럼을 JPA에 반영한다.
- Modify: `src/main/java/com/academy/mudogroupware/lecture/infrastructure/persistence/LectureJpaRepository.java`
  - 충돌 검사를 `classroomCode` 기준으로 변경한다.
- Modify: `src/main/java/com/academy/mudogroupware/lecture/infrastructure/persistence/LectureRepositoryImpl.java`
  - 새 필드 저장/복원과 새 충돌 검사 계약을 연결한다.
- Modify: `src/main/java/com/academy/mudogroupware/lecture/domain/repository/LectureRepository.java`
  - 충돌 검사 파라미터를 `Long classroomId`에서 `String classroomCode`로 변경한다.
- Modify: `src/main/java/com/academy/mudogroupware/lecture/application/service/CreateLectureService.java`
  - `teacherName` 중심으로 생성하고 `termName`/`subjectName`은 값이 있을 때만 마스터를 생성한다.
- Modify: `src/main/java/com/academy/mudogroupware/lecture/application/service/LectureQueryService.java`
  - 저장 문자열을 우선 사용하고 기존 ID 기반 조회는 fallback으로 유지한다.
- Modify: `src/main/java/com/academy/mudogroupware/lecture/application/query/LectureSummaryView.java`
- Modify: `src/main/java/com/academy/mudogroupware/lecture/application/query/LectureDetailView.java`
- Modify: `src/main/java/com/academy/mudogroupware/lecture/presentation/api/response/LectureSummaryResponse.java`
- Modify: `src/main/java/com/academy/mudogroupware/lecture/presentation/api/response/LectureDetailResponse.java`
  - `classType`, `classroomCode` 응답 필드를 추가한다.
- Modify: `src/main/java/com/academy/mudogroupware/lecture/infrastructure/persistence/LectureCatalogPortAdapter.java`
  - student 도메인에 내려주는 강사명을 저장된 `teacherName` 우선으로 바꾼다.
- Create: `src/main/resources/db/migration/be1/V1.5.15__align_lecture_with_timetable_fields.sql`
  - 새 컬럼 추가와 기존 컬럼 nullable 완화.
- Modify: `src/main/java/com/academy/mudogroupware/lecture/docs/API.md`
- Modify: `src/main/java/com/academy/mudogroupware/lecture/docs/README.md`
- Modify: `src/main/java/com/academy/mudogroupware/lecture/docs/CHANGELOG.md`
  - 등록 요청과 nullable 정책 문서화.
- Create: `src/test/java/com/academy/mudogroupware/lecture/presentation/api/request/CreateLectureRequestTest.java`
- Modify: `src/test/java/com/academy/mudogroupware/lecture/domain/model/LectureTest.java`
- Modify: `src/test/java/com/academy/mudogroupware/lecture/application/service/CreateLectureServiceTest.java`
- Modify: `src/test/java/com/academy/mudogroupware/lecture/application/service/LectureQueryServiceTest.java`
- Create: `src/test/java/com/academy/mudogroupware/lecture/infrastructure/persistence/LectureRepositoryImplDataJpaTest.java`
- Modify: `src/test/java/com/academy/mudogroupware/lecture/infrastructure/persistence/LectureCatalogPortAdapterTest.java`
- Verify existing compile-only call sites through these tests after adding compatibility overloads: `LectureEnrollmentPortAdapterTest`, `LectureCatalogPortAdapterTest`, `ConfirmOnboardingImportServiceTest`.

---

### Task 1: API Request And Command Contract

**Files:**
- Create: `src/test/java/com/academy/mudogroupware/lecture/presentation/api/request/CreateLectureRequestTest.java`
- Create: `src/main/java/com/academy/mudogroupware/lecture/domain/model/ClassType.java`
- Modify: `src/main/java/com/academy/mudogroupware/lecture/presentation/api/request/CreateLectureRequest.java`
- Modify: `src/main/java/com/academy/mudogroupware/lecture/application/command/CreateLectureCommand.java`

**Interfaces:**
- Produces: `CreateLectureRequest.toCommand(Long requesterId)` returns `CreateLectureCommand`.
- Produces: `CreateLectureCommand(String name, ClassType classType, String classroomCode, Grade grade, String teacherName, String subjectName, String termName, FeeType feeType, Integer feeAmount, List<ScheduleInput> schedules, Long requesterId, Long teacherId)`.
- Produces legacy constructor: `CreateLectureCommand(String name, Grade grade, String termName, String subjectName, Long teacherId, String classroomName, FeeType feeType, Integer feeAmount, List<ScheduleInput> schedules, Long requesterId)`.

- [ ] **Step 1: Write failing request validation tests**

```java
package com.academy.mudogroupware.lecture.presentation.api.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.lecture.application.command.ScheduleInput;
import com.academy.mudogroupware.lecture.domain.model.ClassType;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class CreateLectureRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsTimetableSlotShapeWithOnlyRequiredFields() {
        CreateLectureRequest request = new CreateLectureRequest(
                "고1 수학 정규반",
                ClassType.CLASS,
                DayOfWeek.MONDAY,
                "601",
                LocalTime.of(19, 0),
                LocalTime.of(21, 0),
                null,
                null,
                null,
                null,
                null,
                null);

        assertThat(validator.validate(request)).isEmpty();
        assertThat(request.toCommand(99L).classType()).isEqualTo(ClassType.CLASS);
        assertThat(request.toCommand(99L).classroomCode()).isEqualTo("601");
        assertThat(request.toCommand(99L).teacherId()).isNull();
        assertThat(request.toCommand(99L).teacherName()).isNull();
        assertThat(request.toCommand(99L).schedules())
                .containsExactly(new ScheduleInput(DayOfWeek.MONDAY, LocalTime.of(19, 0), LocalTime.of(21, 0)));
    }

    @Test
    void rejectsMissingRequiredTimetableSlotFields() {
        CreateLectureRequest request = new CreateLectureRequest(
                "고1 수학 정규반",
                null,
                null,
                " ",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        assertThat(validator.validate(request)).hasSizeGreaterThanOrEqualTo(5);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew test --tests "com.academy.mudogroupware.lecture.presentation.api.request.CreateLectureRequestTest"`

Expected: FAIL at compile time because `lecture.domain.model.ClassType` and the new `CreateLectureRequest` constructor do not exist yet.

- [ ] **Step 3: Add lecture ClassType enum**

```java
package com.academy.mudogroupware.lecture.domain.model;

public enum ClassType {
    CLASS,
    SPECIAL,
    CLINIC,
    STANDING,
    EXAM
}
```

- [ ] **Step 4: Update CreateLectureRequest**

```java
public record CreateLectureRequest(
        @NotBlank String name,
        @NotNull ClassType classType,
        @NotNull DayOfWeek dayOfWeek,
        @NotBlank String classroomCode,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        Grade grade,
        String teacherName,
        String subjectName,
        String termName,
        FeeType feeType,
        Integer feeAmount
) {

    public CreateLectureCommand toCommand(Long requesterId) {
        List<ScheduleInput> inputs = List.of(new ScheduleInput(dayOfWeek, startTime, endTime));
        return new CreateLectureCommand(name, classType, classroomCode, grade, teacherName, subjectName,
                termName, feeType, feeAmount, inputs, requesterId, null);
    }
}
```

- [ ] **Step 5: Update CreateLectureCommand while preserving legacy constructor**

```java
public record CreateLectureCommand(
        String name,
        ClassType classType,
        String classroomCode,
        Grade grade,
        String teacherName,
        String subjectName,
        String termName,
        FeeType feeType,
        Integer feeAmount,
        List<ScheduleInput> schedules,
        Long requesterId,
        Long teacherId
) {

    public CreateLectureCommand(String name, Grade grade, String termName, String subjectName, Long teacherId,
                                String classroomName, FeeType feeType, Integer feeAmount,
                                List<ScheduleInput> schedules, Long requesterId) {
        this(name, ClassType.CLASS, classroomName, grade, null, subjectName, termName,
                feeType, feeAmount, schedules, requesterId, teacherId);
    }
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `.\gradlew test --tests "com.academy.mudogroupware.lecture.presentation.api.request.CreateLectureRequestTest"`

Expected: PASS.

---

### Task 2: Lecture Domain Model Alignment

**Files:**
- Modify: `src/test/java/com/academy/mudogroupware/lecture/domain/model/LectureTest.java`
- Modify: `src/main/java/com/academy/mudogroupware/lecture/domain/model/Lecture.java`

**Interfaces:**
- Consumes: `lecture.domain.model.ClassType`.
- Produces new create method:
  `Lecture.create(String name, ClassType classType, String classroomCode, Grade grade, Long termId, Long subjectId, Long teacherId, Long classroomId, String teacherName, String subjectName, FeeType feeType, Integer feeAmount, List<LectureSchedule> schedules, LocalDateTime now)`.
- Produces getters: `getClassType()`, `getClassroomCode()`, `getTeacherName()`, `getSubjectName()`.
- Keeps legacy `create` and `restore` overloads for existing tests and adapters.

- [ ] **Step 1: Write failing domain tests**

Add to `LectureTest`:

```java
@Test
void createsLectureWithTimetableFieldsAndNullableOptionalFields() {
    Lecture lecture = Lecture.create(
            "고1 수학 정규반",
            ClassType.CLASS,
            "601",
            null,
            null,
            null,
            null,
            null,
            "김선생",
            null,
            null,
            null,
            List.of(schedule(DayOfWeek.MONDAY, 19, 21)),
            NOW);

    assertThat(lecture.getClassType()).isEqualTo(ClassType.CLASS);
    assertThat(lecture.getClassroomCode()).isEqualTo("601");
    assertThat(lecture.getGrade()).isNull();
    assertThat(lecture.getTeacherId()).isNull();
    assertThat(lecture.getTeacherName()).isEqualTo("김선생");
    assertThat(lecture.getSubjectName()).isNull();
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew test --tests "com.academy.mudogroupware.lecture.domain.model.LectureTest"`

Expected: FAIL at compile time because the new `Lecture.create` signature and getters do not exist.

- [ ] **Step 3: Update Lecture fields and creation validation**

Implement these fields:

```java
private final ClassType classType;
private final String classroomCode;
private final String teacherName;
private final String subjectName;
```

Implement new create method with validation:

```java
public static Lecture create(String name, ClassType classType, String classroomCode, Grade grade,
                              Long termId, Long subjectId, Long teacherId, Long classroomId,
                              String teacherName, String subjectName, FeeType feeType, Integer feeAmount,
                              List<LectureSchedule> schedules, LocalDateTime now) {
    if (classType == null) {
        throw new IllegalArgumentException("classType must not be null");
    }
    if (classroomCode == null || classroomCode.isBlank()) {
        throw new IllegalArgumentException("classroomCode must not be blank");
    }
    return new Lecture(null, name, classType, classroomCode, grade, termId, subjectId, teacherId, classroomId,
            teacherName, subjectName, feeType, feeAmount, schedules, now);
}
```

Keep the old `create(String name, Grade grade, Long termId, Long subjectId, Long teacherId, Long classroomId, ...)` overload and map it to `ClassType.CLASS` with `classroomCode = classroomId != null ? String.valueOf(classroomId) : null` only for legacy callers.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew test --tests "com.academy.mudogroupware.lecture.domain.model.LectureTest"`

Expected: PASS.

---

### Task 3: Persistence And Migration

**Files:**
- Create: `src/test/java/com/academy/mudogroupware/lecture/infrastructure/persistence/LectureRepositoryImplDataJpaTest.java`
- Create: `src/main/resources/db/migration/be1/V1.5.15__align_lecture_with_timetable_fields.sql`
- Modify: `src/main/java/com/academy/mudogroupware/lecture/infrastructure/persistence/LectureEntity.java`
- Modify: `src/main/java/com/academy/mudogroupware/lecture/infrastructure/persistence/LectureRepositoryImpl.java`
- Modify: `src/main/java/com/academy/mudogroupware/lecture/infrastructure/persistence/LectureJpaRepository.java`
- Modify: `src/main/java/com/academy/mudogroupware/lecture/domain/repository/LectureRepository.java`

**Interfaces:**
- Produces repository method: `boolean existsOverlap(String classroomCode, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime)`.

- [ ] **Step 1: Write failing DataJpa test**

```java
package com.academy.mudogroupware.lecture.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.academy.mudogroupware.global.infrastructure.config.TimeConfig;
import com.academy.mudogroupware.lecture.domain.model.ClassType;
import com.academy.mudogroupware.lecture.domain.model.Lecture;
import com.academy.mudogroupware.lecture.domain.model.LectureSchedule;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({TimeConfig.class, LectureRepositoryImpl.class})
class LectureRepositoryImplDataJpaTest {

    @Autowired
    private LectureRepositoryImpl repository;

    @Test
    void savesAndRestoresTimetableAlignedFields() {
        Lecture lecture = Lecture.create(
                "고1 수학 정규반",
                ClassType.CLASS,
                "601",
                null,
                null,
                null,
                null,
                null,
                "김선생",
                "수학",
                null,
                null,
                List.of(LectureSchedule.create(DayOfWeek.MONDAY, LocalTime.of(19, 0), LocalTime.of(21, 0))),
                LocalDateTime.of(2026, 8, 11, 9, 0));

        Lecture saved = repository.save(lecture);
        Lecture found = repository.findById(saved.getId()).orElseThrow();

        assertThat(found.getClassType()).isEqualTo(ClassType.CLASS);
        assertThat(found.getClassroomCode()).isEqualTo("601");
        assertThat(found.getTeacherName()).isEqualTo("김선생");
        assertThat(found.getSubjectName()).isEqualTo("수학");
        assertThat(found.getTeacherId()).isNull();
    }

    @Test
    void detectsOverlapByClassroomCode() {
        Lecture lecture = Lecture.create(
                "고1 수학 정규반",
                ClassType.CLASS,
                "601",
                null,
                null,
                null,
                null,
                null,
                "김선생",
                "수학",
                null,
                null,
                List.of(LectureSchedule.create(DayOfWeek.MONDAY, LocalTime.of(19, 0), LocalTime.of(21, 0))),
                LocalDateTime.of(2026, 8, 11, 9, 0));
        repository.save(lecture);

        assertThat(repository.existsOverlap("601", DayOfWeek.MONDAY, LocalTime.of(20, 0), LocalTime.of(22, 0)))
                .isTrue();
        assertThat(repository.existsOverlap("602", DayOfWeek.MONDAY, LocalTime.of(20, 0), LocalTime.of(22, 0)))
                .isFalse();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew test --tests "com.academy.mudogroupware.lecture.infrastructure.persistence.LectureRepositoryImplDataJpaTest"`

Expected: FAIL because entity/repository mappings and `existsOverlap(String, ...)` are not implemented.

- [ ] **Step 3: Add Flyway migration**

Create `src/main/resources/db/migration/be1/V1.5.15__align_lecture_with_timetable_fields.sql`:

```sql
ALTER TABLE lecture
  ADD COLUMN class_type VARCHAR(20) NULL AFTER name,
  ADD COLUMN subject_name VARCHAR(50) NULL AFTER subject_id,
  ADD COLUMN teacher_name VARCHAR(50) NULL AFTER teacher_id,
  ADD COLUMN classroom_code VARCHAR(50) NULL AFTER classroom_id,
  MODIFY grade VARCHAR(20) NULL,
  MODIFY term_id BIGINT NULL,
  MODIFY subject_id BIGINT NULL,
  MODIFY teacher_id BIGINT NULL,
  MODIFY classroom_id BIGINT NULL;
```

- [ ] **Step 4: Update JPA entity and repository mapping**

Add fields to `LectureEntity`:

```java
@Enumerated(EnumType.STRING)
@Column(name = "class_type", length = 20)
private ClassType classType;

@Column(name = "subject_name", length = 50)
private String subjectName;

@Column(name = "teacher_name", length = 50)
private String teacherName;

@Column(name = "classroom_code", length = 50)
private String classroomCode;
```

Change existing `@Column(nullable = false)` on `grade`, `termId`, `subjectId`, `teacherId`, `classroomId` to nullable.

Update `LectureRepositoryImpl.save(...)` and `toDomain(...)` to map all new fields.

Update `LectureJpaRepository.existsOverlap(...)` query:

```java
@Query("select count(s) > 0 from LectureScheduleEntity s "
        + "where s.lecture.classroomCode = :classroomCode and s.dayOfWeek = :dayOfWeek "
        + "and s.startTime < :endTime and :startTime < s.endTime")
boolean existsOverlap(@Param("classroomCode") String classroomCode,
                      @Param("dayOfWeek") DayOfWeek dayOfWeek,
                      @Param("startTime") LocalTime startTime,
                      @Param("endTime") LocalTime endTime);
```

- [ ] **Step 5: Run test to verify it passes**

Run: `.\gradlew test --tests "com.academy.mudogroupware.lecture.infrastructure.persistence.LectureRepositoryImplDataJpaTest"`

Expected: PASS.

---

### Task 4: Lecture Creation Service

**Files:**
- Modify: `src/test/java/com/academy/mudogroupware/lecture/application/service/CreateLectureServiceTest.java`
- Modify: `src/main/java/com/academy/mudogroupware/lecture/application/service/CreateLectureService.java`

**Interfaces:**
- Consumes: new `CreateLectureCommand` fields.
- Produces: new lectures saved with `classType`, `classroomCode`, `teacherName`, `subjectName`.

- [ ] **Step 1: Write failing service tests**

Add to `CreateLectureServiceTest`:

```java
@Test
void createsLectureFromTeacherNameCenteredCommand() {
    CreateLectureCommand command = new CreateLectureCommand(
            "고1 수학 정규반",
            ClassType.CLASS,
            "601",
            Grade.HIGH_1,
            "김선생",
            "수학",
            null,
            FeeType.PER_MONTH,
            300000,
            List.of(new ScheduleInput(DayOfWeek.MONDAY, LocalTime.of(19, 0), LocalTime.of(21, 0))),
            99L,
            null);
    when(subjectRepository.findByName("수학")).thenReturn(Optional.empty());
    when(subjectRepository.save(any(Subject.class))).thenReturn(Subject.restore(20L, "수학", NOW));
    when(classroomRepository.findByName("601")).thenReturn(Optional.empty());
    when(classroomRepository.save(any(Classroom.class))).thenReturn(Classroom.restore(40L, "601", NOW));
    when(lectureRepository.existsOverlap("601", DayOfWeek.MONDAY, LocalTime.of(19, 0), LocalTime.of(21, 0)))
            .thenReturn(false);
    when(lectureRepository.save(any(Lecture.class))).thenAnswer(invocation -> invocation.getArgument(0));

    service.createLecture(command);

    verify(termRepository, never()).findByName(any());
    verify(lectureRepository).save(any(Lecture.class));
}

@Test
void allowsNullableOptionalLectureFields() {
    CreateLectureCommand command = new CreateLectureCommand(
            "고1 정규반",
            ClassType.CLASS,
            "601",
            null,
            null,
            null,
            null,
            null,
            null,
            List.of(new ScheduleInput(DayOfWeek.MONDAY, LocalTime.of(19, 0), LocalTime.of(21, 0))),
            99L,
            null);
    when(classroomRepository.findByName("601")).thenReturn(Optional.of(Classroom.restore(40L, "601", NOW)));
    when(lectureRepository.existsOverlap("601", DayOfWeek.MONDAY, LocalTime.of(19, 0), LocalTime.of(21, 0)))
            .thenReturn(false);
    when(lectureRepository.save(any(Lecture.class))).thenAnswer(invocation -> invocation.getArgument(0));

    service.createLecture(command);

    verify(termRepository, never()).findByName(any());
    verify(subjectRepository, never()).findByName(any());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew test --tests "com.academy.mudogroupware.lecture.application.service.CreateLectureServiceTest"`

Expected: FAIL because service still expects `teacherId`, required term/subject/classroom fields, and classroom ID conflict checks.

- [ ] **Step 3: Update CreateLectureService**

Rules:

```java
Long termId = hasText(command.termName()) ? findOrCreateTerm(command.termName(), now) : null;
Long subjectId = hasText(command.subjectName()) ? findOrCreateSubject(command.subjectName(), now) : null;
Long classroomId = findOrCreateClassroom(command.classroomCode(), now);
```

Conflict check:

```java
if (lectureRepository.existsOverlap(command.classroomCode(), schedule.getDayOfWeek(),
        schedule.getStartTime(), schedule.getEndTime())) {
    throw new ClassroomTimeConflictException();
}
```

Lecture creation:

```java
Lecture lecture = Lecture.create(command.name(), command.classType(), command.classroomCode(), command.grade(),
        termId, subjectId, command.teacherId(), classroomId, command.teacherName(), command.subjectName(),
        command.feeType(), command.feeAmount(), schedules, now);
```

- [ ] **Step 4: Run service tests**

Run: `.\gradlew test --tests "com.academy.mudogroupware.lecture.application.service.CreateLectureServiceTest"`

Expected: PASS.

---

### Task 5: Query Responses And Student Catalog Fallback

**Files:**
- Modify: `src/test/java/com/academy/mudogroupware/lecture/application/service/LectureQueryServiceTest.java`
- Modify: `src/test/java/com/academy/mudogroupware/lecture/infrastructure/persistence/LectureCatalogPortAdapterTest.java`
- Modify: `src/main/java/com/academy/mudogroupware/lecture/application/query/LectureSummaryView.java`
- Modify: `src/main/java/com/academy/mudogroupware/lecture/application/query/LectureDetailView.java`
- Modify: `src/main/java/com/academy/mudogroupware/lecture/presentation/api/response/LectureSummaryResponse.java`
- Modify: `src/main/java/com/academy/mudogroupware/lecture/presentation/api/response/LectureDetailResponse.java`
- Modify: `src/main/java/com/academy/mudogroupware/lecture/application/service/LectureQueryService.java`
- Modify: `src/main/java/com/academy/mudogroupware/lecture/infrastructure/persistence/LectureCatalogPortAdapter.java`

**Interfaces:**
- Produces view/response fields: `classType`, `classroomCode`.
- Keeps response fields: `teacherId`, `teacherName`, `classroomName`, `subjectName`.

- [ ] **Step 1: Write failing query tests**

Add to `LectureQueryServiceTest`:

```java
@Test
void returnsStoredTimetableFieldsBeforeLegacyLookupValues() {
    LectureSchedule schedule = LectureSchedule.create(DayOfWeek.MONDAY, LocalTime.of(19, 0), LocalTime.of(21, 0));
    Lecture lecture = Lecture.restore(1L, "Math Basics", ClassType.CLASS, "601", Grade.HIGH_1,
            null, null, null, null, "Stored Teacher", "Stored Subject", FeeType.PER_MONTH, 300000,
            List.of(schedule), NOW);
    when(lectureRepository.findById(1L)).thenReturn(Optional.of(lecture));
    when(enrolledStudentsPort.findByLectureId(1L)).thenReturn(List.of());

    LectureDetailView view = service.getLectureDetail(1L);

    assertThat(view.classType()).isEqualTo(ClassType.CLASS);
    assertThat(view.classroomCode()).isEqualTo("601");
    assertThat(view.classroomName()).isEqualTo("601");
    assertThat(view.teacherName()).isEqualTo("Stored Teacher");
    assertThat(view.subjectName()).isEqualTo("Stored Subject");
}
```

Add to `LectureCatalogPortAdapterTest`:

```java
@Test
void usesStoredTeacherNameBeforeUsersDirectoryFallback() {
    Lecture lecture = Lecture.restore(100L, "Math", ClassType.CLASS, "601", Grade.HIGH_1,
            null, null, null, null, "Stored Teacher", "Math", FeeType.PER_MONTH, 300000,
            List.of(LectureSchedule.create(DayOfWeek.MONDAY, LocalTime.of(19, 0), LocalTime.of(21, 0))),
            NOW);
    when(lectureRepository.findAllById(List.of(100L))).thenReturn(List.of(lecture));

    Map<Long, LectureCatalogInfo> result = adapter.findByIds(List.of(100L));

    assertThat(result.get(100L).teacherName()).isEqualTo("Stored Teacher");
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
.\gradlew test --tests "com.academy.mudogroupware.lecture.application.service.LectureQueryServiceTest" --tests "com.academy.mudogroupware.lecture.infrastructure.persistence.LectureCatalogPortAdapterTest"
```

Expected: FAIL because new restore signature and view fields do not exist, and lookup still depends on `teacherId`.

- [ ] **Step 3: Update views and responses**

Add fields to summary and detail views/responses:

```java
ClassType classType,
String classroomCode,
```

Place them near `grade` and `classroomName` in record constructors so list/detail response shape stays readable.

- [ ] **Step 4: Update LectureQueryService fallback logic**

Add null-safe ID helper:

```java
private List<Long> distinctNonNullIds(List<Lecture> lectures, Function<Lecture, Long> idExtractor) {
    return lectures.stream().map(idExtractor).filter(Objects::nonNull).distinct().toList();
}
```

Use stored values first:

```java
private String teacherName(Lecture lecture, Map<Long, TeacherInfo> teachers) {
    if (lecture.getTeacherName() != null && !lecture.getTeacherName().isBlank()) {
        return lecture.getTeacherName();
    }
    TeacherInfo teacher = lecture.getTeacherId() != null ? teachers.get(lecture.getTeacherId()) : null;
    return teacher != null ? teacher.name() : null;
}

private String subjectName(Lecture lecture, Map<Long, String> subjectNames) {
    return lecture.getSubjectName() != null && !lecture.getSubjectName().isBlank()
            ? lecture.getSubjectName()
            : subjectNames.get(lecture.getSubjectId());
}

private String classroomName(Lecture lecture, Map<Long, String> classroomNames) {
    String legacyName = classroomNames.get(lecture.getClassroomId());
    return legacyName != null ? legacyName : lecture.getClassroomCode();
}
```

- [ ] **Step 5: Update LectureCatalogPortAdapter fallback logic**

Use stored `lecture.getTeacherName()` first. Only call `TeacherDirectoryPort` for non-null legacy `teacherId` values when at least one lecture lacks stored `teacherName`.

- [ ] **Step 6: Run tests**

Run:

```powershell
.\gradlew test --tests "com.academy.mudogroupware.lecture.application.service.LectureQueryServiceTest" --tests "com.academy.mudogroupware.lecture.infrastructure.persistence.LectureCatalogPortAdapterTest"
```

Expected: PASS.

---

### Task 6: Docs And Full Verification

**Files:**
- Modify: `src/main/java/com/academy/mudogroupware/lecture/docs/API.md`
- Modify: `src/main/java/com/academy/mudogroupware/lecture/docs/README.md`
- Modify: `src/main/java/com/academy/mudogroupware/lecture/docs/CHANGELOG.md`
- Verify: `src/test/java/com/academy/mudogroupware/FlywayFreshDatabaseMigrationTest.java`

**Interfaces:**
- Consumes all previous tasks.
- Produces updated developer-facing lecture docs.

- [ ] **Step 1: Update docs**

In `lecture/docs/API.md`, replace the create request example with:

```json
{
  "name": "고1 수학 정규반",
  "classType": "CLASS",
  "dayOfWeek": "MONDAY",
  "classroomCode": "601",
  "startTime": "19:00:00",
  "endTime": "21:00:00",
  "grade": "HIGH_1",
  "teacherName": "김선생",
  "subjectName": "수학",
  "termName": "2026 1학기",
  "feeType": "PER_MONTH",
  "feeAmount": 300000
}
```

Add to docs:

- `teacherId` is no longer part of the lecture create request.
- `grade`, `teacherName`, `subjectName`, `termName`, `feeType`, `feeAmount` are optional.
- list/detail include `classType` and `classroomCode`.

- [ ] **Step 2: Run focused lecture tests**

Run:

```powershell
.\gradlew test --tests "com.academy.mudogroupware.lecture.*"
```

Expected: PASS.

- [ ] **Step 3: Run data import compile-sensitive tests**

Run:

```powershell
.\gradlew test --tests "com.academy.mudogroupware.dataimport.application.service.ConfirmOnboardingImportServiceTest"
```

Expected: PASS. This proves the legacy `CreateLectureCommand` constructor still protects the data import flow.

- [ ] **Step 4: Run migration verification**

Run:

```powershell
.\gradlew test --tests "com.academy.mudogroupware.FlywayFreshDatabaseMigrationTest"
```

Expected: PASS when Docker/Testcontainers is available. If Docker is unavailable, record the skip or environment failure explicitly.

- [ ] **Step 5: Run compile check**

Run: `.\gradlew compileJava`

Expected: PASS.

- [ ] **Step 6: Final status check**

Run: `git status --short`

Expected: only files from this plan plus pre-existing unrelated file module changes are present. Stage or commit only the lecture/docs/migration/test files touched by this work.

---

## Self-Review

- Spec coverage: the plan covers request shape, nullable optional fields, `teacherName`-centered creation, new lecture storage columns, existing data/import/student fallback, docs, and migration verification.
- Placeholder scan: no implementation placeholders remain.
- Type consistency: `ClassType` is consistently `com.academy.mudogroupware.lecture.domain.model.ClassType`, not the timetable enum.
- Scope check: `timetable` code remains untouched; `dataimport` is protected through the legacy command constructor instead of direct modification.
