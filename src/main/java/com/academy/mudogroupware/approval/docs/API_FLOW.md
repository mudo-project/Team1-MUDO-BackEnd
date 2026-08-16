# approval API Flow

## 결재 문서 생성

```text
POST /api/approvals
-> CreateApprovalDocumentService
-> ApproverDirectoryPort.getApprover(creatorId)
-> ApproverDirectoryPort.getApprovers(approverIds)
-> 신청자 본인 결재자 지정/중복 결재자 검증
-> ApprovalDocument.create
-> ApprovalDocumentRepository.save
-> leaveStartDate/leaveEndDate가 있으면 LeaveRequestSubmittedEvent 발행
```

`ApproverDirectoryPort`는 approval이 정의하고 users 모듈의 `ApprovalApproverDirectoryAdapter`가 구현한다.
최종 결재선에는 신청자 본인이 포함될 수 없고, 같은 결재자가 2번 이상 들어갈 수 없다.

## 결재 처리와 실시간 알림

```text
POST /api/approvals/{documentId}/decide
-> DecideApprovalLineService
-> 현재 결재선 승인/반려 처리
-> 다음 결재선이 있으면 ApprovalLineActivatedEvent 발행
-> 트랜잭션 커밋 후 ApprovalWebSocketNotifier
-> /topic/approvals/users/{approverId}
```

최종 승인/반려가 되면 `ApprovalDocumentDecidedEvent(status=APPROVED|REJECTED)`를 발행한다. attendance는 이 이벤트의 `approved()` 호환 메서드를 받아 휴가 결재 상태를 `CONFIRMED` 또는 `CANCELLED`로 반영한다.

## 결재 신청 취소

```text
POST /api/approvals/{documentId}/cancel
-> CancelApprovalDocumentService
-> 신청자 본인 검증
-> 아직 승인/반려 처리된 결재선이 없는지 검증
-> ApprovalDocument.cancel
-> ApprovalDocumentRepository.save
-> ApprovalDocumentDecidedEvent(status=CANCELLED) 발행
```

취소는 신청자 본인만 가능하며, 이미 결재 처리가 시작된 문서는 취소할 수 없다. 문서는 `CANCELLED` 상태로 남긴다.

## 전체 결재 조회

```text
GET /api/approvals
-> @PreAuthorize("hasAuthority('APPROVAL:READ_ALL')")
-> ApprovalQueryService.getAllApprovals(academyId)
-> ApprovalDocumentRepository.findAllByAcademyId
```

전체 조회 권한자는 같은 학원 문서만 조회한다. 상세조회도 같은 학원 문서인 경우에만 허용한다.

## 내 결재 이력 조회와 숨김

```text
GET /api/approvals/me/history
-> ApprovalQueryService.getMyApprovalHistory(userId)
-> 내 결재선 상태가 APPROVED 또는 REJECTED인 문서만 조회
-> approval_history_hidden에 있는 문서는 제외
```

```text
DELETE /api/approvals/me/history/{documentId}
-> HideApprovalHistoryService
-> 내가 결재선 참여자인지 검증
-> 내 결재선이 APPROVED 또는 REJECTED인지 검증
-> approval_history_hidden 저장
```

이력 숨김은 개인 목록에서만 제외하는 기능이다. 결재 문서 원본과 다른 사람의 목록에는 영향을 주지 않는다.

## 첨부파일 AI 요약

```text
POST /api/approvals/{documentId}/attachments/{fileId}/summarize
-> SummarizeApprovalAttachmentService
-> ApprovalDocument.findAttachmentByFileId
-> AttachmentContentPort.loadContent(fileId)
-> AttachmentSummarizerPort.summarize(content)
-> ApprovalAttachment.applySummary
-> ApprovalDocumentRepository.save
```

`AttachmentContentPort`는 approval이 정의하고 file 모듈의 `ApprovalAttachmentContentAdapter`가 구현한다.

실패 흐름:

- 문서에 해당 fileId가 없으면 `APPROVAL_404_4`.
- 파일 메타데이터가 없거나 텍스트로 읽을 수 없으면 `APPROVAL_409_7`.
- Gemini 호출이 실패하면 `APPROVAL_502_1`.
