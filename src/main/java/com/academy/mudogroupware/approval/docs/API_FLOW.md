# approval API Flow

## 결재 문서 생성

```text
POST /api/approvals
-> CreateApprovalDocumentService
-> ApproverDirectoryPort.getApprover(creatorId)
-> ApproverDirectoryPort.getApprovers(approverIds)
-> ApprovalDocument.create
-> ApprovalDocumentRepository.save
```

`ApproverDirectoryPort`는 approval이 정의하고 users 모듈의 `ApprovalApproverDirectoryAdapter`가 구현한다.

## 결재 처리와 실시간 알림

```text
POST /api/approvals/{documentId}/decisions
-> DecideApprovalLineService
-> 현재 결재선 승인/반려 처리
-> 다음 결재선이 있으면 ApprovalLineActivatedEvent 발행
-> 트랜잭션 커밋 후 ApprovalWebSocketNotifier
-> /topic/approvals/users/{approverId}
```

Web Push 발송 리스너는 추가하지 않는다.

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
