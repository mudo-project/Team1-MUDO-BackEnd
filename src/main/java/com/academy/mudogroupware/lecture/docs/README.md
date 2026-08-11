# lecture 모듈

강의 등록/조회 백엔드 구현 기준 문서다. 강의는 학원 단위의 강의 종류, 학년, 시수, 과목, 담당 선생님, 교실, 수강료, 요일/시간표를 관리한다.

## 책임과 범위

- `Lecture`: 강의 본체. `classType`, `classroomCode`, `teacherName`, `subjectName`을 저장하며, 기존 연동용 `teacherId` 등 ID 필드는 선택값으로 유지한다.
- `Term`: 시수/학기/기간 이름. 강의 등록 시 `termName`이 있으면 생성/재사용한다.
- `Subject`: 과목 이름. 강의 등록 시 `subjectName`이 있으면 생성/재사용한다.
- `Classroom`: 교실 이름. 별도 교실관리 탭 없이 강의 등록 흐름에서 `classroomCode` 기준으로 자동 생성/재사용한다.
- `LectureSchedule`: 강의의 요일/시간 범위.
- `feeType`, `feeAmount`: 회차별/월별 수강료 정책.

## 공개 UseCase

- `CreateLectureUseCase`: 강의 등록.
- `LectureQueryUseCase`: 강의 목록/상세 조회.

## 다른 모듈 연동

- student: `EnrolledStudentsPort`로 강의 상세의 수강 학생 목록을 조회한다.
- users: `TeacherDirectoryPort`를 users 모듈의 `LectureTeacherDirectoryAdapter`가 구현한다. lecture는 users Entity/Repository를 직접 보지 않고, 저장된 `teacherName`이 없고 `teacherId`가 있을 때만 `teacherId -> teacherName`을 fallback 조회한다.

## API 응답 상태

- 강의 목록/상세 응답은 `classType`, `classroomCode`, `teacherId`, `teacherName`을 함께 내려준다.
- 선생님이 다른 학원 소속이거나 존재하지 않으면 `teacherName`은 `null`이 될 수 있다. 강의 생성 단계의 활성 사용자 검증은 별도 정책이 확정되면 추가한다.
- 학생 목록은 student 모듈의 수강 등록 데이터가 기준이다.

## 권한 정책

- `LECTURE:READ`: 강의관리 탭 접근, 강의 목록/상세 조회.
- `LECTURE:MANAGE`: 강의 등록, 강의 수정, 강의 가격 설정. 강의 목록/상세 조회도 함께 허용한다.

## 주의사항

- 같은 `classroomCode`, 같은 요일, 겹치는 시간대에 이미 강의가 있으면 등록을 막는다.
- lecture에서 users/file/student 테이블을 직접 매핑하지 않는다.
- 실제 결제, 환불, POS 연동은 student/payment 후속 범위다.

## 문서

- [API.md](API.md)
- [API_FLOW.md](API_FLOW.md)
- [REVISION.md](REVISION.md)
- [CHANGELOG.md](CHANGELOG.md)
