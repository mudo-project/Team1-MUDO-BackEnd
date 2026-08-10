# rollcall 모듈

> 강의별 출결부와 출결 상태별 문자 템플릿 관리 백엔드 구현 기준 문서다. 실제 SMS 발송은 알리고(Aligo) API로 구현되어 있다(2026-08-10).

## 책임과 범위

- **AttendanceEntry(출결 기록)**: 특정 강의, 특정 날짜, 특정 학생의 출결 상태와 비고를 저장한다.
- **AttendanceStatus(출결 상태)**: `PRESENT`, `ABSENT`, `LATE`, `ONLINE`, `ETC`.
- **MessageTemplate(문자 템플릿)**: 출결 상태별 학부모 안내 문자 템플릿이다. 학원 단위로 상태 1개당 1개만 만들 수 있다.
- **출결부 조회**: lecture/student 연동 Port로 강의와 수강생을 가져와 출결 기록과 합쳐 보여준다.
- **문자 발송 대상 후보 조회**: 출결 상태에 맞는 템플릿이 있는 학생을 `eligible=true`로 표시한다.
- **문자 발송**: 선택한 학생들에게 알리고(Aligo) API로 실제 SMS를 발송하고, 학생별 성공/실패를 반환한다.

## 담당자

(팀 확인 필요)

## 소유하는 주요 데이터와 상태

| 엔티티 | 주요 필드 | 비고 |
|---|---|---|
| `AttendanceEntry` | `academyId`, `lectureId`, `studentId`, `date`, `status`, `note` | 강의·학생·날짜 단위 출결 기록. |
| `MessageTemplate` | `academyId`, `name`, `status`, `content`, `createdBy`, `createdAt`, `updatedAt` | 출결 상태별 템플릿. |

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

`SmsSenderPort`(application/port)로 추상화하고, `AligoSmsAdapter`(infrastructure/external/aligo)가 알리고(Aligo) REST API(`POST https://apis.aligo.in/send/`)로 구현한다. 학생 1명당 API 호출 1건이며(배치 발송 아님), 호출 실패는 예외 대신 `SmsSendResult.failed(reason)`으로 반환해 다른 학생 발송에 영향을 주지 않는다.

- 발신번호(`ALIGO_SENDER_NUMBER`)는 알리고 사이트에 사전 등록이 필요하다.
- 발송 이력 저장, 실패 자동 재시도, 과금 정책은 아직 없다(향후 필요해지면 추가).

## 발행·소비하는 Event

- 현재 발행하는 Event는 없다. 발송이 동기 API 호출 안에서 순차 처리되므로 아직 이벤트가 필요 없다.
- 발송 대상이 많아져 비동기 처리가 필요해지면 `AttendanceMessageSendRequestedEvent` 같은 이벤트를 검토할 수 있다.

## 변경 시 주의 사항

- `ETC` 상태는 비고가 필수다.
- 같은 출결 상태의 문자 템플릿은 학원당 1개만 허용한다.
- "문자 발송 대상 조회"(eligible 표시)와 "실제 발송"(`POST .../message-candidates/send`)은 별개의 API다 — 후자를 호출해야 실제로 문자가 나간다.
- `ROLLCALL:MANAGE` 권한은 출결관리 탭 접근, 출결부 조회, 출결 저장/수정, 엑셀 다운로드, 문자 발송 대상 후보 조회에 적용되어 있다.
- `ROLLCALL:TEMPLATE_MANAGE` 권한은 문자 템플릿 생성/수정/삭제에 적용되어 있다. 템플릿 목록 조회는 출결 담당자도 문구를 확인할 수 있도록 `ROLLCALL:MANAGE` 또는 `ROLLCALL:TEMPLATE_MANAGE` 중 하나가 있으면 허용한다.

## 화면 구성 참고

```text
출결
├─ 강의 출결부
└─ 문자 템플릿
```

강의 출결부 페이지에서는 날짜/요일 필터 근처에 문자 템플릿 탭 또는 버튼을 둔다. 체크한 학생을 기준으로 발송 대상 후보를 조회하고, 실제 SMS 발송은 공급자 확정 후 별도 작업으로 연결한다.

## 세부 문서

- [API.md](API.md)
- [API_FLOW.md](API_FLOW.md)
- [REVISION.md](REVISION.md)
- [CHANGELOG.md](CHANGELOG.md)
