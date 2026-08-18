# rollcall 모듈

> 강의별 출결부와 출결 상태별 문자 템플릿 관리 백엔드 구현 기준 문서다. 실제 SMS 발송은 솔라피(SOLAPI) API로 구현되어 있다(2026-08-10).

## 책임과 범위

- **AttendanceEntry(출결 기록)**: 특정 강의, 특정 날짜, 특정 학생의 출결 상태와 비고를 저장한다.
- **AttendanceStatus(출결 상태)**: `PRESENT`, `ABSENT`, `LATE`, `ONLINE`, `ETC`.
- **MessageTemplate(문자 템플릿)**: 출결 상태별 학부모 안내 문자 템플릿이다. 학원 단위로 상태 1개당 1개만 만들 수 있다.
- **출결부 조회**: lecture/student 연동 Port로 강의와 수강생을 가져와 출결 기록과 합쳐 보여준다.
- **문자 발송 대상 후보 조회**: 출결 상태에 맞는 템플릿이 있는 학생을 `eligible=true`로 표시한다.
- **문자 발송**: 선택한 학생들에게 솔라피(SOLAPI) API로 실제 SMS를 발송하고, 학생별 성공/실패를 반환한다. 발송 직전에 템플릿 변수 `{학생명}`, `{강의명}`, `{날짜}`를 실제 값으로 치환한다.

## 담당자

(팀 확인 필요)

## 소유하는 주요 데이터와 상태

| 엔티티 | 주요 필드 | 비고 |
|---|---|---|
| `AttendanceEntry` | `academyId`, `lectureId`, `studentId`, `date`, `status`, `note` | 강의·학생·날짜 단위 출결 기록. |
| `MessageTemplate` | `academyId`, `name`, `status`, `content`, `createdBy`, `createdAt`, `updatedAt` | 출결 상태별 템플릿. |
| `AttendanceMessageSendRecord` | `lectureId`, `studentId`, `date`, `attendanceStatus`, `status`(`PENDING`/`SENDING`/`SENT`/`FAILED`/`INDETERMINATE`), `failureReason` | 강의·학생·출결날짜·출결상태 단위 SMS 발송 시도 기록. 재시도 시 중복 발송을 막는 데 쓰인다(`lectureId`+`studentId`+`date`+`attendanceStatus` 유니크) — 출결 상태가 정정되면(예: 결석→지각) 새 조합으로 취급해 재발송을 막지 않는다. `SENDING`은 "지금 이 요청이 발송 권한을 가져갔다"는 원자적 표시로, 동시 요청 중 하나만 실제 SOLAPI 호출까지 진행하게 한다. |

## 외부에 공개하는 Application API

- `GetLectureRosterUseCase` — 강의 출결부 조회.
- `SaveAttendanceEntriesUseCase` — 학생별 출결 상태 일괄 저장.
- `ExportAttendanceSheetUseCase` — 출결부 엑셀 다운로드.
- `GetMessageSendCandidatesUseCase` — 문자 발송 후보 조회.
- `SendAttendanceMessagesUseCase` — 선택한 학생에게 출결 안내 문자 발송(학생별 성공/실패 반환).
- `CreateMessageTemplateUseCase` / `MessageTemplateQueryUseCase` / `UpdateMessageTemplateUseCase` / `DeleteMessageTemplateUseCase` — 문자 템플릿 CRUD.

## 다른 모듈 또는 외부 시스템에 요청하는 의존성

### lecture/student 연동

- rollcall은 `LectureEnrollmentPort`를 통해 강의 정보와 수강생 목록을 조회한다.
- 출결 기록은 rollcall이 소유하지만, 강의와 수강생의 원천 데이터는 lecture/student 쪽에 있다.

### SMS 외부 공급자

`SmsSenderPort`(application/port)로 추상화하고, `SolapiSmsAdapter`(infrastructure/external/solapi)가 솔라피(SOLAPI) REST API(`POST https://api.solapi.com/messages/v4/send`, HMAC-SHA256 인증)로 구현한다. 학생 1명당 API 호출 1건이며(배치 발송 아님), 호출 실패는 예외 대신 `SmsSendResult.failed(reason)`으로 반환해 다른 학생 발송에 영향을 주지 않는다.

