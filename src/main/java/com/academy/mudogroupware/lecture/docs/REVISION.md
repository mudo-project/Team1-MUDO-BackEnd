# lecture Revision

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
