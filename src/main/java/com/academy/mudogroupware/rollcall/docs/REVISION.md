# 출결 Revision

## 2026-08-14 · 출결 SMS 발송 재시도 중복 방지

### 배경

이슈 #354. 코드 리뷰 중 발견: SOLAPI 호출 도중 타임아웃/연결 끊김이 나면 실제로는 SOLAPI에 접수돼 발송이 진행됐을 수 있는데도 서버는 이를 "실패"로만 응답했다. 발송 시도 자체를 어디에도 저장하지 않아서, 사용자가 실패 응답을 보고 재시도하면 같은 학생에게 SMS가 중복 발송될 수 있었다.

### 변경 내용

- 신규 테이블 `attendance_message_send_record`(`lecture_id`, `student_id`, `entry_date`에 유니크 제약) 추가. 마이그레이션 `be6/V6.1.7`.
- `AttendanceMessageSendRecord`(도메인 모델) / `AttendanceMessageSendStatus`(`PENDING`/`SENT`/`FAILED`/`INDETERMINATE`) 추가.
- `SendAttendanceMessagesService`가 SOLAPI 호출 전에 이 레코드를 먼저 만들어보고(유니크 제약을 이용한 insert-first 패턴, 동시 요청이 와도 하나만 생성됨), 이미 `SENT` 상태면 SOLAPI를 다시 부르지 않고 그 결과를 그대로 반환한다.
- `SolapiSmsAdapter`가 `ResourceAccessException`(응답 받기 전 타임아웃/연결 끊김)과 그 외 `RestClientException`(SOLAPI가 명확히 준 실패 응답)을 구분해서, 전자는 `INDETERMINATE`로 기록한다 — 재시도는 막지 않되(모르는 상태를 실패로 단정하지 않음) 상태는 남긴다.
- `AttendanceMessageSendRecordRepositoryImpl` 구현 중 발견한 이슈: 제약 위반으로 insert가 실패한 뒤 같은 영속성 컨텍스트에서 바로 조회하면 Hibernate가 "세션이 오염됐다"는 `AssertionFailure`를 던진다 — 조회 전 `EntityManager.clear()`로 해결. 상태 갱신(`save()`)도 `saveAndFlush`로 즉시 flush하도록 해서, 나중 insert 실패 시점까지 flush가 미뤄지며 이전 갱신이 함께 유실되는 문제를 막았다.

### 리뷰(셀프 리뷰 + 코드래빗) 반영 — 같은 날 추가 수정

첫 구현은 "레코드 생성"만 원자적으로 보장했을 뿐, 실제 SOLAPI 호출까지 원자적으로 막지는 못해서 여전히 동시 요청·재시도 조합에서 중복 발송이 가능했다. 리뷰에서 지적받아 같은 세션에서 바로 고쳤다.

- `AttendanceMessageSendStatus`에 `SENDING` 추가. `claimForSending(id)`가 상태가 `PENDING`/`FAILED`일 때만 `SENDING`으로 전환하는 조건부 UPDATE(영향받은 행 수 1이어야 성공)를 실행해, 동시 요청 중 정확히 하나만 실제 SOLAPI 호출로 진행한다. `INDETERMINATE`는 이 전환 대상에서 제외해 자동 재시도를 막는다(이 PR이 막으려던 "타임아웃 후 재시도 중복발송" 시나리오가 INDETERMINATE 경로에서도 재현되던 문제).
- 사용량(과금) 집계가 "이미 SENT라 스킵한 응답"까지 신규 발송으로 카운트하던 버그 수정 — 실제로 SOLAPI를 호출해 성공한 경우만 집계하도록 분리.
- `attendance_message_send_record`에 `failure_reason` 컬럼 추가(마이그레이션은 아직 머지 전이라 기존 `V6.1.7`에 그대로 반영, 새 파일 안 만듦) — `FAILED`/`INDETERMINATE`의 구체적 사유를 발송 이력에서도 조회할 수 있게 했다.