- 발신번호(`SOLAPI_SENDER_NUMBER`)는 솔라피 사이트에 사전 등록이 필요하다.
- 개인 계정은 사업자 인증 없이 바로 API Key를 발급받을 수 있지만 일일 발송량이 50~500건으로 제한된다(사업자 계정은 1,000건 이상).
- 학생별 발송 시도는 `AttendanceMessageSendRecordRepository`에 저장된다(2026-08-14) — 같은 (강의, 학생, 출결 날짜, 출결 상태)로 이미 발송 성공했으면 재요청이 와도 SOLAPI를 다시 호출하지 않는다. 동시 요청은 `SENDING` 상태로의 조건부 전환(`claimForSending`)을 통해 정확히 하나만 실제 호출까지 진행한다.
- 응답을 못 받은 경우(타임아웃/연결 끊김)는 `INDETERMINATE`로 기록하고 **자동 재시도를 차단**한다 — 실제로 발송됐을 수도 있는 상태라 다시 호출하면 중복 발송 위험이 있기 때문이다. 관리자가 확인 후 강제로 재발송할 수 있는 절차/API는 아직 없다. 실패 자동 재시도 스케줄링, 과금 정책도 아직 없다.
- 학생별 SOLAPI 호출은 전용 스레드풀(`rollcallSmsExecutor`, `RollcallSmsExecutorConfig`)로 병렬 처리한다(2026-08-18) — 요청 안의 학생 수만큼 순차 호출 시간이 누적되던 문제를 해소한다. 기본 core 4 / max 6 / queue 50(`rollcall.sms.executor.*`로 조정 가능)으로, SOLAPI 발송 API 기본 한도(5초당 100건)를 응답시간 200~300ms 가정 기준으로 역산한 값이다. 학생별 발송 기록은 `claimForSending` 조건부 UPDATE로 이미 동시성이 보장돼 병렬화해도 중복 발송 위험이 없다.

## 발행·소비하는 Event

- 현재 발행하는 Event는 없다. 학생별 SOLAPI 호출은 전용 스레드풀로 병렬 처리하지만(위 참고), API 응답은 여전히 전체 발송이 끝난 뒤 한 번에 반환하는 동기 방식이라 별도 이벤트가 필요 없다.
- 발송 자체를 비동기 API(즉시 202 응답 후 완료 통지)로 바꿀 필요가 생기면 `AttendanceMessageSendRequestedEvent` 같은 이벤트를 검토할 수 있다.

## 변경 시 주의 사항

- `ETC` 상태는 비고가 필수다.
- 같은 출결 상태의 문자 템플릿은 학원당 1개만 허용한다.
- "문자 발송 대상 조회"(eligible 표시)와 "실제 발송"(`POST .../message-candidates/send`)은 별개의 API다 — 후자를 호출해야 실제로 문자가 나간다.
- 문자 템플릿 본문의 `{학생명}`, `{강의명}`, `{날짜}`는 실제 SMS 발송 직전에만 치환된다. 목록 조회에서는 저장된 템플릿 원문을 그대로 보여준다.
- `ROLLCALL:MANAGE` 권한은 출결관리 탭 접근, 출결부 조회, 출결 저장/수정, 엑셀 다운로드, 문자 발송 대상 후보 조회, 실제 문자 발송(`POST .../message-candidates/send`)에 적용되어 있다.
- `ROLLCALL:TEMPLATE_MANAGE` 권한은 문자 템플릿 생성/수정/삭제에 적용되어 있다. 템플릿 목록 조회는 출결 담당자도 문구를 확인할 수 있도록 `ROLLCALL:MANAGE` 또는 `ROLLCALL:TEMPLATE_MANAGE` 중 하나가 있으면 허용한다.

## 데이터 생명주기 정책

- 출결 기록은 학생·학부모 응대와 운영 증빙에 필요한 기본 기록으로 보고, 플랜에 따라 과거 출결 보관 여부를 나누지 않는다.
- 플랜 차등은 보관기간보다 SMS 발송량, 엑셀 다운로드, 리포트 같은 사용량·편의 기능에 둔다.
- 현재 구현은 강의·학생·날짜 단위 출결 기록을 저장하고, 문자 발송 성공 건수는 `resource_usage_event`에 `SMS` 사용량으로 기록한다.
- 후속 구현에서는 오래된 출결을 일반 조회와 분리한 아카이브 기준으로 관리한다.
- 담당 도메인 기준은 [DATA_LIFECYCLE_POLICY.md](../../../../../../../../docs/DATA_LIFECYCLE_POLICY.md)를 따른다.

## 화면 구성 참고

```text
출결
├─ 강의 출결부
└─ 문자 템플릿
```

강의 출결부 페이지에서는 날짜/요일 필터 근처에 문자 템플릿 탭 또는 버튼을 둔다. 체크한 학생을 기준으로 발송 대상 후보를 조회하고, "문자 보내기" 버튼으로 솔라피(SOLAPI) API를 통해 실제 SMS를 발송한다.

## 세부 문서

- [API.md](API.md)
- [API_FLOW.md](API_FLOW.md)
- [REVISION.md](REVISION.md)
- [CHANGELOG.md](CHANGELOG.md)
