# 시간표 슬롯 색상 저장 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 수업 슬롯(`timetable_slot`) 생성/수정 시점에 `color`(6자리 hex 문자열, 필수)를 받아 저장하고, 내보내기(export)는 그 저장된 색을 그대로 사용하도록 바꾼다. `colorCriterion`/`colorMap`(export 쿼리 파라미터)은 완전히 제거한다.

**Architecture:** `TimetableSlot` 도메인 모델에 `color` 필드를 추가하고 `create()`/`applyFullUpdate()`에서 6자리 hex 정규식으로 검증한다. 이 값이 엔티티 → 영속성 → Command/Request → Service → View/Response 전 계층을 관통해 흐른다. Export 쪽은 `TimetableExportOptions`에서 `colorCriterion`/`colorsByGroupValue`를 제거하고 렌더러가 `TimetableExportColor.fromHex(slot.color())`를 직접 호출하는 구조로 단순화한다.

**Tech Stack:** Spring Boot 3.5 / Java 17, JPA, Flyway(MySQL), JUnit5 + Mockito + AssertJ, MockMvc, Testcontainers.

**설계 문서:** `docs/superpowers/specs/2026-08-13-timetable-slot-color-design.md`

---

## 작업 순서에 대한 중요 안내

`TimetableSlot.create()`/`restore()`/`applyFullUpdate()`의 파라미터 목록이 바뀌기 때문에, **Task 3부터 Task 8까지는 이 프로젝트(단일 Gradle 모듈) 전체가 컴파일되지 않는 중간 상태를 거친다.** 이건 정상이다 — Java는 부분 컴파일을 지원하지 않으므로, 시그니처를 쓰는 모든 호출부(서비스 2개, 영속성 어댑터, 조회 서비스 2개, 그리고 그 테스트들)를 순서대로 고쳐나가야 전체가 다시 컴파일된다. **Task 8을 마치기 전까지는 `./gradlew test` 전체 실행이 실패하는 게 정상이며, 각 태스크는 자기 파일의 의도를 검증하는 데 집중한다.** Task 8 끝에서 처음으로 전체 스위트를 돌려 확인한다. Export 쪽(Task 9~12)도 같은 이유로 Task 12까지 묶어서 간다.

---

### Task 1: 마이그레이션 — `timetable_slot.color` 컬럼 추가

**Files:**
- Create: `src/main/resources/db/migration/be5/V5.1.12__add_color_to_timetable_slot.sql`

- [ ] **Step 1: 마이그레이션 파일 작성**

```sql
ALTER TABLE timetable_slot ADD COLUMN color VARCHAR(6) NOT NULL DEFAULT 'FFFFFF';
ALTER TABLE timetable_slot ALTER COLUMN color DROP DEFAULT;
```

- [ ] **Step 2: 빈 DB 기준 전체 마이그레이션 검증**

Run: `./gradlew test --tests "com.academy.mudogroupware.FlywayFreshDatabaseMigrationTest"`
Expected: PASS (`allMigrationsApplySuccessfullyFromEmptySchema`).이 테스트는 새 마이그레이션 파일을 자동으로 포함하므로 별도 수정이 필요 없다. Docker가 없는 환경이면 `disabledWithoutDocker = true`라 스킵되니, 그 경우 파일 문법만 육안으로 재확인한다.

- [ ] **Step 3: 커밋**

```bash
git add src/main/resources/db/migration/be5/V5.1.12__add_color_to_timetable_slot.sql
git commit -m "feat: timetable_slot에 color 컬럼 추가"
```

---

### Task 2: `InvalidExportColorException` → `InvalidTimetableColorException` 이름 정리

**Files:**
- Modify: `src/main/java/com/academy/mudogroupware/timetable/domain/exception/TimetableErrorCode.java`
- Delete: `src/main/java/com/academy/mudogroupware/timetable/domain/exception/InvalidExportColorException.java`
- Create: `src/main/java/com/academy/mudogroupware/timetable/domain/exception/InvalidTimetableColorException.java`
- Modify: `src/main/java/com/academy/mudogroupware/timetable/domain/model/TimetableExportColor.java`
- Modify: `src/test/java/com/academy/mudogroupware/timetable/application/service/ExportTimetableServiceTest.java`
- Modify: `src/test/java/com/academy/mudogroupware/timetable/presentation/api/TimetableControllerTest.java`

이 예외는 지금 `TimetableExportColor.fromHex()`가 던진다(export의 `colorMap` 값 검증용). 이번 태스크에서는 **이름과 메시지만 바꾸고 동작은 그대로 둔다** — export의 `colorMap`이 실제로 없어지는 건 Task 9~12에서 다룬다.

- [ ] **Step 1: `TimetableErrorCode`에서 항목 이름/메시지 변경**

`INVALID_EXPORT_COLOR` 줄을 찾아서:

```java
INVALID_EXPORT_COLOR(HttpStatus.BAD_REQUEST, "TIMETABLE_400_5", "내보내기 색상 값은 6자리 16진수(RRGGBB)여야 합니다."),
```

다음으로 바꾼다:

```java
INVALID_COLOR(HttpStatus.BAD_REQUEST, "TIMETABLE_400_5", "색상 값은 6자리 16진수(RRGGBB)여야 합니다."),
```

- [ ] **Step 2: 새 예외 클래스 생성, 기존 파일 삭제**

`InvalidTimetableColorException.java` 새로 작성:

```java
package com.academy.mudogroupware.timetable.domain.exception;

public class InvalidTimetableColorException extends com.academy.mudogroupware.global.domain.common.exception.BadRequestException {

    public InvalidTimetableColorException() {
        super(TimetableErrorCode.INVALID_COLOR);
    }
}
```

`InvalidExportColorException.java`는 삭제한다. (기존에 있던 `Throwable cause`를 받는 생성자는 export의 JSON 파싱 실패를 감싸던 용도였는데, 그 경로 자체가 Task 9~12에서 사라지므로 가져오지 않는다 — YAGNI.)

- [ ] **Step 3: `TimetableExportColor`가 새 예외를 던지도록 수정**

`fromHex` 메서드 안의:

```java
throw new InvalidExportColorException();
```

를 다음으로 바꾸고, import도 `InvalidTimetableColorException`으로 교체한다:

```java
throw new InvalidTimetableColorException();
```

- [ ] **Step 4: 기존 테스트의 참조만 우선 교체(동작 검증은 Task 9~12에서)**

`ExportTimetableServiceTest.java`: `import com.academy.mudogroupware.timetable.domain.exception.InvalidExportColorException;`을 `InvalidTimetableColorException`으로, `exportThrowsWhenColorIsNotValidHex` 테스트 안의 `.isInstanceOf(InvalidExportColorException.class)`도 `InvalidTimetableColorException.class`로 바꾼다.

`TimetableControllerTest.java`: `import ... InvalidExportColorException;`을 `InvalidTimetableColorException`으로, `exportTimetableReturns400WhenColorInvalid` 테스트 안의 `new InvalidExportColorException()`도 `new InvalidTimetableColorException()`으로 바꾼다.

- [ ] **Step 5: 컴파일 확인**

Run: `./gradlew compileTestJava`
Expected: BUILD SUCCESSFUL (이 태스크는 이름 변경뿐이라 전체 컴파일이 깨지지 않는다).

- [ ] **Step 6: 커밋**

```bash
git add -A
git commit -m "refactor: InvalidExportColorException을 InvalidTimetableColorException으로 이름 정리"
```

---

### Task 3: `TimetableSlot` 도메인 모델 — `color` 필드 + 검증 (TDD)

**Files:**
- Modify: `src/main/java/com/academy/mudogroupware/timetable/domain/model/TimetableSlot.java`
- Modify: `src/test/java/com/academy/mudogroupware/timetable/domain/model/TimetableSlotTest.java`

- [ ] **Step 1: 실패하는 테스트 작성**

`TimetableSlotTest.java`의 `slot(...)` 헬퍼와 기존 테스트 전부가 새 시그니처를 쓰도록 아래처럼 통째로 바꾼다(기존 테스트도 새 파라미터 없이는 컴파일이 안 되므로 같이 손댄다):

```java
package com.academy.mudogroupware.timetable.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.timetable.domain.exception.InvalidTimetableClassroomException;
import com.academy.mudogroupware.timetable.domain.exception.InvalidTimetableColorException;
import com.academy.mudogroupware.timetable.domain.exception.InvalidTimetableSlotException;

class TimetableSlotTest {

    private static final LocalDate FROM = LocalDate.of(2026, 7, 20);
    private static final LocalDate UNTIL = LocalDate.of(2026, 8, 16);

    private TimetableSlot slot(String classroomCode, DayOfWeek day, LocalTime start, LocalTime end) {
        return TimetableSlot.create(
                1L, ClassType.CLASS, day, classroomCode, start, end, Grade.HIGH_3, "정T", "미적분",
                "FFCC00", FROM, UNTIL);
    }

    @Test
    void createBuildsSlotWithDefaultEffectiveRange() {
        TimetableSlot slot = slot("601", DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0));

        assertThat(slot.getClassroomCode()).isEqualTo("601");
        assertThat(slot.getColor()).isEqualTo("FFCC00");
        assertThat(slot.getEffectiveFrom()).isEqualTo(FROM);
        assertThat(slot.getEffectiveUntil()).isEqualTo(UNTIL);
    }

    @Test
    void overlapsReturnsTrueForSameClassroomDayAndOverlappingTime() {
        TimetableSlot a = slot("601", DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0));
        TimetableSlot b = slot("601", DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(12, 0));

        assertThat(a.overlaps(b)).isTrue();
    }

    @Test
    void overlapsReturnsFalseForDifferentClassroom() {
        TimetableSlot a = slot("601", DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0));
        TimetableSlot b = slot("602", DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0));

        assertThat(a.overlaps(b)).isFalse();
    }

    @Test
    void overlapsReturnsFalseForDifferentDay() {
        TimetableSlot a = slot("601", DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0));
        TimetableSlot b = slot("601", DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(11, 0));

        assertThat(a.overlaps(b)).isFalse();
    }

    @Test
    void overlapsReturnsFalseForNonOverlappingTime() {
        TimetableSlot a = slot("601", DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0));
        TimetableSlot b = slot("601", DayOfWeek.MONDAY, LocalTime.of(11, 0), LocalTime.of(12, 0));

        assertThat(a.overlaps(b)).isFalse();
    }

    @Test
    void overlapsReturnsFalseWhenEffectiveRangesDoNotOverlap() {
        TimetableSlot a = TimetableSlot.create(
                1L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "미적분", "FFCC00", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 31));
        TimetableSlot b = TimetableSlot.create(
                1L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "미적분", "FFCC00", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 16));

        assertThat(a.overlaps(b)).isFalse();
    }

    @Test
    void closeEffectiveUntilTruncatesRange() {
        TimetableSlot slot = slot("601", DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0));

        slot.closeEffectiveUntil(LocalDate.of(2026, 8, 1));

        assertThat(slot.getEffectiveUntil()).isEqualTo(LocalDate.of(2026, 8, 1));
    }

    @Test
    void applyFullUpdateThrowsDomainExceptionWhenGradeIsMissing() {
        TimetableSlot slot = slot("601", DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0));

        assertThatThrownBy(() -> slot.applyFullUpdate(
                ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                null, "정T", "미적분", "FFCC00"))
                .isInstanceOf(InvalidTimetableSlotException.class)
                .satisfies(e -> assertThat(((InvalidTimetableSlotException) e).getContext())
                        .containsEntry("field", "grade"));
    }

    @Test
    void applyFullUpdateAppliesNewColor() {
        TimetableSlot slot = slot("601", DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0));

        slot.applyFullUpdate(ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "미적분", "00AACC");

        assertThat(slot.getColor()).isEqualTo("00AACC");
    }

    @Test
    void applyFullUpdateThrowsWhenColorIsNotValidHex() {
        TimetableSlot slot = slot("601", DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0));

        assertThatThrownBy(() -> slot.applyFullUpdate(
                ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "미적분", "ZZZZZZ"))
                .isInstanceOf(InvalidTimetableColorException.class);
    }

    @Test
    void restoreKeepsPersistedId() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 1, 0, 0);
        TimetableSlot slot = TimetableSlot.restore(
                10L, 1L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "미적분", "FFCC00", FROM, UNTIL, now, now);

        assertThat(slot.getId()).isEqualTo(10L);
        assertThat(slot.getColor()).isEqualTo("FFCC00");
    }

    @Test
    void createThrowsDomainExceptionWhenTimeRangeIsInvalid() {
        assertThatThrownBy(() -> slot(
                "601", DayOfWeek.MONDAY, LocalTime.of(11, 0), LocalTime.of(10, 0)))
                .isInstanceOf(InvalidTimetableSlotException.class)
                .satisfies(e -> assertThat(((InvalidTimetableSlotException) e).getContext())
                        .containsEntry("field", "timeRange"));
    }

    @Test
    void createThrowsDomainExceptionWhenGradeIsMissing() {
        assertThatThrownBy(() -> TimetableSlot.create(
                1L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                null, "정T", "미적분", "FFCC00", FROM, UNTIL))
                .isInstanceOf(InvalidTimetableSlotException.class)
                .satisfies(e -> assertThat(((InvalidTimetableSlotException) e).getContext())
                        .containsEntry("field", "grade"));
    }

    @Test
    void createThrowsWhenColorIsNull() {
        assertThatThrownBy(() -> TimetableSlot.create(
                1L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "미적분", null, FROM, UNTIL))
                .isInstanceOf(InvalidTimetableColorException.class);
    }

    @Test
    void createThrowsWhenColorIsNotSixHexDigits() {
        assertThatThrownBy(() -> TimetableSlot.create(
                1L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "미적분", "FFF", FROM, UNTIL))
                .isInstanceOf(InvalidTimetableColorException.class);
    }

    @Test
    void classroomThrowsDomainExceptionWhenCodeIsBlank() {
        assertThatThrownBy(() -> new TimetableClassroom("6층", " "))
                .isInstanceOf(InvalidTimetableClassroomException.class)
                .satisfies(e -> assertThat(((InvalidTimetableClassroomException) e).getContext())
                        .containsEntry("field", "code"));
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew compileTestJava`
Expected: FAIL — `TimetableSlot.create`/`restore`/`applyFullUpdate`에 `color` 파라미터가 없어 컴파일 에러. (Java에서는 이 컴파일 실패가 유효한 RED 상태다.)