### `now` 파라미터 정리 + `EntityManager.clear()` 주석 정확화

- `createOrGetExisting`에 넘기던 `LocalDateTime now`가 실제로는 어디에도 쓰이지 않던 죽은 파라미터였다(엔티티의 `createdAt`/`updatedAt`은 JPA 감사가 채움) — 파라미터 자체를 제거.
- `EntityManager.clear()` 주석이 "항상 필요한 방어"처럼 읽혀서, 운영 기본 설정(`open-in-view: false`, 이 메서드와 호출부 모두 비트랜잭션)에서는 save/조회가 이미 분리된 영속성 컨텍스트라 이 문제 자체가 안 생긴다는 점과, 나중에 이 메서드가 `@Transactional`로 감싸이면 다시 필요해질 수 있다는 점을 명시하도록 주석을 고쳤다.

### 출결 정정 시 재발송 허용 — 유니크 키에 `attendance_status` 추가

한 번 `SENT`되면 그 (강의, 학생, 날짜) 조합은 영구히 "이미 보냄" 처리돼, 출결을 정정(결석→지각 등)해도 수정된 안내 문자가 재발송되지 않는 문제가 있었다. 현재 시스템엔 담당자가 자유 문구를 입력해서 보내는 기능이 없고(`SendAttendanceMessagesRequest`는 `studentIds`만 받음) 내용은 항상 "그 학생의 현재 출결 상태에 매칭되는 템플릿"으로만 결정되므로, **출결 상태(`AttendanceStatus`)를 유니크 키에 포함**시켜 "내용이 바뀌었는지"를 판단하기로 했다.

- 유니크 키를 `(lecture_id, student_id, entry_date)` → `(lecture_id, student_id, entry_date, attendance_status)`로 변경(마이그레이션은 아직 미머지라 기존 `V6.1.7`에 컬럼 추가 + 유니크 키 변경으로 반영).
- `AttendanceMessageSendRecord`/엔티티/리포지토리 조회 메서드에 `attendanceStatus` 추가, `SendAttendanceMessagesService`가 `candidate.status()`(발송 후보 조회 시점의 출결 상태)를 넘겨준다.
- 결과: 학생이 "결석"으로 SENT된 뒤 "지각"으로 정정되면 (강의,학생,날짜,지각)은 새 조합이라 다시 발송된다. 같은 상태로 또 요청이 오면 여전히 스킵된다.

### 남은 범위 밖 항목

- SOLAPI가 idempotency key를 지원하는지, 지원한다면 발송 식별자를 그 키로 전달하는 것은 이번 범위에 포함하지 않았다.
- `INDETERMINATE`가 된 레코드를 관리자가 확인 후 강제로 재발송할 수 있는 절차/API는 없다 — 지금은 영구적으로 자동 재시도가 막힌 상태로 남는다.
- 담당자가 자유 문구로 SMS를 보내는 기능은 없다 — 생기면 이번 유니크 키 설계(출결 상태 기준)를 다시 검토해야 한다.

> 작성일: 2026-08-14
> 상태: 백엔드 구현 완료, 테스트 통과(서비스 목 테스트/어댑터 테스트/DataJpaTest).

## 2026-08-10 · 출결 안내 문자 실제 발송 구현

### 배경

발송 대상 후보 조회까지는 구현돼 있었지만, 실제 SMS 발송 공급자가 정해지지 않아 발송 자체는 미구현 상태였다. 처음엔 알리고(Aligo, SMS 단가 8.4원으로 가장 저렴)를 선택했으나, 알리고는 API Key 발급에 사업자 인증이 필요해 개인/학교 프로젝트로는 바로 발급받을 수 없었다. 개인 계정도 사업자 인증 없이 즉시 API Key를 발급받을 수 있는 솔라피(SOLAPI)로 전환했다(단가는 13원으로 알리고보다 비싸지만, 일정이 빠듯한 상황에서 즉시 연동 가능한 게 더 중요했다).

### 변경 내용

