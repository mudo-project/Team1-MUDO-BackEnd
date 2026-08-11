# lecture Changelog

## 2026-08-11

- 강의 등록 요청을 캘린더 슬롯 형태(`classType`, `dayOfWeek`, `classroomCode`, `startTime`, `endTime`)로 변경했다.
- 강의 등록은 `teacherId` 대신 `teacherName` 중심으로 받으며, `grade`, `teacherName`, `subjectName`, `termName`, `feeType`, `feeAmount`는 선택값으로 허용한다.
- 강의 목록/상세 응답에 `classType`, `classroomCode`를 추가했다.
- 저장된 `teacherName`, `subjectName`, `classroomCode`를 우선 사용하고 기존 ID 기반 조회는 fallback으로 유지한다.

## 2026-08-06

- 강의 목록/상세 응답에 `teacherName`을 추가했다.
- `TeacherDirectoryPort`를 lecture에 정의하고 users 모듈 Adapter로 `teacherId -> teacherName`을 조회하도록 연결했다.
- API/API_FLOW/README/REVISION 문서를 실제 구현 기준으로 갱신했다.