- [ ] **Step 3: `TimetableSlot`에 `color` 추가 및 검증 구현**

`TimetableSlot.java` 전체를 다음으로 교체한다:

```java
package com.academy.mudogroupware.timetable.domain.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.regex.Pattern;

import com.academy.mudogroupware.timetable.domain.exception.InvalidTimetableColorException;
import com.academy.mudogroupware.timetable.domain.exception.InvalidTimetableSlotException;

public final class TimetableSlot {

    private static final Pattern HEX_COLOR = Pattern.compile("^[0-9A-Fa-f]{6}$");

    private final Long id;
    private final Long timetableSetId;
    private ClassType classType;
    private DayOfWeek dayOfWeek;
    private String classroomCode;
    private LocalTime startTime;
    private LocalTime endTime;
    private Grade grade;
    private String teacherName;
    private String subjectName;
    private String color;
    private LocalDate effectiveFrom;
    private LocalDate effectiveUntil;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private TimetableSlot(Long id, Long timetableSetId, ClassType classType, DayOfWeek dayOfWeek,
                           String classroomCode, LocalTime startTime, LocalTime endTime, Grade grade,
                           String teacherName, String subjectName, String color, LocalDate effectiveFrom,
                           LocalDate effectiveUntil, LocalDateTime createdAt, LocalDateTime updatedAt) {
        if (timetableSetId == null) {
            throw new InvalidTimetableSlotException("timetableSetId");
        }
        if (classType == null) {
            throw new InvalidTimetableSlotException("classType");
        }
        if (dayOfWeek == null) {
            throw new InvalidTimetableSlotException("dayOfWeek");
        }
        if (classroomCode == null || classroomCode.isBlank()) {
            throw new InvalidTimetableSlotException("classroomCode");
        }
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new InvalidTimetableSlotException("timeRange");
        }
        if (grade == null) {
            throw new InvalidTimetableSlotException("grade");
        }
        if (color == null || !HEX_COLOR.matcher(color).matches()) {
            throw new InvalidTimetableColorException();
        }
        if (effectiveFrom == null || effectiveUntil == null || effectiveUntil.isBefore(effectiveFrom)) {
            throw new InvalidTimetableSlotException("effectivePeriod");
        }
        this.id = id;
        this.timetableSetId = timetableSetId;
        this.classType = classType;
        this.dayOfWeek = dayOfWeek;
        this.classroomCode = classroomCode;
        this.startTime = startTime;
        this.endTime = endTime;
        this.grade = grade;
        this.teacherName = teacherName;
        this.subjectName = subjectName;
        this.color = color;
        this.effectiveFrom = effectiveFrom;
        this.effectiveUntil = effectiveUntil;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static TimetableSlot create(Long timetableSetId, ClassType classType, DayOfWeek dayOfWeek,
                                        String classroomCode, LocalTime startTime, LocalTime endTime, Grade grade,
                                        String teacherName, String subjectName, String color,
                                        LocalDate effectiveFrom, LocalDate effectiveUntil) {
        return new TimetableSlot(null, timetableSetId, classType, dayOfWeek, classroomCode, startTime, endTime,
                grade, teacherName, subjectName, color, effectiveFrom, effectiveUntil, null, null);
    }

    public static TimetableSlot restore(Long id, Long timetableSetId, ClassType classType, DayOfWeek dayOfWeek,
                                         String classroomCode, LocalTime startTime, LocalTime endTime, Grade grade,
                                         String teacherName, String subjectName, String color,
                                         LocalDate effectiveFrom, LocalDate effectiveUntil, LocalDateTime createdAt,
                                         LocalDateTime updatedAt) {
        return new TimetableSlot(id, timetableSetId, classType, dayOfWeek, classroomCode, startTime, endTime, grade,
                teacherName, subjectName, color, effectiveFrom, effectiveUntil, createdAt, updatedAt);
    }

    public void applyFullUpdate(ClassType classType, DayOfWeek dayOfWeek, String classroomCode, LocalTime startTime,
                                 LocalTime endTime, Grade grade, String teacherName, String subjectName,
                                 String color) {
        if (classroomCode == null || classroomCode.isBlank()) {
            throw new InvalidTimetableSlotException("classroomCode");
        }
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new InvalidTimetableSlotException("timeRange");
        }
        if (grade == null) {
            throw new InvalidTimetableSlotException("grade");
        }
        if (color == null || !HEX_COLOR.matcher(color).matches()) {
            throw new InvalidTimetableColorException();
        }
        this.classType = classType;
        this.dayOfWeek = dayOfWeek;
        this.classroomCode = classroomCode;
        this.startTime = startTime;
        this.endTime = endTime;
        this.grade = grade;
        this.teacherName = teacherName;
        this.subjectName = subjectName;
        this.color = color;
    }

    public void closeEffectiveUntil(LocalDate newEffectiveUntil) {
        if (newEffectiveUntil == null || newEffectiveUntil.isBefore(effectiveFrom)) {
            throw new InvalidTimetableSlotException("effectiveUntil");
        }
        this.effectiveUntil = newEffectiveUntil;
    }

    public boolean overlaps(TimetableSlot other) {
        if (!this.classroomCode.equals(other.classroomCode) || this.dayOfWeek != other.dayOfWeek) {
            return false;
        }
        boolean timeOverlaps = this.startTime.isBefore(other.endTime) && other.startTime.isBefore(this.endTime);
        boolean rangeOverlaps = !this.effectiveFrom.isAfter(other.effectiveUntil)
                && !other.effectiveFrom.isAfter(this.effectiveUntil);
        return timeOverlaps && rangeOverlaps;
    }

    public Long getId() {
        return id;
    }

    public Long getTimetableSetId() {
        return timetableSetId;
    }

    public ClassType getClassType() {
        return classType;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public String getClassroomCode() {
        return classroomCode;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public Grade getGrade() {
        return grade;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public String getColor() {
        return color;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public LocalDate getEffectiveUntil() {
        return effectiveUntil;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
```

- [ ] **Step 4: `TimetableSlotTest`만 단독으로는 아직 못 돌린다 — 이유 확인**

Run: `./gradlew compileTestJava`
Expected: 여전히 FAIL. 이번엔 `TimetableSlot.java` 자체는 컴파일되지만, `CreateTimetableSlotService`/`UpdateTimetableSlotService`/`TimetableSlotPersistenceAdapter`/`GetTimetableSlotService`/`GetTimetableSlotsService`와 그 테스트들이 옛 시그니처로 `TimetableSlot.create`/`restore`를 호출하고 있어 에러가 난다. Task 4~7을 마저 진행한다.

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/com/academy/mudogroupware/timetable/domain/model/TimetableSlot.java \
        src/test/java/com/academy/mudogroupware/timetable/domain/model/TimetableSlotTest.java
git commit -m "feat: TimetableSlot 도메인에 color 필드와 검증 추가 (다음 태스크까지 빌드 깨짐, 의도됨)"
```

---

### Task 4: `TimetableSlotEntity` + `TimetableSlotPersistenceAdapter` — `color` 매핑

**Files:**
- Modify: `src/main/java/com/academy/mudogroupware/timetable/infrastructure/persistence/TimetableSlotEntity.java`
- Modify: `src/main/java/com/academy/mudogroupware/timetable/infrastructure/persistence/TimetableSlotPersistenceAdapter.java`
- Modify: `src/test/java/com/academy/mudogroupware/timetable/infrastructure/persistence/TimetableSlotPersistenceAdapterDataJpaTest.java`

- [ ] **Step 1: `TimetableSlotEntity`에 `color` 컬럼 추가**

`@Column(name = "subject_name", ...) private String subjectName;` 아래에 추가:

```java
    @Column(length = 6, nullable = false)
    private String color;
```

`@Builder` 생성자 파라미터에 `String color`를 `subjectName` 다음에 추가하고 `this.color = color;`도 추가한다:

```java
    @Builder
    private TimetableSlotEntity(Long id, Long timetableSetId, ClassType classType, DayOfWeek dayOfWeek,
                                 String classroomCode, LocalTime startTime, LocalTime endTime, Grade grade,
                                 String teacherName, String subjectName, String color, LocalDate effectiveFrom,
                                 LocalDate effectiveUntil) {
        this.id = id;
        this.timetableSetId = timetableSetId;
        this.classType = classType;
        this.dayOfWeek = dayOfWeek;
        this.classroomCode = classroomCode;
        this.startTime = startTime;
        this.endTime = endTime;
        this.grade = grade;
        this.teacherName = teacherName;
        this.subjectName = subjectName;
        this.color = color;
        this.effectiveFrom = effectiveFrom;
        this.effectiveUntil = effectiveUntil;
    }
