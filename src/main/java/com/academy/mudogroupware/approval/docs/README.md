# approval 모듈

전자결재 템플릿, 결재 문서, 결재선, 결재 신청 취소, 개인 결재 이력 숨김, 첨부파일 AI 요약, 결재 차례 실시간 알림을 담당한다.

## 책임과 범위

- `ApprovalTemplate`: 반복 사용하는 결재 양식과 기본 결재선.
- `ApprovalDocument`: 사용자가 실제로 상신한 결재 문서.
- `ApprovalDocumentLine`: 결재자별 승인/반려 상태.
- `ApprovalAttachment`: 결재 문서에 연결된 `fileId`와 AI 요약 상태.
- `approval_history_hidden`: 사용자가 본인 결재 이력에서 숨긴 문서 기록. 문서 원본은 삭제하지 않는다.
- `ApprovalLineActivatedEvent`: 다음 결재자 차례가 되었을 때 발행하는 이벤트.
- `ApprovalDocumentDecidedEvent`: 결재 최종 승인/반려 또는 신청 취소 시 attendance 연동을 위해 발행하는 이벤트.
- `PushSubscription`: 과거 Web Push 준비용 구독 정보. 현재 정책상 실제 발송은 하지 않는다.

## 다른 모듈 연동

- users: `ApproverDirectoryPort`를 users 모듈의 `ApprovalApproverDirectoryAdapter`가 구현한다. approval은 users 테이블을 직접 매핑하지 않는다.
- file: `AttachmentContentPort`를 file 모듈의 `ApprovalAttachmentContentAdapter`가 구현한다. 텍스트/PDF/이미지/docx 파일 원문을 Gemini 요약으로 전달한다(PDF·이미지는 멀티모달 inline 데이터, docx는 Apache POI로 추출한 텍스트).
- file: 첨부파일 다운로드 URL 발급은 file 모듈이 공개하는 `GetFileDownloadUrlUseCase`를 직접 주입해서 쓴다(`GetApprovalAttachmentDownloadUrlService`). approval이 신청자/결재선 참여자 검증과 fileId의 문서 소속 검증을 먼저 마친 뒤에만 호출한다 — file 모듈의 범용 다운로드 API는 인증만 되면 fileId를 아는 누구나 호출할 수 있으므로, 그대로 노출하면 결재선과 무관한 사람도 URL을 받을 수 있다.
- attendance: 휴가 기간이 포함된 결재는 `LeaveRequestSubmittedEvent`와 `ApprovalDocumentDecidedEvent`로 휴가 상태를 전달한다.
- corporatecard: `ExtractApprovalAttachmentFieldsUseCase`를 corporatecard가 직접 주입해서 쓴다. 결재 문서의 첫 번째 첨부파일에서 Gemini 구조화 출력으로 금액/일자/가맹점을 추출해준다(영수증-카드거래 대사 검증용). REST 엔드포인트는 없고 UseCase만 공개한다.
- global security: `AuthUser`로 인증 사용자 정보를 받는다.

## 권한 정책

- `APPROVAL:SUBMIT` 권한자는 결재를 상신하거나 반려된 본인 문서를 재상신할 수 있다.
- `APPROVAL:TEMPLATE_MANAGE` 권한자는 결재 템플릿을 생성/수정/삭제할 수 있다.
- 결재 템플릿 목록/상세 조회는 결재 상신 화면에서도 필요하므로 `APPROVAL:SUBMIT` 또는 `APPROVAL:TEMPLATE_MANAGE` 중 하나가 있으면 허용한다.
- 기본 결재 문서 상세조회는 신청자 또는 결재선 참여자만 가능하다.
- `APPROVAL:READ_ALL` 권한자는 소속 학원의 전체 결재 목록과 상세를 조회할 수 있다.
- 내 결재 이력 삭제는 개인 목록 숨김 처리이며, 문서 원본을 삭제하지 않는다.

## 알림 정책

- 결재 차례 실시간 반영은 WebSocket/STOMP로 고정한다.
- `ApprovalWebSocketNotifier`가 `ApprovalLineActivatedEvent`를 트랜잭션 커밋 후 소비해 `/topic/approvals/users/{userId}`로 보낸다.
- Web Push 발송, VAPID, web-push 라이브러리 연동은 구현하지 않는다.

## AI 요약 정책

- placeholder 텍스트를 Gemini로 보내지 않는다.
- 파일 메타데이터가 없거나 미지원 contentType(hwp 등)이면 `APPROVAL_409_7`로 실패하고 `summaryStatus=FAILED`로 저장한다.
- 지원 범위는 UTF-8 텍스트 계열, PDF, 이미지(jpeg/png/webp/heic/heif), docx contentType이다. PDF·이미지는 15MB를 초과하면 `APPROVAL_409_7`로 실패한다.

## 문서

- [API.md](API.md)
- [API_FLOW.md](API_FLOW.md)
- [REVISION.md](REVISION.md)
- [CHANGELOG.md](CHANGELOG.md)
