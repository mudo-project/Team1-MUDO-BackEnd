# Lecture Timetable Field Alignment 설계

기준일: 2026-08-11

## 목표

강의 관리(`lecture`)의 강의 등록 필드를 시간표/캘린더 화면에서 쓰는 수업 슬롯 필드와 맞춘다. 등록 입력은 `teacherId` 중심에서 `teacherName` 중심으로 바꾸고, 꼭 필요한 값 외에는 nullable을 허용해 초안 작성과 수기 등록을 유연하게 만든다.

## 배경

현재 `lecture`는 `teacherId`, `subjectId`, `classroomId`, `termId`처럼 마스터 ID 기반으로 강의를 저장한다. 반면 시간표 슬롯(`timetable_slot`)은 화면 입력에 가까운 `classType`, `classroomCode`, `teacherName`, `subjectName` 문자열 중심이다.

이 차이 때문에 캘린더/시간표에서 관리하는 강의 정보와 강의 관리에서 등록하는 강의 정보가 다르게 보인다. `timetable/docs/README.md`에도 `lecture`와 `timetable`의 데이터 모델 중복이 명시되어 있으므로, 이번 작업에서는 `lecture`를 시간표 슬롯 필드에 맞추되 도메인 전체 통합은 하지 않는다.

## 범위

- `lecture` 등록 요청에 시간표 슬롯 기준 필드를 추가한다.
- 강의 등록 입력에서 `teacherId` 대신 `teacherName`을 중심으로 사용한다.
- `lecture` 저장 모델에 `classType`, `teacherName`, `classroomCode`를 추가한다.
- 기존 강의 전용 필드(`name`, `termName`, `feeType`, `feeAmount`, 수강생 목록, 학생 수 카운트)는 유지한다.
- 필수 검증은 실제 저장과 충돌 검사에 필요한 값 위주로 줄인다.
- 기존 `dataimport` 등 다른 도메인 호출이 즉시 깨지지 않도록 호환 경로를 둔다.

## 제외 범위

- `lecture`와 `timetable` 테이블 통합
- `timetable_slot`에서 자동으로 `lecture`를 생성하거나 반대로 동기화하는 기능
- 강의 수정/삭제 API 신규 구현
- `teacherName`으로 users 도메인의 실제 사용자 계정을 자동 매칭하는 기능
- 기존 마스터 테이블(`term`, `subject`, `classroom`) 삭제

## 기준 필드

시간표 슬롯 기준 강의 필드는 다음과 같다.

| 필드 | 필수 여부 | 설명 |
| --- | --- | --- |
| `classType` | true | 수업 종류. `CLASS`, `SPECIAL`, `CLINIC`, `STANDING`, `EXAM` |
| `dayOfWeek` | true | 요일 |
| `classroomCode` | true | 강의실 코드. 시간 겹침 검사 기준 |
| `startTime` | true | 시작 시각 |
| `endTime` | true | 종료 시각 |
| `grade` | false | 학년. 기존 강의 관리 필드지만 유연하게 nullable 허용 |
| `teacherName` | false | 강사명. `teacherId` 대신 등록 중심 필드로 사용 |
| `subjectName` | false | 과목명 |

강의 관리 전용으로 유지할 필드는 다음과 같다.

| 필드 | 필수 여부 | 설명 |
| --- | --- | --- |
| `name` | true | 강의명. 강의 목록/상세와 수강 등록에서 쓰는 대표 이름 |
| `termName` | false | 학기/시즌 이름 |
| `feeType` | false | 수강료 유형 |
| `feeAmount` | false | 수강료 금액 |

## API 설계

`POST /api/lectures` 요청은 시간표 슬롯 형태를 기본으로 받는다.

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

기존 `schedules` 배열은 더 이상 신규 등록 요청의 기본 형태로 쓰지 않는다. 단, 데이터 가져오기와 기존 내부 호출을 위해 `CreateLectureCommand`에는 호환 생성 또는 변환 경로를 둔다.

## 저장 설계

`lecture` 테이블에 다음 컬럼을 추가한다.

- `class_type VARCHAR(20) NULL`
- `teacher_name VARCHAR(50) NULL`
- `classroom_code VARCHAR(50) NULL`

기존 컬럼은 즉시 삭제하지 않고 nullable로 완화한다.