```

`update(...)` 메서드도 `color` 파라미터를 받아 반영하도록 바꾼다:

```java
    public void update(ClassType classType, DayOfWeek dayOfWeek, String classroomCode, LocalTime startTime,
                        LocalTime endTime, Grade grade, String teacherName, String subjectName, String color,
                        LocalDate effectiveFrom, LocalDate effectiveUntil) {
        this.classType = classType;
        this.dayOfWeek = dayOfWeek;
        this.classroomCode = classroomCode;
        this.startTime = startTime;
        this.endTime = endTime;
        this.grade = grade;
        this.teacherName = teacherName;
        this.subjectName = subjectName;
        this.color = color;
        this.effectiveFrom = effectiveFrom;
        this.effectiveUntil = effectiveUntil;
    }
```

- [ ] **Step 2: `TimetableSlotPersistenceAdapter` 3곳(`updateExisting`/`toEntity`/`toDomain`) 수정**

```java
    private TimetableSlotEntity updateExisting(TimetableSlot domain) {
        TimetableSlotEntity entity = timetableSlotJpaRepository.getReferenceById(domain.getId());
        entity.update(domain.getClassType(), domain.getDayOfWeek(), domain.getClassroomCode(),
                domain.getStartTime(), domain.getEndTime(), domain.getGrade(), domain.getTeacherName(),
                domain.getSubjectName(), domain.getColor(), domain.getEffectiveFrom(), domain.getEffectiveUntil());
        return entity;
    }

    private TimetableSlotEntity toEntity(TimetableSlot domain) {
        return TimetableSlotEntity.builder()
                .id(domain.getId())
                .timetableSetId(domain.getTimetableSetId())
                .classType(domain.getClassType())
                .dayOfWeek(domain.getDayOfWeek())
                .classroomCode(domain.getClassroomCode())
                .startTime(domain.getStartTime())
                .endTime(domain.getEndTime())
                .grade(domain.getGrade())
                .teacherName(domain.getTeacherName())
                .subjectName(domain.getSubjectName())
                .color(domain.getColor())
                .effectiveFrom(domain.getEffectiveFrom())
                .effectiveUntil(domain.getEffectiveUntil())
                .build();
    }

    private TimetableSlot toDomain(TimetableSlotEntity entity) {
        return TimetableSlot.restore(
                entity.getId(), entity.getTimetableSetId(), entity.getClassType(), entity.getDayOfWeek(),
                entity.getClassroomCode(), entity.getStartTime(), entity.getEndTime(), entity.getGrade(),
                entity.getTeacherName(), entity.getSubjectName(), entity.getColor(), entity.getEffectiveFrom(),
                entity.getEffectiveUntil(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
```

- [ ] **Step 3: `TimetableSlotPersistenceAdapterDataJpaTest`에 색상 저장/복원 검증 추가**

`savesAndFindsSlot` 테스트를 다음으로 바꾸고(생성 호출에 `"FFCC00"` 추가), 색상 검증 assert를 추가한다:

```java
    @Test
    void savesAndFindsSlot() {
        TimetableSlot slot = TimetableSlot.create(
                1L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "미적분", "FFCC00", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16));

        TimetableSlot saved = adapter.save(slot);
        Optional<TimetableSlot> found = adapter.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getClassroomCode()).isEqualTo("601");
        assertThat(found.get().getGrade()).isEqualTo(Grade.HIGH_3);
        assertThat(found.get().getColor()).isEqualTo("FFCC00");
    }
```

나머지 두 테스트도 같은 방식으로 `subjectName` 다음, `effectiveFrom` 앞에 `"FFCC00"`을 끼워 넣는다:

```java
    @Test
    void findsAllByTimetableSetIdAndClassroomCode() {
        TimetableSlot slot601 = TimetableSlot.create(
                1L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "미적분", "FFCC00", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16));
        TimetableSlot slot602 = TimetableSlot.create(
                1L, ClassType.CLASS, DayOfWeek.MONDAY, "602", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "미적분", "FFCC00", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16));
        adapter.save(slot601);
        adapter.save(slot602);

        List<TimetableSlot> found = adapter.findAllByTimetableSetIdAndClassroomCode(1L, "601");

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getClassroomCode()).isEqualTo("601");
    }

    @Test
    void deletesSlotById() {
        TimetableSlot slot = TimetableSlot.create(
                1L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "미적분", "FFCC00", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16));
        TimetableSlot saved = adapter.save(slot);

        adapter.deleteById(saved.getId());

        assertThat(adapter.findById(saved.getId())).isEmpty();
    }
```

- [ ] **Step 4: 컴파일 확인 (여전히 전체 통과는 안 됨, 예상된 상태)**

Run: `./gradlew compileTestJava`
Expected: `CreateTimetableSlotService`/`UpdateTimetableSlotService`/`GetTimetableSlotService`/`GetTimetableSlotsService`와 그 테스트가 아직 안 고쳐져서 FAIL. Task 5~7로 이어간다.

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/com/academy/mudogroupware/timetable/infrastructure/persistence/TimetableSlotEntity.java \
        src/main/java/com/academy/mudogroupware/timetable/infrastructure/persistence/TimetableSlotPersistenceAdapter.java \
        src/test/java/com/academy/mudogroupware/timetable/infrastructure/persistence/TimetableSlotPersistenceAdapterDataJpaTest.java
git commit -m "feat: TimetableSlotEntity/PersistenceAdapter에 color 매핑 추가"
```

---

### Task 5: 슬롯 생성 경로 — `color` 필수 필드로 받기

**Files:**
- Modify: `src/main/java/com/academy/mudogroupware/timetable/application/command/CreateTimetableSlotCommand.java`
- Modify: `src/main/java/com/academy/mudogroupware/timetable/presentation/api/request/CreateTimetableSlotRequest.java`
- Modify: `src/main/java/com/academy/mudogroupware/timetable/application/service/CreateTimetableSlotService.java`
- Modify: `src/test/java/com/academy/mudogroupware/timetable/application/service/CreateTimetableSlotServiceTest.java`

- [ ] **Step 1: `CreateTimetableSlotCommand`에 `color` 추가**

```java
package com.academy.mudogroupware.timetable.application.command;

import java.time.DayOfWeek;
import java.time.LocalTime;

import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.Grade;

public record CreateTimetableSlotCommand(
        Long timetableSetId, ClassType classType, DayOfWeek dayOfWeek, String classroomCode,
        LocalTime startTime, LocalTime endTime, Grade grade, String teacherName, String subjectName,
        String color) {
}
```

- [ ] **Step 2: `CreateTimetableSlotRequest`에 `color` 검증 필드 추가**

```java
package com.academy.mudogroupware.timetable.presentation.api.request;

import java.time.DayOfWeek;
import java.time.LocalTime;

import com.academy.mudogroupware.timetable.application.command.CreateTimetableSlotCommand;
import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.Grade;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateTimetableSlotRequest(
        @Schema(description = "수업 종류") @NotNull ClassType classType,
        @Schema(description = "요일") @NotNull DayOfWeek dayOfWeek,
        @Schema(description = "강의실 코드", example = "601") @NotBlank String classroomCode,
        @Schema(description = "시작 시각", example = "09:00") @NotNull LocalTime startTime,
        @Schema(description = "종료 시각", example = "11:00") @NotNull LocalTime endTime,
        @Schema(description = "학년(초1~고3 중 하나)", example = "HIGH_3") @NotNull Grade grade,
        @Schema(description = "강사명", example = "정T") String teacherName,
        @Schema(description = "과목", example = "미적분") String subjectName,
        @Schema(description = "색상(6자리 16진수, RRGGBB)", example = "FFCC00")
        @NotBlank @Pattern(regexp = "^[0-9A-Fa-f]{6}$") String color
) {

    public CreateTimetableSlotCommand toCommand(Long timetableSetId) {
        return new CreateTimetableSlotCommand(timetableSetId, classType, dayOfWeek, classroomCode,
                startTime, endTime, grade, teacherName, subjectName, color);
    }
}
```

- [ ] **Step 3: `CreateTimetableSlotService`가 `color`를 넘기도록 수정**

```java
    @Override
    public Long createSlot(CreateTimetableSlotCommand command) {
        TimetableSet set = timetableSetRepository.findById(command.timetableSetId())
                .orElseThrow(TimetableSetNotFoundException::new);

        TimetableSlot candidate = TimetableSlot.create(
                set.getId(), command.classType(), command.dayOfWeek(), command.classroomCode(),
                command.startTime(), command.endTime(), command.grade(), command.teacherName(),
                command.subjectName(), command.color(), set.getStartDate(), set.getEndDate());

        boolean conflicts = timetableSlotRepository
                .findAllByTimetableSetIdAndClassroomCode(set.getId(), command.classroomCode()).stream()
                .anyMatch(candidate::overlaps);
        if (conflicts) {
            throw new ClassroomTimeConflictException();
        }

        return timetableSlotRepository.save(candidate).getId();
    }
```

(다른 부분은 그대로 — import도 추가 필요 없음, `command.color()`만 새 라인.)

- [ ] **Step 4: `CreateTimetableSlotServiceTest` 갱신**

세 테스트 전부에서 `CreateTimetableSlotCommand`와 `TimetableSlot.restore`/`existing` 생성 호출에 `"FFCC00"`을 끼워 넣는다:

```java
    @Test
    void createSlotSavesAndReturnsIdWhenNoConflict() {
        when(timetableSetRepository.findById(1L)).thenReturn(Optional.of(timetableSet()));
        when(timetableSlotRepository.findAllByTimetableSetIdAndClassroomCode(1L, "601")).thenReturn(List.of());
        TimetableSlot saved = TimetableSlot.restore(
                100L, 1L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "미적분", "FFCC00", SET_START, SET_END, null, null);
        when(timetableSlotRepository.save(any(TimetableSlot.class))).thenReturn(saved);

        CreateTimetableSlotCommand command = new CreateTimetableSlotCommand(
                1L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "미적분", "FFCC00");

        Long id = service.createSlot(command);

        assertThat(id).isEqualTo(100L);
    }

    @Test
    void createSlotThrowsWhenTimetableSetNotFound() {
        when(timetableSetRepository.findById(999L)).thenReturn(Optional.empty());
        CreateTimetableSlotCommand command = new CreateTimetableSlotCommand(
                999L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "미적분", "FFCC00");

        assertThatThrownBy(() -> service.createSlot(command))
                .isInstanceOf(TimetableSetNotFoundException.class);
    }

    @Test
    void createSlotThrowsWhenClassroomTimeConflicts() {
        when(timetableSetRepository.findById(1L)).thenReturn(Optional.of(timetableSet()));
        TimetableSlot existing = TimetableSlot.restore(
                50L, 1L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "미적분", "FFCC00", SET_START, SET_END, null, null);
        when(timetableSlotRepository.findAllByTimetableSetIdAndClassroomCode(1L, "601"))
                .thenReturn(List.of(existing));

        CreateTimetableSlotCommand command = new CreateTimetableSlotCommand(
                1L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(10, 0), LocalTime.of(12, 0),
                Grade.HIGH_3, "정T", "미적분", "FFCC00");

        assertThatThrownBy(() -> service.createSlot(command))
                .isInstanceOf(ClassroomTimeConflictException.class);
    }
```

