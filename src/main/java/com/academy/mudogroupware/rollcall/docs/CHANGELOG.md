# 출결 Changelog

## 2026-08-15 - 출결 SMS 발송 시 플랜 한도 체크 추가

- `SendAttendanceMessagesService.send()`가 학생별 발송 루프를 돌기 전, 이번 달 SMS 사용량이 플랜 한도(무료 150건/유료 10,000건)를 넘으면 배치 전체를 429로 차단하도록 변경했다.

## 2026-08-14 - 출결 SMS 발송 재시도 중복 방지 (#354)

**최종 정책**: 학생별 발송 시도를 `attendance_message_send_record`(유니크 키: 강의+학생+출결날짜+출결상태)에 저장한다. 상태는 `PENDING`/`SENDING`/`SENT`/`FAILED`/`INDETERMINATE` 다섯 가지다. 이미 `SENT`면 재요청이 와도 SOLAPI를 다시 호출하지 않는다. `INDETERMINATE`(SOLAPI 응답을 못 받아 결과를 모름)는 **자동 재시도를 차단**하고 관리자 확인이 필요하다 — 실제로는 이미 발송됐을 수 있어 다시 호출하면 중복 발송 위험이 있기 때문이다. 출결이 정정되면(결석→지각 등) 새 조합으로 취급해 재발송을 막지 않는다.

- `attendance_message_send_record` 테이블 추가, 이슈 #354 해결.
- `SolapiSmsAdapter`가 SOLAPI 호출 실패를 "명확한 실패"(`FAILED`)와 "응답을 못 받아 결과를 모름"(`INDETERMINATE`, 타임아웃/연결 끊김)으로 구분해서 기록한다.
- (셀프 리뷰·코드래빗 리뷰 반영) `SENDING` 상태와 조건부 UPDATE(`claimForSending`)로 동시 요청 중 하나만 실제 SOLAPI를 호출하도록 강화. 이미 발송된 건을 스킵할 때 사용량 집계가 중복 카운트되던 버그 수정, 실패 사유(`failure_reason`) 저장 추가.
- 유니크 키에 `attendance_status` 추가. 안 쓰이던 `now` 파라미터 정리.
- (코드래빗 2차 리뷰 반영) `claimForSending`에 빠져있던 `@Transactional` 추가(운영 DB에서 실패할 수 있던 문제). `claimed_at`으로 `SENDING` 중 서버가 죽은 레코드를 5분 뒤 감지해 `INDETERMINATE`로 전환(자동 재발송은 여전히 안 함, 관리자 확인 필요). 완료 로그의 실패 건수 계산 오류 수정.

## 2026-08-11 - 문자 템플릿 변수 치환

- 실제 SMS 발송 직전에 문자 템플릿의 `{학생명}`, `{강의명}`, `{날짜}` 변수를 실제 값으로 치환하도록 변경했다.
- 후보 조회 결과에 강의명을 내부적으로 포함해 `{강의명}` 치환에 사용한다. API 응답 형식은 유지한다.

## 2026-08-10 - 출결 안내 문자 실제 발송 구현

- 솔라피(SOLAPI) API로 학부모에게 출결 안내 SMS를 실제로 발송하는 기능을 추가했다.
- `POST /api/rollcall/lectures/{lectureId}/attendance/message-candidates/send`로 선택한 학생들에게 발송하고, 학생별 성공/실패 결과를 즉시 반환한다.
- 발송 대상 조회(`eligible=false`)였던 학생은 실제 발송 없이 실패로 처리된다.

## 2026-08-08 - 출결/문자 템플릿 권한 분리

- 출결관리 탭 접근, 출결 조회/저장, 엑셀 다운로드, 문자 발송 대상 후보 조회를 `ROLLCALL:MANAGE`로 묶었다.
- 문자 템플릿 생성/수정/삭제는 `ROLLCALL:TEMPLATE_MANAGE`로 분리했다.
- 문자 템플릿 목록 조회는 출결 담당자도 문구 확인이 가능하도록 `ROLLCALL:MANAGE` 또는 `ROLLCALL:TEMPLATE_MANAGE` 중 하나가 있으면 허용한다.

## 2026-08-06 - 문서 정합성 갱신

- README의 설계 단계 문구를 제거하고 실제 구현 기준으로 갱신했다.
- API.md, API_FLOW.md, REVISION.md, CHANGELOG.md를 추가했다.
- 실제 SMS 발송은 아직 구현되지 않았고, 현재는 문자 템플릿 CRUD와 발송 후보 조회까지만 제공한다는 점을 명확히 적었다.

## 2026-08-05 - 출결 백엔드 초기 구현

- 강의 출결부 조회 API를 추가했다.
- 학생별 출결 상태 일괄 저장 API를 추가했다.
- 출결부 엑셀 다운로드를 추가했다.
- 출결 상태별 문자 템플릿 CRUD를 추가했다.
- 출결 상태에 맞는 문자 발송 후보 조회 API를 추가했다.
- `ROLLCALL:MANAGE` 권한을 출결 저장 API에 적용했다.
- 문자 템플릿 변경 권한은 2026-08-08에 별도 권한으로 분리했다.
