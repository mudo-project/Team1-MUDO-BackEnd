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

## 출결 안내 문자 발송

```text
POST /api/rollcall/lectures/{lectureId}/attendance/message-candidates/send
→ SendAttendanceMessagesService.send
→ GetMessageSendCandidatesUseCase.getCandidates (eligible 여부 확인)
→ (학생별) AttendanceMessageSendRecordRepository.createOrGetExisting(lectureId, studentId, date, candidate.status())
  → 이미 SENT면 SOLAPI 호출 없이 그 결과 반환
  → INDETERMINATE면 SOLAPI 호출 없이 "자동 재발송 차단" 응답(관리자 확인 필요)
→ AttendanceMessageSendRecordRepository.claimForSending(id)
  → PENDING/FAILED일 때만 SENDING으로 원자적 전환 — 동시 요청 중 하나만 통과
  → 실패하면(다른 요청이 이미 가져감) SOLAPI 호출 없이 "처리 중이거나 이미 처리됨" 응답
→ SmsSenderPort.send → SolapiSmsAdapter
  → 응답 명확(성공/실패) 또는 ResourceAccessException(INDETERMINATE)
→ AttendanceMessageSendRecord.markResult + repository.save
→ MessageSendResultView(학생별 성공/실패), 실제로 새로 발송한 건수만 사용량 집계
```

같은 (강의, 학생, 출결 날짜, 출결 상태)로 재요청이 와도 이미 발송 성공(`SENT`)한 건 중복 발송하지 않는다(이슈 #354, 2026-08-14). 출결 상태가 정정되면(예: 결석→지각) 새 조합으로 취급해 재발송을 막지 않는다.