- [ ] **Step 5: 컴파일 확인 (Update/Get 경로는 아직 남아있음, 예상된 상태)**

Run: `./gradlew compileTestJava`
Expected: FAIL — `UpdateTimetableSlotService`류가 아직 안 고쳐짐. Task 6으로 이어간다.

- [ ] **Step 6: 커밋**

```bash
git add src/main/java/com/academy/mudogroupware/timetable/application/command/CreateTimetableSlotCommand.java \
        src/main/java/com/academy/mudogroupware/timetable/presentation/api/request/CreateTimetableSlotRequest.java \
        src/main/java/com/academy/mudogroupware/timetable/application/service/CreateTimetableSlotService.java \
        src/test/java/com/academy/mudogroupware/timetable/application/service/CreateTimetableSlotServiceTest.java
git commit -m "feat: 슬롯 생성 요청에 color 필수 필드 추가"
```

---

### Task 6: 슬롯 수정 경로 — `color` 필수 필드로 받기

**Files:**
- Modify: `src/main/java/com/academy/mudogroupware/timetable/application/command/UpdateTimetableSlotCommand.java`
- Modify: `src/main/java/com/academy/mudogroupware/timetable/presentation/api/request/UpdateTimetableSlotRequest.java`
- Modify: `src/main/java/com/academy/mudogroupware/timetable/application/service/UpdateTimetableSlotService.java`
- Modify: `src/test/java/com/academy/mudogroupware/timetable/application/service/UpdateTimetableSlotServiceTest.java`

- [ ] **Step 1: `UpdateTimetableSlotCommand`에 `color` 추가**

```java
package com.academy.mudogroupware.timetable.application.command;

import java.time.DayOfWeek;
import java.time.LocalTime;

import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.Grade;
import com.academy.mudogroupware.timetable.domain.model.UpdateScope;

public record UpdateTimetableSlotCommand(
        Long timetableSetId, Long timetableSlotId, UpdateScope scope, ClassType classType,
        DayOfWeek dayOfWeek, String classroomCode, LocalTime startTime, LocalTime endTime, Grade grade,
        String teacherName, String subjectName, String color) {
}
```

- [ ] **Step 2: `UpdateTimetableSlotRequest`에 `color` 검증 필드 추가**

```java
package com.academy.mudogroupware.timetable.presentation.api.request;

import java.time.DayOfWeek;
import java.time.LocalTime;

import com.academy.mudogroupware.timetable.application.command.UpdateTimetableSlotCommand;
import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.Grade;
import com.academy.mudogroupware.timetable.domain.model.UpdateScope;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UpdateTimetableSlotRequest(
        @Schema(description = "적용 범위. 현재는 ALL만 지원") @NotNull UpdateScope scope,
        @Schema(description = "수업 종류") @NotNull ClassType classType,
        @Schema(description = "요일") @NotNull DayOfWeek dayOfWeek,
        @Schema(description = "강의실 코드") @NotBlank String classroomCode,
        @Schema(description = "시작 시각") @NotNull LocalTime startTime,
        @Schema(description = "종료 시각") @NotNull LocalTime endTime,
        @Schema(description = "학년(초1~고3 중 하나)") @NotNull Grade grade,
        @Schema(description = "강사명") String teacherName,
        @Schema(description = "과목") String subjectName,
        @Schema(description = "색상(6자리 16진수, RRGGBB)", example = "FFCC00")
        @NotBlank @Pattern(regexp = "^[0-9A-Fa-f]{6}$") String color
) {

    public UpdateTimetableSlotCommand toCommand(Long timetableSetId, Long timetableSlotId) {
        return new UpdateTimetableSlotCommand(timetableSetId, timetableSlotId, scope, classType,
                dayOfWeek, classroomCode, startTime, endTime, grade, teacherName, subjectName, color);
    }
}
```

- [ ] **Step 3: `UpdateTimetableSlotService`가 `color`를 넘기도록 수정**

```java
    @Override
    public void updateSlot(UpdateTimetableSlotCommand command) {
        if (command.scope() != UpdateScope.ALL) {
            throw new UnsupportedSlotScopeException();
        }

        timetableSetRepository.findById(command.timetableSetId())
                .orElseThrow(TimetableSetNotFoundException::new);

        TimetableSlot slot = timetableSlotRepository.findById(command.timetableSlotId())
                .filter(found -> found.getTimetableSetId().equals(command.timetableSetId()))
                .orElseThrow(TimetableSlotNotFoundException::new);

        TimetableSlot candidate = TimetableSlot.create(
                slot.getTimetableSetId(), command.classType(), command.dayOfWeek(), command.classroomCode(),
                command.startTime(), command.endTime(), command.grade(), command.teacherName(),
                command.subjectName(), command.color(), slot.getEffectiveFrom(), slot.getEffectiveUntil());

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
                command.subjectName(), command.color());

        timetableSlotRepository.save(slot);
    }
```

- [ ] **Step 4: `UpdateTimetableSlotServiceTest` 갱신**

`existingSlot()` 헬퍼와 모든 `UpdateTimetableSlotCommand`/`TimetableSlot.restore` 호출에 `"FFCC00"`(또는 갱신 테스트는 새 색 `"00AACC"`)을 끼워 넣는다:

```java
    private TimetableSlot existingSlot() {
        return TimetableSlot.restore(
                100L, 1L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "미적분", "FFCC00", FROM, UNTIL, null, null);
    }

    @Test
    void updateSlotAppliesNewValuesWhenScopeIsAll() {
        when(timetableSetRepository.findById(1L)).thenReturn(Optional.of(timetableSet()));
        TimetableSlot slot = existingSlot();
        when(timetableSlotRepository.findById(100L)).thenReturn(Optional.of(slot));
        when(timetableSlotRepository.findAllByTimetableSetIdAndClassroomCode(1L, "602")).thenReturn(List.of());
        UpdateTimetableSlotCommand command = new UpdateTimetableSlotCommand(
                1L, 100L, UpdateScope.ALL, ClassType.SPECIAL, DayOfWeek.TUESDAY, "602",
                LocalTime.of(13, 0), LocalTime.of(15, 0), Grade.HIGH_2, "오T", "물리", "00AACC");

        service.updateSlot(command);

        verify(timetableSlotRepository).save(slot);
        assertThat(slot.getGrade()).isEqualTo(Grade.HIGH_2);
        assertThat(slot.getColor()).isEqualTo("00AACC");
    }

    @Test
    void updateSlotThrowsWhenScopeIsNotAll() {
        UpdateTimetableSlotCommand command = new UpdateTimetableSlotCommand(
                1L, 100L, UpdateScope.THIS_OCCURRENCE, ClassType.CLASS, DayOfWeek.MONDAY, "601",
                LocalTime.of(9, 0), LocalTime.of(11, 0), Grade.HIGH_3, "정T", "미적분", "FFCC00");

        assertThatThrownBy(() -> service.updateSlot(command))
                .isInstanceOf(UnsupportedSlotScopeException.class);
    }

    @Test
    void updateSlotThrowsWhenTimetableSetNotFound() {
        when(timetableSetRepository.findById(999L)).thenReturn(Optional.empty());
        UpdateTimetableSlotCommand command = new UpdateTimetableSlotCommand(
                999L, 100L, UpdateScope.ALL, ClassType.CLASS, DayOfWeek.MONDAY, "601",
                LocalTime.of(9, 0), LocalTime.of(11, 0), Grade.HIGH_3, "정T", "미적분", "FFCC00");

        assertThatThrownBy(() -> service.updateSlot(command))
                .isInstanceOf(TimetableSetNotFoundException.class);
    }

    @Test
    void updateSlotThrowsWhenSlotIsMissing() {
        when(timetableSetRepository.findById(1L)).thenReturn(Optional.of(timetableSet()));
        UpdateTimetableSlotCommand command = new UpdateTimetableSlotCommand(
                1L, 100L, UpdateScope.ALL, ClassType.CLASS, DayOfWeek.MONDAY, "601",
                LocalTime.of(9, 0), LocalTime.of(11, 0), Grade.HIGH_3, "정T", "미적분", "FFCC00");

        assertThatThrownBy(() -> service.updateSlot(command))
                .isInstanceOf(TimetableSlotNotFoundException.class);
    }

    @Test
    void updateSlotThrowsWhenNotFound() {
        when(timetableSetRepository.findById(1L)).thenReturn(Optional.of(timetableSet()));
        when(timetableSlotRepository.findById(999L)).thenReturn(Optional.empty());
        UpdateTimetableSlotCommand command = new UpdateTimetableSlotCommand(
                1L, 999L, UpdateScope.ALL, ClassType.CLASS, DayOfWeek.MONDAY, "601",
                LocalTime.of(9, 0), LocalTime.of(11, 0), Grade.HIGH_3, "정T", "미적분", "FFCC00");

        assertThatThrownBy(() -> service.updateSlot(command))
                .isInstanceOf(TimetableSlotNotFoundException.class);
    }

    @Test
    void updateSlotThrowsWhenNewTimeConflictsWithAnotherSlot() {
        when(timetableSetRepository.findById(1L)).thenReturn(Optional.of(timetableSet()));
        TimetableSlot slot = existingSlot();
        when(timetableSlotRepository.findById(100L)).thenReturn(Optional.of(slot));
        TimetableSlot other = TimetableSlot.restore(
                200L, 1L, ClassType.CLASS, DayOfWeek.TUESDAY, "602", LocalTime.of(13, 0), LocalTime.of(15, 0),
                Grade.HIGH_2, "오T", "물리", "FFCC00", FROM, UNTIL, null, null);
        when(timetableSlotRepository.findAllByTimetableSetIdAndClassroomCode(1L, "602")).thenReturn(List.of(other));

        UpdateTimetableSlotCommand command = new UpdateTimetableSlotCommand(
                1L, 100L, UpdateScope.ALL, ClassType.SPECIAL, DayOfWeek.TUESDAY, "602",
                LocalTime.of(14, 0), LocalTime.of(16, 0), Grade.HIGH_2, "오T", "물리", "00AACC");

        assertThatThrownBy(() -> service.updateSlot(command))
                .isInstanceOf(ClassroomTimeConflictException.class);
    }
```

- [ ] **Step 5: 컴파일 확인 (Get 경로는 아직 남아있음, 예상된 상태)**

Run: `./gradlew compileTestJava`
Expected: FAIL — `GetTimetableSlotService`/`GetTimetableSlotsService`가 아직 안 고쳐짐. Task 7로 이어간다.

- [ ] **Step 6: 커밋**

```bash
git add src/main/java/com/academy/mudogroupware/timetable/application/command/UpdateTimetableSlotCommand.java \
        src/main/java/com/academy/mudogroupware/timetable/presentation/api/request/UpdateTimetableSlotRequest.java \
        src/main/java/com/academy/mudogroupware/timetable/application/service/UpdateTimetableSlotService.java \
        src/test/java/com/academy/mudogroupware/timetable/application/service/UpdateTimetableSlotServiceTest.java
git commit -m "feat: 슬롯 수정 요청에 color 필수 필드 추가"
```

---

### Task 7: 조회 경로 — `TimetableSlotView`/`TimetableSlotResponse`에 `color` 노출

