# 시간표 슬롯 색상 저장 설계

기준일: 2026-08-13

## 목표

수업 슬롯(`timetable_slot`) 생성 시점에 색상을 입력받아 문자열로 저장한다. 지금은 내보내기(export) 호출마다 프론트가 `colorCriterion`(강의실/강사 기준)과 `colorMap`을 매번 넘겨줘야 색이 반영되는데, 이걸 슬롯 자체의 영구 속성으로 옮긴다.

## 배경

현재 색상은 `GET /api/timetables/{id}/export`의 `colorCriterion`/`colorMap` 쿼리 파라미터로만 존재한다. DB에는 전혀 저장되지 않고, 매 내보내기 요청마다 프론트가 강의실 코드나 강사명 → 색상 매핑을 새로 계산해서 보낸다. 프론트에서 슬롯 생성 시점에 색을 직접 고르는 UX로 바뀌면서, 이 매핑을 매번 다시 계산해 보내는 대신 슬롯 자체에 색을 저장해두는 편이 자연스러워졌다.

## 범위

- `timetable_slot`에 `color` 컬럼(`VARCHAR(6)`, `RRGGBB`, NOT NULL)을 추가한다.
- 슬롯 생성/수정 요청에 `color`(필수)를 받는다.
- 슬롯 조회 응답(목록/상세)에 `color`를 내려준다.
- 내보내기(export) 렌더러가 슬롯에 저장된 `color`를 직접 사용하도록 바꾼다.
- `TimetableExportColorCriterion` enum과 export의 `colorCriterion`/`colorMap` 쿼리 파라미터를 제거한다.
- 색상 검증 예외를 export 전용 이름에서 일반 이름으로 바꾼다.

## 제외 범위

- 캘린더(`calendar`) 도메인 색상(`CalendarEvent.color`)은 이번 범위가 아니다 — 이미 슬롯과 별개로 자유 문자열로 저장되고 있고 이번 설계와 무관하다.
- 강의실(`TimetableClassroom`)이나 강사 단위 "기본 색상" 저장은 하지 않는다 — 슬롯마다 독립적으로 색을 가진다(합의된 방향).
- 프론트 색상 팔레트 계산/추천 로직은 이번 범위가 아니다(기존과 동일하게 백엔드는 팔레트를 계산하지 않는다).
- `timetable_slot_exception`(THIS_OCCURRENCE/FROM_NOW 단위 예외 오버라이드) 테이블에 색상 오버라이드를 추가하는 것은 이번 범위가 아니다 — 그 스코프 자체가 아직 쓰기 경로에서 미구현 상태다.

## 기준 필드

| 필드 | 필수 여부 | 설명 |
| --- | --- | --- |
| `color` | true | 6자리 16진수(`RRGGBB`, `#` 없음). 정규식 `^[0-9A-Fa-f]{6}$` |

## API 설계

### 슬롯 생성/수정

`POST /api/timetables/{timetableSetId}/slots`, `PATCH /api/timetables/{timetableSetId}/slots/{timetableSlotId}` 요청 바디에 `color` 필드를 추가한다.

```json
{
  "classType": "CLASS",
  "dayOfWeek": "MONDAY",
  "classroomCode": "601",
  "startTime": "19:00:00",
  "endTime": "21:00:00",
  "grade": "HIGH_1",
  "teacherName": "김선생",
  "subjectName": "수학",
  "color": "FFCC00"
}
```

`color`가 없거나 6자리 16진수가 아니면 `400 TIMETABLE_400_5`(`InvalidTimetableColorException`)로 거절한다.

### 슬롯 조회 응답

`TimetableSlotResponse`(목록/상세 공용)에 `color`를 추가한다.

```json
{
  "timetableSlotId": 10,
  "classType": "CLASS",
  "dayOfWeek": "MONDAY",
  "classroomCode": "601",
  "startTime": "19:00:00",
  "endTime": "21:00:00",
  "grade": "HIGH_1",
  "teacherName": "김선생",
  "subjectName": "수학",
  "color": "FFCC00"
}
```

### 내보내기(export)

`GET /api/timetables/{timetableSetId}/export`에서 `colorCriterion`, `colorMap` 쿼리 파라미터를 제거한다. 남는 파라미터: `format`(필수), `density`(선택, 기본 `NORMAL`), `dayOfWeek`/`floor`/`classType`(선택, 필터).

렌더러는 각 슬롯의 `color`를 `TimetableExportColor.fromHex(slot.color())`로 바로 변환해 셀/이미지 배경색에 적용한다. `colorMap`에 없는 값이면 흰색으로 대체하던 기존 fallback 로직은 사라진다 — 색이 항상 슬롯에 저장돼 있으므로 fallback이 필요 없다.

## 도메인 규칙

