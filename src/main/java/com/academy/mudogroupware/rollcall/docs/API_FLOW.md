# 출결 API 흐름

> 기준일: 2026-08-06

## 출결부 조회

```text
GET /api/rollcall/lectures/{lectureId}/attendance
→ GetLectureRosterService.getRoster
→ LectureEnrollmentPort.findLecture
→ academyId 검증
→ LectureEnrollmentPort.getEnrolledStudents
→ AttendanceEntryRepository.findByLectureIdAndDate
→ 수강생 목록과 출결 기록 병합
→ RosterResponse
```

## 출결 저장

```text
PUT /api/rollcall/lectures/{lectureId}/attendance
→ SaveAttendanceEntriesRequest
→ SaveAttendanceEntriesCommand
→ SaveAttendanceEntriesService.saveEntries
→ LectureEnrollmentPort.findLecture
→ AttendanceEntry.create 또는 기존 기록 갱신
→ AttendanceEntryRepository.saveAll
→ 204 No Content
```

`ETC` 상태는 사유가 필요하다.

## 엑셀 다운로드

```text
GET /api/rollcall/lectures/{lectureId}/attendance/export
→ ExportAttendanceSheetService.exportSheet
→ GetLectureRosterUseCase.getRoster
→ Apache POI로 .xlsx 생성
→ byte[] 응답
```

## 문자 발송 후보 조회

```text
GET /api/rollcall/lectures/{lectureId}/attendance/message-candidates
→ GetMessageSendCandidatesService.getCandidates
→ GetLectureRosterUseCase.getRoster
→ status가 있는 학생만 필터
→ MessageTemplateRepository.findByAcademyIdAndStatus
→ MessageSendCandidateResponse
```

이 흐름은 실제 SMS를 보내지 않는다. “보낼 수 있는 대상과 매칭된 템플릿”을 알려주는 사전 조회다.

## 문자 템플릿 CRUD

```text
POST /api/rollcall/message-templates
→ CreateMessageTemplateService
→ 같은 status 템플릿 존재 여부 확인
→ MessageTemplateRepository.save

GET /api/rollcall/message-templates
→ MessageTemplateQueryService
→ 학원 전체 템플릿 조회

PATCH /api/rollcall/message-templates/{templateId}
→ UpdateMessageTemplateService
→ 이름/내용 수정

DELETE /api/rollcall/message-templates/{templateId}
→ DeleteMessageTemplateService
→ 학원 소속 검증 후 삭제
```

## 실제 SMS 발송이 필요한 경우

```text
출결 저장
→ 후보 조회
→ 프론트에서 발송 제외 학생 체크 해제
→ 실제 발송 API 호출
→ SmsSenderPort
→ 외부 SMS Adapter
→ 발송 결과 저장
```

마지막 네 단계는 아직 구현되지 않았다. SMS 공급자와 운영 정책이 확정된 뒤 별도 작업으로 진행한다.