**Files:**
- Modify: `src/main/java/com/academy/mudogroupware/timetable/application/query/TimetableSlotView.java`
- Modify: `src/main/java/com/academy/mudogroupware/timetable/presentation/api/response/TimetableSlotResponse.java`
- Modify: `src/main/java/com/academy/mudogroupware/timetable/application/service/GetTimetableSlotService.java`
- Modify: `src/main/java/com/academy/mudogroupware/timetable/application/service/GetTimetableSlotsService.java`
- Modify: `src/test/java/com/academy/mudogroupware/timetable/application/service/GetTimetableSlotServiceTest.java`
- Modify: `src/test/java/com/academy/mudogroupware/timetable/application/service/GetTimetableSlotsServiceTest.java`

- [ ] **Step 1: `TimetableSlotView`에 `color` 추가**

```java
package com.academy.mudogroupware.timetable.application.query;

import java.time.DayOfWeek;
import java.time.LocalTime;

import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.Grade;

public record TimetableSlotView(
        Long timetableSlotId, ClassType classType, DayOfWeek dayOfWeek, String classroomCode,
        LocalTime startTime, LocalTime endTime, Grade grade, String teacherName, String subjectName,
        String color) {
}
```

- [ ] **Step 2: `TimetableSlotResponse`에 `color` 추가**

```java
package com.academy.mudogroupware.timetable.presentation.api.response;

import java.time.DayOfWeek;
import java.time.LocalTime;

import com.academy.mudogroupware.timetable.application.query.TimetableSlotView;
import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.Grade;

import io.swagger.v3.oas.annotations.media.Schema;

public record TimetableSlotResponse(
        @Schema(description = "수업 슬롯 번호") Long timetableSlotId,
        @Schema(description = "수업 종류") ClassType classType,
        @Schema(description = "요일") DayOfWeek dayOfWeek,
        @Schema(description = "강의실 코드") String classroomCode,
        @Schema(description = "시작 시각") LocalTime startTime,
        @Schema(description = "종료 시각") LocalTime endTime,
        @Schema(description = "학년(초1~고3 중 하나)") Grade grade,
        @Schema(description = "강사명") String teacherName,
        @Schema(description = "과목") String subjectName,
        @Schema(description = "색상(6자리 16진수, RRGGBB)") String color
) {

    public static TimetableSlotResponse from(TimetableSlotView view) {
        return new TimetableSlotResponse(
                view.timetableSlotId(), view.classType(), view.dayOfWeek(), view.classroomCode(),
                view.startTime(), view.endTime(), view.grade(), view.teacherName(), view.subjectName(),
                view.color());
    }
}
```

- [ ] **Step 3: `GetTimetableSlotService`/`GetTimetableSlotsService`가 `color`를 View에 채우도록 수정**

`GetTimetableSlotService.java`의 `getSlot(...)` 마지막 return:

```java
        return new TimetableSlotView(
                slot.getId(), slot.getClassType(), slot.getDayOfWeek(), slot.getClassroomCode(),
                slot.getStartTime(), slot.getEndTime(), slot.getGrade(), slot.getTeacherName(),
                slot.getSubjectName(), slot.getColor());
```

`GetTimetableSlotsService.java`의 `toView(...)`:

```java
    private TimetableSlotView toView(TimetableSlot slot) {
        return new TimetableSlotView(
                slot.getId(), slot.getClassType(), slot.getDayOfWeek(), slot.getClassroomCode(),
                slot.getStartTime(), slot.getEndTime(), slot.getGrade(), slot.getTeacherName(),
                slot.getSubjectName(), slot.getColor());
    }
```

- [ ] **Step 4: 두 조회 서비스 테스트 갱신**

`GetTimetableSlotServiceTest.java`의 두 `TimetableSlot.restore(...)` 호출(`getSlotReturnsViewWhenBelongsToSet`, `getSlotThrowsWhenBelongsToDifferentSet`)에 `subjectName` 다음, `effectiveFrom` 앞에 `"FFCC00"` 추가. `GetTimetableSlotsServiceTest.java`의 `TimetableSlot.restore(...)` 호출도 동일하게 수정.

- [ ] **Step 5: 전체 컴파일 + 여기까지의 단위 테스트 실행 (이제부터 GREEN)**

Run: `./gradlew test --tests "com.academy.mudogroupware.timetable.domain.model.TimetableSlotTest" --tests "com.academy.mudogroupware.timetable.application.service.CreateTimetableSlotServiceTest" --tests "com.academy.mudogroupware.timetable.application.service.UpdateTimetableSlotServiceTest" --tests "com.academy.mudogroupware.timetable.application.service.GetTimetableSlotServiceTest" --tests "com.academy.mudogroupware.timetable.application.service.GetTimetableSlotsServiceTest" --tests "com.academy.mudogroupware.timetable.infrastructure.persistence.TimetableSlotPersistenceAdapterDataJpaTest"`
Expected: 전부 PASS. (`TimetableController`/`TimetableSlotController`/`ExportTimetable*` 관련 테스트는 아직 컴파일이 안 될 수 있다 — Task 8, 9~12에서 다룬다.)

- [ ] **Step 6: 커밋**

```bash
git add src/main/java/com/academy/mudogroupware/timetable/application/query/TimetableSlotView.java \
        src/main/java/com/academy/mudogroupware/timetable/presentation/api/response/TimetableSlotResponse.java \
        src/main/java/com/academy/mudogroupware/timetable/application/service/GetTimetableSlotService.java \
        src/main/java/com/academy/mudogroupware/timetable/application/service/GetTimetableSlotsService.java \
        src/test/java/com/academy/mudogroupware/timetable/application/service/GetTimetableSlotServiceTest.java \
        src/test/java/com/academy/mudogroupware/timetable/application/service/GetTimetableSlotsServiceTest.java
git commit -m "feat: 슬롯 조회 응답에 color 노출"
```

---

### Task 8: `TimetableSlotController` MockMvc 테스트 — `color` 반영

**Files:**
- Modify: `src/test/java/com/academy/mudogroupware/timetable/presentation/api/TimetableSlotControllerTest.java`

컨트롤러 자체(`TimetableSlotController.java`)는 요청 바디를 그대로 `toCommand()`에 위임하므로 수정할 필요가 없다 — Request record가 이미 Task 5/6에서 `color`를 받도록 바뀌었다.

- [ ] **Step 1: 기존 요청 바디에 `color` 추가, 색상 검증 실패 테스트 추가**

`createSlotReturns201WithGeneratedId`의 요청 바디:

```java
        String body = """
                {
                  "classType": "CLASS",
                  "dayOfWeek": "MONDAY",
                  "classroomCode": "601",
                  "startTime": "09:00:00",
                  "endTime": "11:00:00",
                  "grade": "HIGH_3",
                  "teacherName": "정T",
                  "subjectName": "미적분",
                  "color": "FFCC00"
                }
                """;
```

`createSlotReturns400WhenGradeIsMissing`을 다음으로 바꾼다(여전히 `grade` 누락만 검증하되, `color`는 채워서 다른 원인으로 400이 나지 않게 한다):

```java
    @Test
    void createSlotReturns400WhenGradeIsMissing() throws Exception {
        String body = """
                {
                  "classType": "CLASS",
                  "dayOfWeek": "MONDAY",
                  "classroomCode": "601",
                  "startTime": "09:00:00",
                  "endTime": "11:00:00",
                  "teacherName": "정T",
                  "subjectName": "미적분",
                  "color": "FFCC00"
                }
                """;

        mockMvc.perform(post("/api/timetables/1/slots")
                        .with(authentication(authenticatedUser("TIMETABLE:MANAGE")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_1"));
    }
```

`createSlotReturns400WhenGradeIsNotValidEnumValue`도 같은 방식으로 바꾼다:

```java
    @Test
    void createSlotReturns400WhenGradeIsNotValidEnumValue() throws Exception {
        String body = """
                {
                  "classType": "CLASS",
                  "dayOfWeek": "MONDAY",
                  "classroomCode": "601",
                  "startTime": "09:00:00",
                  "endTime": "11:00:00",
                  "grade": "고3",
                  "teacherName": "정T",
                  "subjectName": "미적분",
                  "color": "FFCC00"
                }
                """;

        mockMvc.perform(post("/api/timetables/1/slots")
                        .with(authentication(authenticatedUser("TIMETABLE:MANAGE")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
```

새 테스트 두 개를 `createSlotReturns401WhenUnauthenticated` 앞에 추가:

```java
    @Test
    void createSlotReturns400WhenColorIsMissing() throws Exception {
        String body = """
                {
                  "classType": "CLASS",
                  "dayOfWeek": "MONDAY",
                  "classroomCode": "601",
                  "startTime": "09:00:00",
                  "endTime": "11:00:00",
                  "grade": "HIGH_3",
                  "teacherName": "정T",
                  "subjectName": "미적분"
                }
                """;

        mockMvc.perform(post("/api/timetables/1/slots")
                        .with(authentication(authenticatedUser("TIMETABLE:MANAGE")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_1"));
    }

    @Test
    void createSlotReturns400WhenColorIsNotSixHexDigits() throws Exception {
        String body = """
                {
                  "classType": "CLASS",
                  "dayOfWeek": "MONDAY",
                  "classroomCode": "601",
                  "startTime": "09:00:00",
                  "endTime": "11:00:00",
                  "grade": "HIGH_3",
                  "teacherName": "정T",
                  "subjectName": "미적분",
                  "color": "ZZZZZZ"
                }
                """;

        mockMvc.perform(post("/api/timetables/1/slots")
                        .with(authentication(authenticatedUser("TIMETABLE:MANAGE")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_1"));
    }
```

`getSlotsReturns200WithList`와 `getSlotReturns200WithDetail`의 `new TimetableSlotView(...)` 호출에 `"FFCC00"`을 끝에 추가하고, `getSlotsReturns200WithList`에 색상 검증 assert도 추가:

```java
    @Test
    void getSlotsReturns200WithList() throws Exception {
        when(getTimetableSlotsUseCase.getSlots(1L)).thenReturn(List.of(
                new TimetableSlotView(100L, ClassType.CLASS, DayOfWeek.MONDAY, "601",
                        LocalTime.of(9, 0), LocalTime.of(11, 0), Grade.HIGH_3, "정T", "미적분", "FFCC00")));

        mockMvc.perform(get("/api/timetables/1/slots")
                        .with(authentication(authenticatedUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TIMETABLE_200_3"))
                .andExpect(jsonPath("$.data[0].classroomCode").value("601"))
                .andExpect(jsonPath("$.data[0].grade").value("HIGH_3"))
                .andExpect(jsonPath("$.data[0].color").value("FFCC00"));
    }

    @Test
    void getSlotReturns200WithDetail() throws Exception {
        when(getTimetableSlotUseCase.getSlot(1L, 100L)).thenReturn(
                new TimetableSlotView(100L, ClassType.CLASS, DayOfWeek.MONDAY, "601",
                        LocalTime.of(9, 0), LocalTime.of(11, 0), Grade.HIGH_3, "정T", "미적분", "FFCC00"));

        mockMvc.perform(get("/api/timetables/1/slots/100")
                        .with(authentication(authenticatedUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TIMETABLE_200_4"))
                .andExpect(jsonPath("$.data.teacherName").value("정T"))
                .andExpect(jsonPath("$.data.grade").value("HIGH_3"))
                .andExpect(jsonPath("$.data.color").value("FFCC00"));
    }
```

`updateSlotReturns204`의 요청 바디에도 `"color": "00AACC"` 추가:

