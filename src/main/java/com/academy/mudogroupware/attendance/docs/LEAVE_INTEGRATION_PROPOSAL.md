# 휴가 결재 ↔ 근태 연동

> 업데이트: 2026-08-06 · 상태: 구현 반영

## 목적

결재 문서 생성 요청에 휴가 기간이 포함되면 근태 도메인이 신청 가능 여부를 검증하고 자체
`leave_request`로 저장한다. 최종 승인·반려는 결재 도메인의 공개 이벤트를 받아 상태에 반영한다.

## 처리 흐름

```text
1. 휴가 결재 신청
   → approval: ApprovalDocument 저장
   → approval: LeaveRequestSubmissionPort 동기 호출
   → attendance: 근무일 수, 기간 중복, 신청 가능 연차 검증
   → attendance: leave_request를 PENDING으로 저장
   → 검증 또는 저장 실패 시 결재 문서 생성도 함께 롤백

2. 최종 승인·반려
   → approval: ApprovalDocumentDecidedEvent 발행
   → attendance: AFTER_COMMIT 리스너가 Application UseCase 호출
   → 승인: PENDING → APPROVED
   → 반려: PENDING → REJECTED

3. 오늘 팀 근태 조회
   → academyId와 오늘 날짜로 APPROVED 휴가 직원 ID를 한 번에 조회
   → 출근 기록이 없고 승인 휴가 기간이면 LEAVE로 표시
```

## 신청 검증

- `leaveStartDate`와 `leaveEndDate`가 모두 없으면 일반 결재이다.
- 두 날짜 중 하나만 없거나 종료일이 시작일보다 빠르면 거절한다.
- `attendance_policies`와 요일별 설정으로 실제 근무일만 계산해 `used_days`에 저장한다.
- `PENDING`, `APPROVED` 신청과 기간이 겹치면 거절한다.
- 현재 유효한 `leave_grant`의 15일에서 `PENDING`, `APPROVED` 사용 일수를 제외해 신청 가능 일수를 계산한다.
- 신청 기간은 현재 지급 이력의 유효 기간 안에 있어야 한다.
- 직원별 활성 지급 이력을 비관적 잠금으로 조회해 동시 신청을 직렬화한다.

## 상태 규칙

```text
PENDING → APPROVED
PENDING → REJECTED
```

- 동일한 최종 결정 이벤트의 재수신은 멱등 처리한다.
- `APPROVED`와 `REJECTED` 사이의 재변경은 허용하지 않는다.
- 승인된 연차의 별도 취소 기능은 제공하지 않는다.

## 연차 지급

- 활성 직원에게 입사일 기준으로 매년 15일을 지급한다.
- `AttendanceLeaveGrantScheduler`가 매일 00:05 KST에 실행된다.
- 기존 Global `SchedulingConfig`와 공통 `Clock`을 재사용한다.
- 스케줄러는 현재 지급 기간의 이력이 없을 때만 `leave_grant`를 생성한다.
- `UNIQUE (academy_id, user_id, grant_date)`가 중복 지급을 최종 방어한다.

## 모듈 경계

- approval은 `LeaveRequestSubmissionPort`만 호출하고 attendance의 Entity와 Repository를 참조하지 않는다.
- attendance는 최종 결정 시 `ApprovalDocumentDecidedEvent`만 소비한다.
- `leave_request.document_id`는 다른 도메인의 FK가 아닌 식별값으로만 보관한다.

## 현재 제외 범위

- 직원 개인 잔여 연차 조회 API
- 반차
- 공휴일 및 특정 날짜 임시 휴무일
- 승인된 연차 취소
- 이벤트 재시도 및 아웃박스