- `grade`
- `term_id`
- `subject_id`
- `teacher_id`
- `classroom_id`

이 방식은 기존 데이터, 학생 수강 조회, 데이터 가져오기 흐름을 보존하면서 새 등록 입력을 받을 수 있게 한다.

## 도메인 규칙

- `name`은 공백일 수 없다.
- `classType`, `dayOfWeek`, `classroomCode`, `startTime`, `endTime`은 필수다.
- `startTime`은 `endTime`보다 빨라야 한다.
- `grade`, `teacherName`, `subjectName`, `termName`, `feeType`, `feeAmount`는 nullable을 허용한다.
- 교실 시간 충돌 검사는 새 기준인 `classroomCode + dayOfWeek + startTime/endTime`으로 수행한다.
- 새 등록 요청은 `classroomCode`가 필수이므로 새 충돌 검사는 항상 `classroomCode` 기준으로 수행한다. 기존 데이터 중 `classroomCode`가 없는 행은 조회 응답에서만 기존 `classroomId -> classroomName`을 fallback으로 사용한다.

## 조회 응답

목록/상세 응답은 기존 클라이언트 호환을 위해 `teacherId`를 당장 제거하지 않는다. 다만 `teacherName`은 저장된 문자열을 우선 사용하고, 값이 없고 `teacherId`가 있으면 기존 `TeacherDirectoryPort` 조회 결과를 fallback으로 사용한다.

`classroomName`은 기존 응답 호환을 위해 유지하되, 새 데이터는 `classroomCode`를 함께 내려준다. `subjectName`은 저장 문자열을 우선 사용하고, 없으면 기존 `subjectId` 기반 이름을 fallback으로 사용한다.

## 다른 도메인 영향

- `dataimport`: 현재 `CreateLectureUseCase`를 직접 호출한다. 즉시 깨지지 않게 기존 필드를 받는 호환 경로를 유지한다. 이후 별도 작업에서 import 초안도 `teacherName` 중심 검증으로 완화할 수 있다.
- `student`: `LectureCatalogPortAdapter`는 수강 강의 이름/강사명/가격/시간표 텍스트를 조회한다. `teacherName`은 저장 문자열을 우선 사용하도록 변경한다.
- `users`: 새 등록 흐름은 users 도메인의 사용자 ID를 필요로 하지 않는다. 기존 `TeacherDirectoryPort`는 과거 데이터 fallback 전용으로 남는다.
- `timetable`: 직접 수정하지 않는다. `timetable`의 도메인 모델에서 DB와 달리 `grade`를 필수로 막고 있는 부분은 별도 후속 작업으로 다룬다.

## 마이그레이션

새 Flyway 파일은 `src/main/resources/db/migration/be1/`에 추가한다. 기존 `be1` 최신 파일 뒤 버전을 사용한다.

작업 내용:

- `lecture`에 `class_type`, `teacher_name`, `classroom_code` 추가
- `lecture.grade`, `lecture.term_id`, `lecture.subject_id`, `lecture.teacher_id`, `lecture.classroom_id` nullable 변경
- 기존 foreign key는 컬럼 nullable 변경 후 유지한다

기존 baseline 파일은 수정하지 않는다.

## 테스트 전략

- `CreateLectureRequest` 검증 테스트: 필수값만 있으면 등록 요청이 통과하고, 선택값은 null 허용한다.
- `CreateLectureServiceTest`: `teacherName` 중심 Command로 저장되는 강의가 생성된다.
- `LectureRepositoryImpl` 관련 테스트: 새 컬럼이 저장/복원된다.
- 목록/상세 조회 테스트: 저장된 `teacherName`, `classroomCode`, `subjectName`이 우선 응답된다.
- 회귀 테스트: 기존 `teacherId` 기반 데이터가 있을 때도 `TeacherDirectoryPort` fallback으로 조회가 깨지지 않는다.
- 마이그레이션 테스트: 빈 DB에서 전체 Flyway 마이그레이션이 통과한다.

## 성공 기준

- 강의 등록 API가 시간표 슬롯 기준 필드를 받을 수 있다.
- `teacherId` 없이 `teacherName`만으로 강의를 등록할 수 있다.
- 필수값을 제외한 강의 부가 정보는 null이어도 거절되지 않는다.
- 기존 수강 조회와 데이터 가져오기 흐름은 컴파일과 테스트에서 깨지지 않는다.