```java
        String body = """
                {
                  "scope": "ALL",
                  "classType": "SPECIAL",
                  "dayOfWeek": "TUESDAY",
                  "classroomCode": "602",
                  "startTime": "13:00:00",
                  "endTime": "15:00:00",
                  "grade": "HIGH_2",
                  "teacherName": "오T",
                  "subjectName": "물리",
                  "color": "00AACC"
                }
                """;
```

- [ ] **Step 2: 전체 슬롯 CRUD 경로 테스트 실행**

Run: `./gradlew test --tests "com.academy.mudogroupware.timetable.presentation.api.TimetableSlotControllerTest"`
Expected: 전부 PASS.

- [ ] **Step 3: 지금까지 손댄 슬롯 CRUD 전체를 한 번에 실행해 회귀 확인**

Run: `./gradlew test --tests "com.academy.mudogroupware.timetable.*"`
Expected: `TimetableController*`/`ExportTimetable*`/renderer 테스트는 아직 옛 `TimetableExportOptions`/`ExportTimetableCommand` 시그니처를 써서 FAIL — 이건 Task 9~12 몫이라 예상된 상태다. 슬롯 생성/수정/조회/영속성/도메인 테스트만 GREEN인지 확인한다.

- [ ] **Step 4: 커밋**

```bash
git add src/test/java/com/academy/mudogroupware/timetable/presentation/api/TimetableSlotControllerTest.java
git commit -m "test: TimetableSlotController 테스트에 color 반영 및 검증 케이스 추가"
```

---

### Task 9: `TimetableExportOptions`/`TimetableExportColorCriterion`/`ExportTimetableCommand` — 그룹 색상 개념 제거

**Files:**
- Modify: `src/main/java/com/academy/mudogroupware/timetable/domain/model/TimetableExportOptions.java`
- Delete: `src/main/java/com/academy/mudogroupware/timetable/domain/model/TimetableExportColorCriterion.java`
- Modify: `src/main/java/com/academy/mudogroupware/timetable/application/command/ExportTimetableCommand.java`

- [ ] **Step 1: `TimetableExportOptions`를 `density`만 남기도록 축소**

```java
package com.academy.mudogroupware.timetable.domain.model;

public record TimetableExportOptions(TimetableExportDensity density) {
}
```

- [ ] **Step 2: `TimetableExportColorCriterion.java` 삭제**

```bash
rm src/main/java/com/academy/mudogroupware/timetable/domain/model/TimetableExportColorCriterion.java
```

- [ ] **Step 3: `ExportTimetableCommand`에서 색상 관련 필드 제거**

```java
package com.academy.mudogroupware.timetable.application.command;

import java.time.DayOfWeek;

import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportDensity;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportFormat;

public record ExportTimetableCommand(
        Long timetableSetId, TimetableExportFormat format, TimetableExportDensity density,
        DayOfWeek dayOfWeek, String floor, ClassType classType) {
}
```

- [ ] **Step 4: 컴파일 확인 (여전히 안 됨, 예상된 상태)**

Run: `./gradlew compileJava`
Expected: FAIL — `ExportTimetableService`, 3개 렌더러, `TimetableController.exportTimetable`이 옛 필드를 참조. Task 10~12로 이어간다.

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/com/academy/mudogroupware/timetable/domain/model/TimetableExportOptions.java \
        src/main/java/com/academy/mudogroupware/timetable/application/command/ExportTimetableCommand.java
git rm src/main/java/com/academy/mudogroupware/timetable/domain/model/TimetableExportColorCriterion.java
git commit -m "refactor: export의 colorCriterion/colorMap 개념 제거 (다음 태스크까지 빌드 깨짐, 의도됨)"
```

---

### Task 10: `ExportTimetableService` — 색상 파싱 로직 제거

**Files:**
- Modify: `src/main/java/com/academy/mudogroupware/timetable/application/service/ExportTimetableService.java`
- Modify: `src/test/java/com/academy/mudogroupware/timetable/application/service/ExportTimetableServiceTest.java`

- [ ] **Step 1: `ExportTimetableService`에서 `parseColors` 제거, `TimetableExportOptions` 생성 단순화**

```java
package com.academy.mudogroupware.timetable.application.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.academy.mudogroupware.timetable.application.command.ExportTimetableCommand;
import com.academy.mudogroupware.timetable.application.port.TimetableExportRenderer;
import com.academy.mudogroupware.timetable.application.query.TimetableSetDetailView;
import com.academy.mudogroupware.timetable.application.query.TimetableSlotView;
import com.academy.mudogroupware.timetable.application.usecase.ExportTimetableUseCase;
import com.academy.mudogroupware.timetable.application.usecase.GetTimetableSetUseCase;
import com.academy.mudogroupware.timetable.application.usecase.GetTimetableSlotsUseCase;
import com.academy.mudogroupware.timetable.domain.model.TimetableClassroom;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportFormat;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportOptions;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExportTimetableService implements ExportTimetableUseCase {

    private final GetTimetableSetUseCase getTimetableSetUseCase;
    private final GetTimetableSlotsUseCase getTimetableSlotsUseCase;
    private final List<TimetableExportRenderer> renderers;

    @Override
    public byte[] export(ExportTimetableCommand command) {
        TimetableSetDetailView set = getTimetableSetUseCase
                .getTimetableSet(command.timetableSetId());

        TimetableExportOptions options = new TimetableExportOptions(command.density());

        List<TimetableSlotView> allSortedSlots = getTimetableSlotsUseCase
                .getSlots(command.timetableSetId()).stream()
                .sorted(Comparator.comparing(TimetableSlotView::dayOfWeek)
                        .thenComparing(TimetableSlotView::startTime))
                .toList();

        // PDF는 인쇄용 고정 산출물이라 화면의 필터 상태와 무관하게 항상 세트 전체를 내보낸다.
        List<TimetableSlotView> slotsToRender = command.format() == TimetableExportFormat.PDF
                ? allSortedSlots
                : applyFilters(command, set, allSortedSlots);

        TimetableExportRenderer renderer = renderers.stream()
                .filter(r -> r.supports(command.format()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("지원하지 않는 내보내기 형식: " + command.format()));

        return renderer.render(set.name(), slotsToRender, options);
    }

    private List<TimetableSlotView> applyFilters(
            ExportTimetableCommand command, TimetableSetDetailView set, List<TimetableSlotView> slots) {
        Map<String, String> floorByClassroomCode = set.classrooms().stream()
                .collect(Collectors.toMap(TimetableClassroom::code, TimetableClassroom::floor, (a, b) -> a));

        return slots.stream()
                .filter(slot -> command.dayOfWeek() == null || slot.dayOfWeek() == command.dayOfWeek())
                .filter(slot -> command.classType() == null || slot.classType() == command.classType())
                .filter(slot -> command.floor() == null
                        || command.floor().equals(floorByClassroomCode.get(slot.classroomCode())))
                .toList();
    }
}
```

(`parseColors` 메서드와 `TimetableExportColor`/`Map<String, TimetableExportColor>` 관련 import를 통째로 뺐다 — 이제 색상은 각 슬롯이 이미 들고 있으므로 export 시점에 파싱할 게 없다.)

- [ ] **Step 2: `ExportTimetableServiceTest` 갱신 — `command(...)` 헬�터에서 색상 인자 제거, 색상 검증 테스트 삭제**

`command(...)` 헬퍼:

```java
    private ExportTimetableCommand command(TimetableExportFormat format, DayOfWeek dayOfWeek, String floor,
                                            ClassType classType) {
        return new ExportTimetableCommand(
                1L, format, TimetableExportDensity.NORMAL, dayOfWeek, floor, classType);
    }
```

`validColors()` 헬퍼와 `import java.util.Map;`, `import ... TimetableExportColor;`, `import ... TimetableExportColorCriterion;`, `import ... InvalidTimetableColorException;`(Task 2에서 이름만 바꿔둔 그 import)는 이제 아무 데도 안 쓰이므로 삭제한다.

`exportDelegatesToSupportingRendererWithSortedSlots` 테스트에서 옵션 검증 부분을 다음으로 바꾼다(더 이상 `colorCriterion`/`colorFor`가 없으므로):

```java
        TimetableExportOptions options = optionsCaptor.getValue();
        assertThat(options.density()).isEqualTo(TimetableExportDensity.NORMAL);
```

`exportThrowsWhenColorIsNotValidHex` 테스트는 통째로 삭제한다 — 색상 검증은 이제 export가 아니라 슬롯 생성/수정 시점(Task 3의 `TimetableSlotTest`)에서 검증된다.

`exportPropagatesNotFoundFromGetTimetableSetUseCase` 테스트의 `ExportTimetableCommand` 생성 부분도 새 시그니처로 바꾼다:

```java
    @Test
    void exportPropagatesNotFoundFromGetTimetableSetUseCase() {
        when(getTimetableSetUseCase.getTimetableSet(999L)).thenThrow(new TimetableSetNotFoundException());
        ExportTimetableCommand command = new ExportTimetableCommand(
                999L, TimetableExportFormat.EXCEL, TimetableExportDensity.NORMAL, null, null, null);

        assertThatThrownBy(() -> service.export(command))
                .isInstanceOf(TimetableSetNotFoundException.class);
    }
```

- [ ] **Step 3: 컴파일 확인 (렌더러가 아직 안 고쳐짐, 예상된 상태)**

Run: `./gradlew compileTestJava`
Expected: FAIL — 3개 렌더러와 그 테스트가 아직 `options.colorFor(...)`/`TimetableExportColorCriterion`을 쓴다. Task 11로 이어간다.

- [ ] **Step 4: 커밋**

```bash
git add src/main/java/com/academy/mudogroupware/timetable/application/service/ExportTimetableService.java \
        src/test/java/com/academy/mudogroupware/timetable/application/service/ExportTimetableServiceTest.java
git commit -m "refactor: ExportTimetableService에서 색상 파싱 로직 제거"
```

---

### Task 11: 렌더러 3종 — 슬롯의 저장된 `color`를 직접 사용

**Files:**
- Modify: `src/main/java/com/academy/mudogroupware/timetable/infrastructure/export/ExcelTimetableExportRenderer.java`
- Modify: `src/main/java/com/academy/mudogroupware/timetable/infrastructure/export/PdfTimetableExportRenderer.java`
- Modify: `src/main/java/com/academy/mudogroupware/timetable/infrastructure/export/PngTimetableExportRenderer.java`
- Modify: `src/test/java/com/academy/mudogroupware/timetable/infrastructure/export/ExcelTimetableExportRendererTest.java`
- Modify: `src/test/java/com/academy/mudogroupware/timetable/infrastructure/export/PdfTimetableExportRendererTest.java`
- Modify: `src/test/java/com/academy/mudogroupware/timetable/infrastructure/export/PngTimetableExportRendererTest.java`

- [ ] **Step 1: `ExcelTimetableExportRenderer.writeRows(...)`에서 `options.colorFor(...)` 대신 `slot.color()` 사용**

`writeRows` 메서드 안:

```java
    private void writeRows(Sheet sheet, List<TimetableSlotView> slots, TimetableExportOptions options, Font font,
                            float rowHeightPoints, XSSFWorkbook workbook, Map<String, CellStyle> stylesByColorKey) {
        int rowIndex = 1;
        for (TimetableSlotView slot : slots) {
            Row row = sheet.createRow(rowIndex++);
            row.setHeightInPoints(rowHeightPoints);
            String[] values = TimetableExportLabels.toRow(slot);
            TimetableExportColor color = TimetableExportColor.fromHex(slot.color());
            CellStyle style = stylesByColorKey.computeIfAbsent(
                    color.red() + "," + color.green() + "," + color.blue(),
                    key -> buildStyle(workbook, font, color));
            for (int i = 0; i < values.length; i++) {
                Cell cell = row.createCell(i);
                cell.setCellValue(values[i]);
                cell.setCellStyle(style);
            }
        }
    }
```

- [ ] **Step 2: `PdfTimetableExportRenderer.buildTable(...)`도 동일하게 수정**

```java
        for (TimetableSlotView slot : sortedSlots) {
            TimetableExportColor color = TimetableExportColor.fromHex(slot.color());
            for (String value : TimetableExportLabels.toRow(slot)) {
                PdfPCell cell = new PdfPCell(mixedFontPhrase(selectorCache, value, bodyFontSize, Font.NORMAL));
                cell.setMinimumHeight(rowHeightPoints);
                cell.setBackgroundColor(new Color(color.red(), color.green(), color.blue()));
                table.addCell(cell);
            }
        }
```

- [ ] **Step 3: `PngTimetableExportRenderer.render(...)`도 동일하게 수정**

```java
            for (TimetableSlotView slot : sortedSlots) {
                TimetableExportColor color = TimetableExportColor.fromHex(slot.color());
                Color rowColor = new Color(color.red(), color.green(), color.blue());
                drawRow(g, y, rowHeight, TimetableExportLabels.toRow(slot), rowColor, false, fontSize);
                y += rowHeight;
            }
```

- [ ] **Step 4: `ExcelTimetableExportRendererTest` 갱신**

```java
package com.academy.mudogroupware.timetable.infrastructure.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.timetable.application.query.TimetableSlotView;
import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.Grade;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportDensity;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportFormat;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportOptions;

class ExcelTimetableExportRendererTest {

    private final ExcelTimetableExportRenderer renderer = new ExcelTimetableExportRenderer();

    @Test
    void supportsOnlyExcelFormat() {
        assertThat(renderer.supports(TimetableExportFormat.EXCEL)).isTrue();
        assertThat(renderer.supports(TimetableExportFormat.PDF)).isFalse();
    }

    @Test
    void renderProducesReadableWorkbookWithHeaderAndRows() throws Exception {
        List<TimetableSlotView> slots = List.of(new TimetableSlotView(
                100L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "미적분", "FFCC00"));
        TimetableExportOptions options = new TimetableExportOptions(TimetableExportDensity.NORMAL);

        byte[] bytes = renderer.render("2026 여름특강", slots, options);

        assertThat(bytes).isNotEmpty();
        assertThat(bytes[0]).isEqualTo((byte) 'P');
        assertThat(bytes[1]).isEqualTo((byte) 'K');

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            XSSFSheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("요일");
            Row dataRow = sheet.getRow(1);
            assertThat(dataRow.getCell(2).getStringCellValue()).isEqualTo("601");
        }
    }

    @Test
    void renderPaintsSlotOwnColorOnDataRow() throws Exception {
        List<TimetableSlotView> slots = List.of(new TimetableSlotView(
                100L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "미적분", "00AACC"));
        TimetableExportOptions options = new TimetableExportOptions(TimetableExportDensity.NORMAL);

        byte[] bytes = renderer.render("2026 여름특강", slots, options);

        assertThat(dataRowFillColor(bytes)).isEqualTo("00AACC");
    }

    private String dataRowFillColor(byte[] bytes) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Row dataRow = workbook.getSheetAt(0).getRow(1);
            Cell cell = dataRow.getCell(0);
            XSSFCellStyle style = (XSSFCellStyle) cell.getCellStyle();
            return style.getFillForegroundColorColor().getARGBHex().substring(2).toUpperCase();
        }
    }
}
```

- [ ] **Step 5: `PdfTimetableExportRendererTest` 갱신**

```java
package com.academy.mudogroupware.timetable.infrastructure.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.timetable.application.query.TimetableSlotView;
import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.Grade;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportDensity;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportFormat;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportOptions;

