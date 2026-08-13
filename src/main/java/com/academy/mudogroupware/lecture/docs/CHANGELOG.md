# lecture Changelog

## 2026-08-13

- 강의 목록 조회 필터를 프론트 화면 값 기준(`subjectName`, `teacherName`, `classroomCode`)으로 변경했다.

- 강의 수정 API(`PATCH /api/lectures/{lectureId}`)를 추가했다.
- 강의 삭제 API(`DELETE /api/lectures/{lectureId}`)를 추가했다.
- 강의 삭제는 `deleted_at` 기반 소프트 삭제로 처리하며, 조회/충돌 검사/매출 리포트 조회에서 삭제된 강의를 제외한다.
- 강의 수정 시 자기 자신을 제외하고 교실·요일·시간대 충돌을 검사한다.

## 2026-08-11

- 강의 등록 요청을 캘린더 슬롯 형태(`classType`, `dayOfWeek`, `classroomCode`, `startTime`, `endTime`)로 변경했다.
- 강의 등록은 `teacherId` 대신 `teacherName` 중심으로 받으며, `grade`, `teacherName`, `subjectName`, `termName`, `feeType`, `feeAmount`는 선택값으로 허용한다.
- 강의 목록/상세 응답에 `classType`, `classroomCode`를 추가했다.
- 저장된 `teacherName`, `subjectName`, `classroomCode`를 우선 사용하고 기존 ID 기반 조회는 fallback으로 유지한다.

## 2026-08-06

- 강의 목록/상세 응답에 `teacherName`을 추가했다.
- `TeacherDirectoryPort`를 lecture에 정의하고 users 모듈 Adapter로 `teacherId -> teacherName`을 조회하도록 연결했다.
- API/API_FLOW/README/REVISION 문서를 실제 구현 기준으로 갱신했다.