- `TimetableSlot.create()`, `applyFullUpdate()` 둘 다 `color`가 `^[0-9A-Fa-f]{6}$`를 만족하지 않으면 `InvalidTimetableColorException`을 던진다.
- 교실 시간 겹침 검사(`overlaps()`)는 색상과 무관하다 — 색은 시간/강의실 충돌 판정에 영향을 주지 않는다.
- 강의실 코드나 강사명이 같아도 슬롯마다 색이 다를 수 있다(그룹 색상 개념 폐기).

## 예외 이름 정리

- `InvalidExportColorException` → `InvalidTimetableColorException`으로 이름을 바꾼다. export 전용이 아니라 슬롯 생성/수정 시점의 일반 검증 실패이므로.
- `TimetableErrorCode.INVALID_EXPORT_COLOR` → `INVALID_COLOR`로 이름을 바꾸되 코드 값(`TIMETABLE_400_5`)은 유지한다. 메시지도 "내보내기 색상 값은..." → "색상 값은 6자리 16진수(RRGGBB)여야 합니다."로 바꾼다.
- `TimetableExportColorCriterion` enum은 삭제한다.
- `TimetableExportOptions`는 `colorCriterion`/`colors` 필드를 제거하고 `density`만 남긴다. `colorFor(classroomCode, teacherName)` 메서드도 삭제한다.
- `ExportTimetableCommand`에서 `colorCriterion`, `colorHexByGroupValue` 필드를 제거한다.

## 다른 도메인 영향

- 없음. 이 변경은 `timetable` 도메인 내부(슬롯 생성/수정/조회/내보내기)로 닫혀 있다. 다른 도메인이 `TimetableSlot`이나 export 관련 클래스를 참조하는 코드는 없다(포트/어댑터로 노출된 적 없음).

## 마이그레이션

`src/main/resources/db/migration/be5/V5.1.12__add_color_to_timetable_slot.sql` (be5 폴더가 지금까지 시간표 슬롯 마이그레이션을 담당해왔으므로 이어서 사용).

```sql
ALTER TABLE timetable_slot ADD COLUMN color VARCHAR(6) NOT NULL DEFAULT 'FFFFFF';
ALTER TABLE timetable_slot ALTER COLUMN color DROP DEFAULT;
```

기존 슬롯(이 기능 이전에 생성된 row)은 전부 `FFFFFF`(흰색)로 백필된다. `DROP DEFAULT`는 이후 INSERT가 항상 애플리케이션에서 명시적으로 값을 채우도록 강제하기 위함이다(DB 기본값에 의존하지 않음).

기존 baseline/be4/be6/be7 파일은 수정하지 않는다.

## 테스트 전략

- `TimetableSlot` 도메인 테스트: `color`가 6자리 16진수가 아니면 `create()`/`applyFullUpdate()`가 `InvalidTimetableColorException`을 던진다. 유효한 값이면 정상 생성/수정된다.
- `CreateTimetableSlotRequest`/`UpdateTimetableSlotRequest` 검증 테스트: `color` 누락·형식 오류 시 `400 TIMETABLE_400_5`.
- `CreateTimetableSlotServiceTest`/`UpdateTimetableSlotServiceTest`: 저장된 슬롯이 요청받은 `color`를 그대로 가진다.
- `TimetableSlotPersistenceAdapter` 관련 테스트: `color`가 저장/복원된다.
- `GetTimetableSlotsServiceTest`/`GetTimetableSlotServiceTest`: 응답에 `color`가 포함된다.
- `ExportTimetableServiceTest`/각 렌더러 테스트: `colorMap` 없이도 슬롯의 `color`가 그대로 렌더링 결과에 반영된다. 기존 `colorCriterion`/`colorMap` 기반 테스트는 삭제하거나 새 방식으로 다시 작성한다.
- `TimetableController` MockMvc 테스트: export 요청에서 `colorCriterion`/`colorMap` 파라미터가 더 이상 필요 없음을 확인.
- 마이그레이션 테스트: `FlywayFreshDatabaseMigrationTest`(Testcontainers)로 빈 DB에서 전체 마이그레이션이 통과하고, 기존 슬롯이 있는 상태에서 컬럼 추가 시 `FFFFFF` 백필이 되는지 확인.

## 성공 기준

- 슬롯 생성/수정 API가 `color`를 필수로 받고, 형식이 틀리면 `400 TIMETABLE_400_5`로 거절한다.
- 슬롯 조회 응답에 `color`가 포함된다.
- 내보내기 API가 `colorCriterion`/`colorMap` 없이도 슬롯별로 저장된 색을 그대로 반영해 Excel/PDF/PNG를 만든다.
- 기존 슬롯 데이터는 마이그레이션 이후에도 깨지지 않고 `FFFFFF` 기본색으로 조회/내보내기된다.
- 전체 Flyway 마이그레이션과 기존 테스트 스위트가 깨지지 않는다.