class PdfTimetableExportRendererTest {

    private final PdfTimetableExportRenderer renderer = new PdfTimetableExportRenderer();

    @Test
    void supportsOnlyPdfFormat() {
        assertThat(renderer.supports(TimetableExportFormat.PDF)).isTrue();
        assertThat(renderer.supports(TimetableExportFormat.PNG)).isFalse();
    }

    @Test
    void renderProducesValidPdfBytes() {
        List<TimetableSlotView> slots = List.of(new TimetableSlotView(
                100L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "미적분", "FFCC00"));
        TimetableExportOptions options = new TimetableExportOptions(TimetableExportDensity.NORMAL);

        byte[] bytes = renderer.render("2026 여름특강", slots, options);

        assertThat(bytes).isNotEmpty();
        String header = new String(bytes, 0, 4, StandardCharsets.US_ASCII);
        assertThat(header).isEqualTo("%PDF");
    }

    @Test
    void renderSucceedsWithSlotOwnColor() {
        List<TimetableSlotView> slots = List.of(new TimetableSlotView(
                100L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "미적분", "00AACC"));
        TimetableExportOptions options = new TimetableExportOptions(TimetableExportDensity.NORMAL);

        byte[] bytes = renderer.render("2026 여름특강", slots, options);

        assertThat(bytes).isNotEmpty();
        assertThat(new String(bytes, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }
}
```

- [ ] **Step 6: `PngTimetableExportRendererTest` 갱신**

```java
package com.academy.mudogroupware.timetable.infrastructure.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.timetable.application.query.TimetableSlotView;
import com.academy.mudogroupware.timetable.domain.exception.ExportImageTooLargeException;
import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.Grade;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportDensity;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportFormat;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportOptions;

class PngTimetableExportRendererTest {

    private final PngTimetableExportRenderer renderer = new PngTimetableExportRenderer();

    @Test
    void supportsOnlyPngFormat() {
        assertThat(renderer.supports(TimetableExportFormat.PNG)).isTrue();
        assertThat(renderer.supports(TimetableExportFormat.EXCEL)).isFalse();
    }

    @Test
    void renderProducesReadablePngImage() throws Exception {
        List<TimetableSlotView> slots = List.of(new TimetableSlotView(
                100L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "미적분", "FFCC00"));

        byte[] bytes = renderer.render(
                "2026 여름특강", slots, new TimetableExportOptions(TimetableExportDensity.NORMAL));

        assertThat(bytes).isNotEmpty();
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        assertThat(image).isNotNull();
        assertThat(image.getHeight()).isGreaterThan(0);
        assertThat(image.getWidth()).isGreaterThan(0);
    }

    @Test
    void renderPaintsSlotOwnColorOnDataRow() throws Exception {
        List<TimetableSlotView> slots = List.of(new TimetableSlotView(
                100L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "미적분", "FFCC00"));

        byte[] bytes = renderer.render(
                "2026 여름특강", slots, new TimetableExportOptions(TimetableExportDensity.NORMAL));
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));

        // 요일 열(0~60px) 안쪽, 텍스트와 테두리를 피한 지점의 배경색을 검사한다.
        // title(30) + header(=NORMAL rowHeight 32) = 62, 데이터 행 중앙 y = 62 + 32/2 = 78.
        int pixel = image.getRGB(45, 78) & 0xFFFFFF;
        assertThat(pixel).isEqualTo(0xFFCC00);
    }

    @Test
    void renderAppliesDensityToHeaderHeightAsWellAsRowHeight() throws Exception {
        List<TimetableSlotView> slots = List.of(new TimetableSlotView(
                100L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "미적분", "FFCC00"));

        // title(30) + header(density rowHeight) + 1 row(density rowHeight)
        byte[] compactBytes = renderer.render(
                "2026 여름특강", slots, new TimetableExportOptions(TimetableExportDensity.COMPACT));
        byte[] spaciousBytes = renderer.render(
                "2026 여름특강", slots, new TimetableExportOptions(TimetableExportDensity.SPACIOUS));

        BufferedImage compactImage = ImageIO.read(new ByteArrayInputStream(compactBytes));
        BufferedImage spaciousImage = ImageIO.read(new ByteArrayInputStream(spaciousBytes));

        assertThat(compactImage.getHeight()).isEqualTo(30 + 24 + 24);
        assertThat(spaciousImage.getHeight()).isEqualTo(30 + 44 + 44);
    }

    @Test
    void renderThrowsWhenResultingImageExceedsMaxPixelBudget() {
        List<TimetableSlotView> hugeSlotList = new java.util.ArrayList<>();
        for (int i = 0; i < 700_000; i++) {
            hugeSlotList.add(new TimetableSlotView(
                    (long) i, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                    Grade.HIGH_3, "정T", "미적분", "FFCC00"));
        }

        assertThatThrownBy(() -> renderer.render(
                "2026 여름특강", hugeSlotList, new TimetableExportOptions(TimetableExportDensity.NORMAL)))
                .isInstanceOf(ExportImageTooLargeException.class);
    }
}
```

- [ ] **Step 7: 렌더러 + export 서비스 테스트 실행**

Run: `./gradlew test --tests "com.academy.mudogroupware.timetable.infrastructure.export.*" --tests "com.academy.mudogroupware.timetable.application.service.ExportTimetableServiceTest"`
Expected: 전부 PASS.

- [ ] **Step 8: 커밋**

```bash
git add src/main/java/com/academy/mudogroupware/timetable/infrastructure/export/ \
        src/test/java/com/academy/mudogroupware/timetable/infrastructure/export/
git commit -m "feat: 내보내기 렌더러가 슬롯에 저장된 color를 직접 사용하도록 변경"
```

---

### Task 12: `TimetableController.exportTimetable` — 쿼리 파라미터 정리

**Files:**
- Modify: `src/main/java/com/academy/mudogroupware/timetable/presentation/api/TimetableController.java`
- Modify: `src/test/java/com/academy/mudogroupware/timetable/presentation/api/TimetableControllerTest.java`

- [ ] **Step 1: `exportTimetable`에서 `colorCriterion`/`colorMap` 파라미터와 `parseColorMap` 제거, `objectMapper` 의존성 제거**

`TimetableController.java` 상단 필드에서 `private final ObjectMapper objectMapper;`를 삭제하고, 관련 import(`com.fasterxml.jackson.core.JsonProcessingException`, `com.fasterxml.jackson.core.type.TypeReference`, `com.fasterxml.jackson.databind.ObjectMapper`, `com.academy.mudogroupware.timetable.domain.exception.InvalidExportColorException`(Task 2에서 이미 이름 바뀐 것도 함께), `com.academy.mudogroupware.timetable.domain.model.TimetableExportColorCriterion`, `java.util.Map`)를 삭제한다.

`exportTimetable` 메서드와 `parseColorMap` 헬퍼를 다음으로 교체:

```java
    @Operation(summary = "시간표 세트 내보내기",
            description = "시간표 세트의 수업 목록을 엑셀/PDF/PNG 파일로 내보냅니다. 엑셀·PNG는 요일/강의실 층/수업종류 필터를 반영하며, "
                    + "PDF는 인쇄용으로 필터와 무관하게 항상 세트 전체를 내보냅니다. 배경색은 각 수업 슬롯에 저장된 값을 그대로 사용합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "내보내기 성공"),
        @ApiResponse(responseCode = "404", description = "시간표 세트가 존재하지 않는 경우")
    })
    @GetMapping("/{timetableSetId}/export")
    public ResponseEntity<byte[]> exportTimetable(
            @PathVariable Long timetableSetId,
            @RequestParam TimetableExportFormat format,
            @RequestParam(defaultValue = "NORMAL") TimetableExportDensity density,
            @RequestParam(required = false) DayOfWeek dayOfWeek,
            @RequestParam(required = false) String floor,
            @RequestParam(required = false) ClassType classType) {
        byte[] file = exportTimetableUseCase.export(new ExportTimetableCommand(
                timetableSetId, format, density, dayOfWeek, floor, classType));

        String filename = "timetable_" + timetableSetId + "." + extension(format);
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mediaType(format)))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(file);
    }
