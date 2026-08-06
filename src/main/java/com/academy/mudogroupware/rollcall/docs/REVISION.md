# 출결 Revision

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

- 출결 저장과 템플릿 생성/수정/삭제에는 `ROLLCALL:MANAGE` 권한이 필요하다.
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