- `SmsSenderPort`(application/port) 추가, `SolapiSmsAdapter`(infrastructure/external/solapi)가 솔라피 REST API(`POST https://api.solapi.com/messages/v4/send`, HMAC-SHA256 인증, JSON)로 구현.
- `SendAttendanceMessagesUseCase`/`SendAttendanceMessagesService` 추가: 기존 `GetMessageSendCandidatesUseCase`로 후보를 조회해 요청받은 `studentIds` 중 `eligible=true`인 학생에게만 학생 1명당 API 호출 1건으로 발송. 배치(다수 수신자 comma-join) 대신 개별 호출을 택해 학생별 성공/실패 귀속을 단순하고 명확하게 유지했다.
- 외부 API 호출 실패는 예외 대신 `SmsSendResult.failed(reason)`으로 반환해, 한 학생의 발송 실패가 나머지 학생 발송을 막지 않는다.
- `POST /api/rollcall/lectures/{lectureId}/attendance/message-candidates/send` 추가, 신규 에러코드 `NO_STUDENTS_SELECTED`(`ROLLCALL_400_2`).
- 발송 이력 저장(DB), 실패 자동 재시도, 과금 정책은 이번 범위에 포함하지 않았다 — 필요해지면 후속 작업으로 분리.

> 작성일: 2026-08-10
> 상태: 백엔드 구현 완료, 테스트 통과. 실제 Solapi API로 문자 발송 성공(응답 `statusCode: "2000"`, 실제 수신 확인)까지 검증 완료. 발송 이력/재시도/과금 정책은 후속 과제.

## 2026-08-06 · 초기 백엔드 구현

> 작성일: 2026-08-06
> 상태: 초기 백엔드 구현 완료, 실제 SMS 발송 보류

## 변경 목적

강의별 출결 체크와 출결 상태별 학부모 문자 안내 준비를 백엔드에서 처리할 수 있게 했다. 실제 SMS 외부 발송은 공급자와 키가 정해지지 않아 구현하지 않고, 템플릿 관리와 발송 후보 조회까지만 구현했다.

## 구현 내용

### Domain

- `AttendanceEntry`: 강의·학생·날짜 단위 출결 기록.
- `AttendanceStatus`: 출석/결석/지각/인강/기타 상태.
- `MessageTemplate`: 출결 상태별 문자 템플릿.

### Application

- 강의 출결부 조회.
- 출결 일괄 저장(upsert).
- 출결부 엑셀 다운로드.
- 문자 발송 후보 조회.
- 문자 템플릿 생성/목록/수정/삭제.

### Presentation

- `/api/rollcall/lectures/{lectureId}/attendance`
- `/api/rollcall/lectures/{lectureId}/attendance/export`
- `/api/rollcall/lectures/{lectureId}/attendance/message-candidates`
- `/api/rollcall/message-templates`

### 권한

- 출결관리 탭 접근, 출결 조회/저장, 엑셀 다운로드, 문자 발송 대상 후보 조회에는 `ROLLCALL:MANAGE` 권한이 필요하다.
- 문자 템플릿 생성/수정/삭제에는 `ROLLCALL:TEMPLATE_MANAGE` 권한이 필요하다.
- 조회 API는 인증 사용자 학원 스코프로 제한된다.

## 의도적으로 남긴 제한

- 실제 SMS 발송 API는 없다.
- 외부 SMS 공급자, API 키, 발신번호, 발송 이력 저장, 실패 재시도 정책이 미정이다.
- 후보 조회 결과의 `eligible=true`는 “발송 가능 후보”라는 뜻이지 “발송 완료”가 아니다.

## 검증 기준

- 강의 출결부는 수강생 목록과 저장된 출결 기록을 병합해 반환한다.
- `ETC` 상태에서 비고가 없으면 실패한다.
- 같은 출결 상태 템플릿은 중복 생성할 수 없다.
- 엑셀 다운로드는 출결 상태가 비어 있어도 파일을 생성한다.