```

(`extension(...)`/`mediaType(...)` private 메서드는 그대로 둔다.) 생성자는 Lombok `@RequiredArgsConstructor`가 `final` 필드 목록에서 자동으로 다시 만들어주므로 별도로 손댈 필요 없다.

- [ ] **Step 2: `TimetableControllerTest`에서 export 관련 테스트 갱신**

`colorCriterion`/`colorMap` 관련 4개 테스트(`exportTimetableReturns400WhenColorInvalid`, `exportTimetableReturns400WhenColorMapIsNotValidJson`, `exportTimetableReturns400WhenColorMapIsJsonNull`, `exportTimetableReturns400WhenColorMapHasNullValue`)는 통째로 삭제한다 — 색상 검증은 이제 export가 아니라 `TimetableSlotControllerTest`(Task 8에서 이미 추가) 몫이다.

`import com.academy.mudogroupware.timetable.domain.exception.InvalidTimetableColorException;`(Task 2에서 바뀐 이름)도 더 이상 안 쓰이므로 삭제한다.

남는 테스트들은 `.param("colorCriterion", ...)`/`.param("colorMap", ...)` 줄만 지운다:

```java
    @Test
    void exportTimetableReturns200WithExcelContentType() throws Exception {
        when(exportTimetableUseCase.export(any(ExportTimetableCommand.class))).thenReturn(new byte[] {1, 2, 3});

        mockMvc.perform(get("/api/timetables/1/export")
                        .param("format", "EXCEL")
                        .with(authentication(authenticatedUser())))
                .andExpect(status().isOk())
                .andExpect(content().contentType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("timetable_1.xlsx")));
    }

    @Test
    void exportTimetableAcceptsFilterAndDensityParameters() throws Exception {
        when(exportTimetableUseCase.export(any(ExportTimetableCommand.class))).thenReturn(new byte[] {1, 2, 3});

        mockMvc.perform(get("/api/timetables/1/export")
                        .param("format", "PNG")
                        .param("density", "SPACIOUS")
                        .param("dayOfWeek", "MONDAY")
                        .param("floor", "6층")
                        .param("classType", "CLASS")
                        .with(authentication(authenticatedUser())))
                .andExpect(status().isOk());
    }

    @Test
    void exportTimetableReturns404WhenNotFound() throws Exception {
        when(exportTimetableUseCase.export(any(ExportTimetableCommand.class)))
                .thenThrow(new TimetableSetNotFoundException());

        mockMvc.perform(get("/api/timetables/999/export")
                        .param("format", "PDF")
                        .with(authentication(authenticatedUser())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TIMETABLE_404_1"));
    }

    @Test
    void exportTimetableReturns400WhenFormatIsInvalidEnumValue() throws Exception {
        mockMvc.perform(get("/api/timetables/1/export")
                        .param("format", "INVALID")
                        .with(authentication(authenticatedUser())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exportTimetableReturns400WhenRequiredParameterIsMissing() throws Exception {
        mockMvc.perform(get("/api/timetables/1/export")
                        .with(authentication(authenticatedUser())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_1"));
    }

    @Test
    void exportTimetableReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/timetables/1/export")
                        .param("format", "EXCEL"))
                .andExpect(status().isUnauthorized());
    }
```

- [ ] **Step 3: 전체 timetable 패키지 테스트 실행 — 여기서 처음으로 전부 GREEN이어야 한다**

Run: `./gradlew test --tests "com.academy.mudogroupware.timetable.*"`
Expected: 전부 PASS.

- [ ] **Step 4: 커밋**

```bash
git add src/main/java/com/academy/mudogroupware/timetable/presentation/api/TimetableController.java \
        src/test/java/com/academy/mudogroupware/timetable/presentation/api/TimetableControllerTest.java
git commit -m "feat: export 엔드포인트에서 colorCriterion/colorMap 파라미터 제거"
```

---

### Task 13: 문서 갱신

**Files:**
- Modify: `src/main/java/com/academy/mudogroupware/timetable/docs/TIMETABLE_EXPORT_API.md`
- Modify: `src/main/java/com/academy/mudogroupware/timetable/docs/TIMETABLE_SLOT_API.md`
- Modify: `src/main/java/com/academy/mudogroupware/timetable/docs/BUSINESS_RULES.md`

- [ ] **Step 1: `TIMETABLE_EXPORT_API.md` 갱신**

`## 인증 및 권한` 문단을 다음으로 바꾼다(학원 스코핑 문구도 함께 정리 — `academy_id`는 스키마에서 이미 제거됨):

```markdown
## 인증 및 권한

- `Authorization: Bearer {accessToken}` 헤더가 필요하다. 인증된 사용자라면 누구나 호출할 수 있다(`TIMETABLE:MANAGE` 불필요).
```

`## Query Parameter` 표에서 `colorCriterion`/`colorMap` 행을 삭제한다. 남는 표:

```markdown
| name | type | required | description |
| --- | --- | --- | --- |
| `format` | Enum | true | `EXCEL`/`PDF`/`PNG` 중 하나 |
| `density` | Enum | false (기본값 `NORMAL`) | 행 높이·글자 크기. `COMPACT`/`NORMAL`/`SPACIOUS` |
| `dayOfWeek` | Enum | false | 특정 요일만 내보냄. 생략 시 전체 요일(`EXCEL`/`PNG`만 적용, `PDF`는 항상 전체) |
| `floor` | String | false | 특정 층(`TimetableClassroom.floor`)의 강의실만 내보냄. 생략 시 전체 층(`EXCEL`/`PNG`만 적용, `PDF`는 항상 전체) |
| `classType` | Enum | false | 특정 수업종류(`CLASS`/`SPECIAL`/`CLINIC`/`STANDING`/`EXAM`)만 내보냄. 생략 시 전체(`EXCEL`/`PNG`만 적용, `PDF`는 항상 전체) |
```

`## Error Response` 표에서 `COMMON_400_1` 조건 문구의 `colorCriterion`, `colorMap` 언급과 `TIMETABLE_400_5` 행(더 이상 이 엔드포인트에서 안 남)을 제거하고, `TIMETABLE_404_1` 조건에서 "다른 학원 소속인 경우" 문구를 뺀다:

```markdown
| HTTP 상태 | code | 발생 조건 |
| --- | --- | --- |
| `400 Bad Request` | `COMMON_400_1` | 필수 쿼리 파라미터(`format`)가 누락되었거나 enum 값이 유효하지 않은 경우 |
| `400 Bad Request` | `TIMETABLE_400_6` | 내보내기 결과 이미지(PNG)가 허용 크기를 초과하는 경우 |
| `401 Unauthorized` | `COMMON_401_1` | Access Token이 없거나 유효하지 않은 경우 |
| `404 Not Found` | `TIMETABLE_404_1` | 시간표 세트가 존재하지 않는 경우 |
| `500 Internal Server Error` | `COMMON_500_1` | 처리되지 않은 서버 오류 |
```

`## Business Rules`의 색상 관련 항목을 다음으로 바꾼다:

```markdown
- 배경색은 각 수업 슬롯 생성/수정 시점에 저장된 `color`(6자리 16진수)를 그대로 사용한다. export 시점에는 색상 정보를 따로 받지 않는다.
```

- [ ] **Step 2: `TIMETABLE_SLOT_API.md`에 `color` 필드 반영**

등록/수정 요청 예시 JSON, 목록/상세 응답 예시 JSON 전부(`subjectName` 다음 줄)에 `"color": "FFCC00"`(수정 예시는 `"color": "00AACC"`)을 추가하고, 필드 설명 표에 행을 추가한다:

```markdown
| `color` | String | true | 색상(6자리 16진수, RRGGBB) |
```

`| classType/... | - | - | 등록 API와 동일 |` 문구에도 `color`가 포함되도록 자연스럽게 이어진다(별도 문구 수정 불필요, 이미 "등록 API와 동일"이라 표현되어 있음).

- [ ] **Step 3: `BUSINESS_RULES.md` 갱신**

**색상** 항목을 다음으로 바꾼다:

```markdown
- **색상**: 수업 슬롯 생성/수정 시점에 `color`(6자리 hex, 필수)를 받아 저장한다. 형식이 6자리 16진수가 아니면 400(`TIMETABLE_400_5`, `InvalidTimetableColorException`)으로 거절한다. 내보내기(export)는 각 슬롯에 저장된 `color`를 그대로 사용하며, export 시점에 별도로 색상 정보를 받지 않는다. 백엔드는 팔레트를 계산하지 않고 프론트가 슬롯 생성/수정 시 지정한 값을 그대로 쓴다.
```

예외 목록 줄에서 `InvalidExportColorException`을 `InvalidTimetableColorException`으로 바꾼다:

```markdown
- 사용 중인 예외: `TimetableNameRequiredException`(400), `InvalidTimetablePeriodException`(400), `DuplicateClassroomCodeException`(400), `UnsupportedSlotScopeException`(400), `InvalidTimetableColorException`(400), `ExportImageTooLargeException`(400, `TIMETABLE_400_6`), `TimetableSetNotFoundException`(404), `TimetableSlotNotFoundException`(404), `ClassroomTimeConflictException`(409).
```

- [ ] **Step 4: 커밋**

```bash
git add -f src/main/java/com/academy/mudogroupware/timetable/docs/TIMETABLE_EXPORT_API.md \
        src/main/java/com/academy/mudogroupware/timetable/docs/TIMETABLE_SLOT_API.md \
        src/main/java/com/academy/mudogroupware/timetable/docs/BUSINESS_RULES.md
git commit -m "docs: 시간표 슬롯 color 필드 및 export 파라미터 변경 반영"
```

(`docs/` 하위는 보통 `git add`로 충분하지만, `docs/superpowers/`만 `.gitignore`에 걸려 있다 — `timetable/docs/`는 걸려 있지 않으므로 `-f` 없이도 되지만, 위 설계 문서 커밋과 일관되게 안전하게 `-f`를 붙여도 무해하다.)

---

### Task 14: 전체 검증

**Files:** 없음(검증 전용)

- [ ] **Step 1: 전체 유닛/슬라이스 테스트 실행**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL, 실패 0건.

- [ ] **Step 2: Flyway 빈 DB 마이그레이션 재확인**

Run: `./gradlew test --tests "com.academy.mudogroupware.FlywayFreshDatabaseMigrationTest"`
Expected: PASS.

- [ ] **Step 3: 남은 `TimetableExportColorCriterion`/`InvalidExportColorException`/`colorCriterion`/`colorMap` 참조가 없는지 전수 확인**

Run: `grep -rn "TimetableExportColorCriterion\|InvalidExportColorException\|colorCriterion\|colorMap" src/`
Expected: 결과 없음(빈 출력).

- [ ] **Step 4: 최종 커밋(있다면)**

여기까지 오면 각 태스크에서 이미 커밋을 마쳤으므로, `git status`로 남은 변경이 없는지만 확인한다.

```bash
git status --short
```

Expected: 빈 출력(모두 커밋됨).
