# lecture Revision

## 2026-08-13 · 강의 수정/삭제 추가

### 배경

강의 관리 화면에서 생성과 조회만 가능하면 운영 중 시간표 변경, 담당 선생님 변경, 폐강 처리 같은 기본 유지보수 흐름을 처리할 수 없었다. `LECTURE:MANAGE` 권한의 의미에도 강의 수정이 포함되어 있어 실제 API를 맞췄다.

### 변경

- `PATCH /api/lectures/{lectureId}` 강의 수정 API 추가.
- `DELETE /api/lectures/{lectureId}` 강의 삭제 API 추가.
- 수정 시 기존 강의의 생성 시각과 id를 유지하면서 강의 기본 정보와 스케줄을 교체한다.
- 수정 시간 충돌 검사는 자기 자신을 제외하고 확인한다.
- 강의 삭제는 `lecture.deleted_at`을 채우는 소프트 삭제로 처리한다.
- 목록/상세/id 목록 조회, 시간 충돌 검사, 매출 리포트용 강의 조회에서 삭제된 강의를 제외한다.

### 검증

- `UpdateLectureServiceTest`: 정상 수정, 존재하지 않는 강의, 다른 강의와 시간 충돌 케이스를 검증한다.
- `DeleteLectureServiceTest`: 정상 삭제, 존재하지 않는 강의 케이스를 검증한다.
- `UpdateLectureRequestTest`: 수정 요청 DTO가 명령 객체로 변환되는지 검증한다.
- `LectureRepositoryImplDataJpaTest`: 기존 강의 수정, 스케줄 교체, 소프트 삭제 후 조회/충돌 검사 제외를 검증한다.

## 2026-08-06 · teacherName 응답 연결

### 배경

강의 목록/상세 화면에서 담당 선생님 이름이 필요했지만 기존 응답은 `teacherId`만 제공했다. lecture가 users 테이블을 직접 조회하면 모듈 경계를 깨기 때문에, lecture가 조회 Port를 정의하고 users가 Adapter를 구현하는 방식으로 연결했다.

### 변경

- `TeacherDirectoryPort`, `TeacherInfo` 추가.
- users 모듈에 `LectureTeacherDirectoryAdapter` 추가.
- `LectureQueryService`에서 목록/상세 조회 시 `teacherName` 보강.
- `LectureSummaryView`, `LectureDetailView`, `LectureSummaryResponse`, `LectureDetailResponse`에 `teacherName` 추가.

### 검증

- `LectureQueryServiceTest`에서 목록/상세 `teacherName` 보강을 검증한다.
- `LectureTeacherDirectoryAdapterTest`에서 academyId 범위 필터링과 빈 목록 처리를 검증한다.

## 2026-08-06 · 문서 실제 구현 기준 갱신

- 강의 등록/목록/상세 API를 실제 Controller/DTO 기준으로 정리했다.
- 교실관리 탭은 별도 기능으로 두지 않고 강의 등록 흐름에서 교실명을 생성/재사용하는 것으로 정리했다.
- 수강료는 `feeType`, `feeAmount`로 회차별/월별 정책을 표현한다.
