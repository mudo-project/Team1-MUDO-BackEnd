# 휴가 결재 ↔ 근태 자동 연동 제안 (검토 요청)

> 작성일: 2026-08-06 · 작성: approval 담당자(minseopark0327) · **상태: attendance 담당자 리뷰 대기 — 아직 병합 전**
> `docs/MODULES.md`의 "타 모듈 변경 요청" 절차에 따라, attendance 도메인에 실제로 추가될 코드까지 초안으로 작성해서 제안한다. 리뷰 후 attendance 담당자가 그대로 반영하거나, 구조를 바꿔서 다시 구현해도 된다 — approval 쪽 이벤트 계약(아래 2개)만 유지되면 됨.

## 배경 (필요한 이유)

전자결재는 "휴가" 같은 별도 카테고리 없이 템플릿을 자유롭게 만드는 게 컨셉이라, 근태 쪽에서 "이 결재가 휴가 신청이고 언제부터 언제까지인지"를 알 방법이 없었다. 그래서 승인된 휴가 결재가 있어도 근태 현황(`오늘 팀 근태 조회`)에는 그냥 "미출근(ABSENT)"으로만 보였다.

## 설계 방향

이벤트/리스너 방식을 선택했다. 이유: 근태 현황 조회(읽기)가 결재 신청(쓰기)보다 훨씬 빈번해서, 조회 시점에 매번 approval에 물어보는 것보다 승인 시점에 근태 쪽에 미리 반영해두고 조회는 근태 자체 테이블만 보는 게 성능상 유리하다.

## 데이터 흐름

```text
1. 결재 신청 (POST /api/approvals, leaveStartDate/leaveEndDate 둘 다 채워서 요청한 경우에만)
   → approval: ApprovalDocument 저장 (휴가 기간은 approval DB에 남기지 않음)
   → approval: LeaveRequestSubmittedEvent 발행 (documentId, academyId, requesterId, startDate, endDate, submittedAt)
   → attendance: leave_request 테이블에 status=PENDING으로 저장

2. 결재 승인/반려 (POST /api/approvals/{documentId}/decide, 마지막 결재선까지 처리된 시점)
   → approval: ApprovalDocument.status가 APPROVED 또는 REJECTED로 확정
   → approval: ApprovalDocumentDecidedEvent 발행 (documentId, academyId, requesterId, approved, decidedAt)
   → attendance: documentId로 leave_request를 찾아 CONFIRMED(승인) 또는 CANCELLED(반려)로 갱신
     (해당 documentId로 등록된 leave_request가 없으면 - 즉 휴가 신청이 아니었던 일반 결재면 - 아무 것도 안 함)

3. 오늘 팀 근태 조회 (GET, TodayTeamAttendanceQueryService.getToday)
   → 직원별로 반복 조회하지 않고, academyId+오늘 날짜로 CONFIRMED인 leave_request의 userId를
     한 번에 모아서(Set) 사용 - 이 부분이 이번 변경의 핵심 성능 포인트.
   → 출근 기록 없는 직원 중 이 Set에 있으면 LEAVE, 없으면 기존처럼 ABSENT.
```

## approval이 공개하는 이벤트 (계약 — attendance는 이 값만 신뢰하면 됨)

```java
// approval.domain.event.LeaveRequestSubmittedEvent
record LeaveRequestSubmittedEvent(Long documentId, Long academyId, Long requesterId,
                                   LocalDate startDate, LocalDate endDate, LocalDateTime submittedAt)

// approval.domain.event.ApprovalDocumentDecidedEvent
// 문서 전체가 최종 승인/반려로 "확정"된 시점 1회만 발행 (중간 결재 단계 통과 시엔 발행 안 함)
record ApprovalDocumentDecidedEvent(Long documentId, Long academyId, Long requesterId, boolean approved,
                                     LocalDateTime decidedAt)
```

두 이벤트 모두 Spring `ApplicationEventPublisher`로 발행되고(같은 JVM 내부, 메시지 큐 아님), 리스너는 `@TransactionalEventListener(phase = AFTER_COMMIT)`로 붙어야 한다 — approval 쪽 트랜잭션이 롤백되면 이벤트 자체가 발행되지 않아서 안전하다.

## attendance 쪽에 추가한 코드 (제안, 리뷰 필요)

- `domain/model/LeaveRequestStatus.java` — `PENDING`/`CONFIRMED`/`CANCELLED`
- `domain/model/LeaveRequest.java` — `academyId`/`userId`/`documentId`/`startDate`/`endDate`/`status`
- `domain/repository/LeaveRequestRepository.java` — `save`/`findByDocumentId`/`findConfirmedUserIds(academyId, date)`(배치 조회용)
- `infrastructure/persistence/LeaveRequestJpaEntity.java` + `LeaveRequestJpaRepository.java` + `LeaveRequestRepositoryImpl.java`
- `infrastructure/event/ApprovalLeaveEventListener.java` — 위 두 이벤트를 구독해 `LeaveRequestRepository`에 반영
- `domain/model/TeamAttendanceStatus.java`에 `LEAVE` 값 추가
- `application/query/TodayTeamAttendanceQueryService.java`에 `findConfirmedUserIds` 1회 배치 조회 + 분기 추가
- `application/query/TodayTeamAttendanceView.Summary`, `presentation/api/response/TodayTeamAttendanceResponse.Summary`에 `leaveCount` 필드 추가 (LEAVE 인원도 요약 집계에 잡히도록 — 안 넣으면 present+absent+off 합이 전체 인원과 안 맞게 됨)
- 마이그레이션: `db/migration/be2/V2.1.9__create_leave_request_table.sql` (attendance 담당자 번호 `be2` 사용, 아직 어떤 환경에도 적용 안 됨)

approval의 Entity/Repository/Service는 어디서도 직접 참조하지 않는다 — 오직 위 두 이벤트 레코드 타입만 import한다.

## 알려진 제약 / 리뷰 시 봐줬으면 하는 부분

1. **이벤트 유실 가능성**: `AFTER_COMMIT` 리스너가 예외를 던지면(예: DB 순간 장애) Spring이 로그만 남기고 넘어간다 — 재시도 로직이 없다. 승인됐는데 근태에 반영 안 되는 케이스가 드물게 생길 수 있어서, 운영 중 불일치 발견되면 수동 보정이 필요할 수 있다. 재시도/아웃박스 패턴은 이번 범위에서 일부러 안 넣었다(요청하신 "변경 많이 없이"에 맞춤).
2. **`leave_request.document_id`는 approval의 FK가 아니라 그냥 값**이다 — DB 레벨 참조 무결성은 없다(다른 도메인 FK 금지 규칙 때문).
3. 문서가 "휴가 신청"인지 여부는 **템플릿 카테고리가 아니라 문서 생성 요청에 `leaveStartDate`/`leaveEndDate`가 둘 다 채워졌는지**로만 판단한다. 프론트에서 이 두 필드를 넣어야 연동된다.
4. 반려된 휴가는 `leave_request`에 `CANCELLED`로 남는다(삭제 안 함) — 이력 보존 목적. 필요 없으면 삭제 방식으로 바꿔도 됨.

## 남은 것

- [ ] attendance 담당자 코드 리뷰 및 필요시 재구현
- [ ] `docs/DATABASE.md` 기준 `be2` 마이그레이션 번호 충돌 여부 재확인(현재 attendance 최신 버전은 `V2.1.8`)
- [ ] (선택) 결재 신청 화면에 휴가 기간 입력 UI 추가는 프론트 별도 작업
