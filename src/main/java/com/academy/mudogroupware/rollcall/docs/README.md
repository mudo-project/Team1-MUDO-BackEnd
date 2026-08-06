# rollcall 모듈

> 강의별 출결부와 출결 상태별 문자 템플릿 관리 백엔드 구현 기준 문서다. 현재 실제 SMS 외부 발송은 구현되어 있지 않으며, 템플릿 관리와 발송 대상 후보 조회까지만 제공한다.

## 책임과 범위

- **AttendanceEntry(출결 기록)**: 특정 강의, 특정 날짜, 특정 학생의 출결 상태와 비고를 저장한다.
- **AttendanceStatus(출결 상태)**: `PRESENT`, `ABSENT`, `LATE`, `ONLINE`, `ETC`.
- **MessageTemplate(문자 템플릿)**: 출결 상태별 학부모 안내 문자 템플릿이다. 학원 단위로 상태 1개당 1개만 만들 수 있다.
- **출결부 조회**: lecture/student 연동 Port로 강의와 수강생을 가져와 출결 기록과 합쳐 보여준다.
- **문자 발송 대상 후보 조회**: 출결 상태에 맞는 템플릿이 있는 학생을 `eligible=true`로 표시한다.

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
- `CreateMessageTemplateUseCase` / `MessageTemplateQueryUseCase` / `UpdateMessageTemplateUseCase` / `DeleteMessageTemplateUseCase` — 문자 템플릿 CRUD.

## 다른 모듈 또는 외부 시스템에 요청하는 의존성

### lecture/student 연동

- rollcall은 `LectureEnrollmentPort`를 통해 강의 정보와 수강생 목록을 조회한다.
- 출결 기록은 rollcall이 소유하지만, 강의와 수강생의 원천 데이터는 lecture/student 쪽에 있다.

### SMS 외부 공급자

실제 문자 발송은 아직 구현하지 않는다. 외부 SMS 공급자, API 키, 발신번호, 과금 정책, 실패 재시도 정책이 확정되어야 한다.

```text
[대상]
SMS 공급자 또는 추후 notification/sms 연동 범위

[필요한 변경]
문자 발송 Port 및 외부 API Adapter 추가

[입력]
academyId, senderUserId, lectureId, date, 수신자 전화번호 목록, 템플릿 내용

[출력]
발송 요청 ID, 성공/실패 결과, 실패 사유

[필요한 이유]
출결 체크 후 선택한 학부모에게 결석/지각/인강/기타 안내 문자를 실제 발송해야 함

[영향 범위]
rollcall 문자 발송 화면, 발송 이력/실패 재시도/과금 정책
```

## 발행·소비하는 Event

- 현재 발행하는 Event는 없다.
- 실제 SMS 발송을 비동기로 처리하게 되면 `AttendanceMessageSendRequestedEvent` 같은 이벤트를 검토할 수 있다.

## 변경 시 주의 사항

- `ETC` 상태는 비고가 필수다.
- 같은 출결 상태의 문자 템플릿은 학원당 1개만 허용한다.
- 현재는 외부 SMS 호출이 없으므로 “문자 발송 대상 조회”를 “실제 발송 완료”로 오해하면 안 된다.
- `ROLLCALL:MANAGE` 권한은 출결 저장과 템플릿 생성/수정/삭제에 적용되어 있다.

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
