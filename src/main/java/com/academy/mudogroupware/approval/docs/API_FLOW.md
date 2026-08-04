# 🔄 전자결재 처리 Flow

## 1. 결재 템플릿 생성 흐름

```text
AuthUser (JWT)
→ SecurityConfig: POST /api/approval-templates, 인증 확인
→ ApprovalTemplateController.createTemplate
→ AuthUser.userId 추출
→ CreateApprovalTemplateRequest → CreateApprovalTemplateCommand
→ CreateApprovalTemplateService.createTemplate
→ ApproverDirectoryPort.getApprover(creatorId) → academyId 조회
→ ApprovalTemplate.create(academyId, name, creatorId, approverIds, now)
→ ApprovalTemplateRepository.save
→ ApprovalTemplateRepositoryImpl → template 테이블(type='APPROVAL') + approval_line_step 저장
→ GlobalApiResponse<ApprovalTemplateCreateResponse>
```

- `academyId`는 요청 값이 아니라 생성자 정보에서 서버가 직접 조회합니다 (클라이언트가 다른 학원을 지정할 수 없도록).
- `template` 테이블은 approval 전용이 아니라 팀 공용 테이블입니다. `type='APPROVAL'`로만 필터링해 사용합니다.

## 2. 결재 템플릿 목록 조회 흐름

```text
AuthUser
→ ApprovalTemplateController.getTemplates(page, size)
→ ApprovalTemplateQueryService.getTemplates(requesterId, page, size)
→ ApproverDirectoryPort.getApprover(requesterId) → academyId 조회
→ ApprovalTemplateRepository.findAll(academyId, page, size)
→ ApprovalTemplateJpaRepository.findAllByTypeAndAcademyId(TYPE, academyId, Pageable) → Slice<Entity>
→ 템플릿별 결재선 approverId 목록 수집
→ ApproverDirectoryPort.getApprovers(approverIds) 배치 조회
→ PageResult<ApprovalTemplateSummaryView>(+ lines)
→ GlobalApiResponse<SliceResponse<ApprovalTemplateSummaryResponse>>
```

- 학원 스코프 필터링이 없으면 다른 학원 템플릿이 섞여 보이는 버그였습니다 (CodeRabbit 리뷰로 발견, 2026-08-03 수정).

## 3. 결재 신청 생성 흐름

```text
AuthUser
→ ApprovalController.createDocument
→ CreateApprovalDocumentRequest → CreateApprovalDocumentCommand
→ CreateApprovalDocumentService.createDocument
→ ApprovalTemplateRepository.findById(templateId)
→ ApproverDirectoryPort.getApprover(creatorId)
→ 신청자 academyId != 템플릿 academyId 이면 ForbiddenException
→ approverIds 미지정 시 template.approverIdsInOrder() 사용
→ ApprovalContent.create(contentType, text)
→ ApprovalDocument.create(academyId, templateId, title, content, creatorId, approverIds, fileIds, now)
   → buildLines: 1차만 PENDING, 나머지는 WAITING
   → fileIds → ApprovalAttachment.create 목록
→ ApprovalDocumentRepository.save
→ GlobalApiResponse<ApprovalCreateResponse>
```

- 결재선 순서는 항상 서버에서 1부터 재계산합니다 (클라이언트가 stepOrder를 직접 지정하지 않음).
- 교차 학원 신청 차단은 2026-08-03 CodeRabbit 리뷰로 추가되었습니다.

## 4. 결재 승인/반려 흐름

```text
AuthUser (현재 차례의 결재자)
→ ApprovalController.decide
→ DecideApprovalLineRequest → DecideApprovalLineCommand
→ DecideApprovalLineService.decide
→ ApprovalDocumentRepository.findById(documentId)
→ ApprovalDocument.decide(approverId, decision, comment, now)
   → 상태가 IN_PROGRESS 아니면 ApprovalException(DOCUMENT_ALREADY_DECIDED)
   → 현재 PENDING 라인의 approverId != 요청자 이면 ApprovalException(NOT_YOUR_TURN)
   → APPROVE: 현재 라인 approve() → 다음 라인 activate() → 전원 승인이면 문서 상태 APPROVED
   → REJECT: 현재 라인 reject() → 문서 상태 REJECTED
→ ApprovalDocumentRepository.save
→ APPROVE로 다음 라인이 활성화되었으면(문서 상태 여전히 IN_PROGRESS) ApplicationEventPublisher.publishEvent(ApprovalLineActivatedEvent)
   → 아직 이 이벤트를 소비하는 리스너는 없음 (Web Push 발송 로직 준비 전)
→ 204 No Content
```

- 순차 결재만 지원합니다 (병렬 결재 미지원, 의도된 설계 — 난이도를 낮추기 위한 결정).
- 반려 시 `comment`가 사실상 필수로 쓰이도록 프론트에서 강제해야 합니다 (백엔드는 선택값으로 열어둠).

## 5. 결재 재상신 흐름

```text
AuthUser (원본 신청자 본인)
→ ApprovalController.resubmit
→ ResubmitApprovalDocumentService.resubmit
→ ApprovalDocumentRepository.findById(documentId)
→ 신청자 본인 아니면 ApprovalException(NOT_DOCUMENT_OWNER_RESUBMIT)
→ ApprovalDocument.markResubmitted(now)
   → 상태가 REJECTED가 아니면 ApprovalException(RESUBMIT_NOT_REJECTED)
   → 이미 resubmittedAt이 있으면 ApprovalException(ALREADY_RESUBMITTED) (중복 재상신 차단)
→ 원본의 title/content/lines/attachments를 복사해 새 ApprovalDocument.create(..., now)
→ 새 문서 저장 → 원본 문서(resubmittedAt 채워짐) 저장
→ GlobalApiResponse<ApprovalCreateResponse>(새 documentId)
```

- 원본 문서에 `resubmittedAt`을 남겨서, 같은 문서로 두 번째 재상신을 시도하면 막습니다 (2026-08-03 CodeRabbit 리뷰로 추가 — 원래는 무제한 재상신이 가능한 버그가 있었습니다).

## 6. 결재 대기 건수 조회 흐름

```text
AuthUser
→ ApprovalController.getMyPendingCount
→ ApprovalQueryService.getMyPendingCount(userId)
→ ApprovalDocumentRepository.findAllByApproverId(userId)
→ status == IN_PROGRESS && 내 라인 status == PENDING 인 것만 카운트
→ GlobalApiResponse<ApprovalPendingCountResponse>
```

- 목록 API를 재사용하지 않고 카운트만 계산합니다. 사이드바 뱃지처럼 자주 호출되는 지점이라 별도 경량 엔드포인트로 분리했습니다.

## 7. 푸시 구독 등록 흐름

```text
AuthUser
→ PushSubscriptionController.register
→ RegisterPushSubscriptionRequest → RegisterPushSubscriptionCommand
→ RegisterPushSubscriptionService.register
→ PushSubscription.create(userId, endpoint, p256dh, auth, now)
→ PushSubscriptionRepository.save
   → PushSubscriptionJpaRepository.findByUserIdAndEndpoint 로 기존 구독 존재 여부 확인
   → 있으면 p256dh/auth만 갱신, 없으면 새로 insert
→ GlobalApiResponse<PushSubscriptionCreateResponse>
```

- 실제 푸시 발송은 이 API 범위에 없습니다. `ApprovalLineActivatedEvent` 리스너가 추가되면, 그 리스너가 이 구독 정보를 조회해 발송합니다.

## 8. 첨부파일 AI 요약 생성 흐름

```text
AuthUser (신청자 본인 또는 결재선 포함자)
→ ApprovalController.summarizeAttachment
→ SummarizeApprovalAttachmentCommand(documentId, fileId, requesterId)
→ SummarizeApprovalAttachmentService.summarize
→ ApprovalDocumentRepository.findById(documentId)
→ 신청자·결재자 아니면 ApprovalException(DOCUMENT_ACCESS_DENIED)
→ ApprovalDocument.findAttachmentByFileId(fileId) → 없으면 ApprovalException(ATTACHMENT_NOT_FOUND)
→ placeholder 텍스트 생성 ("첨부파일(fileId=...)의 내용을 요약해줘" — 실제 파일 내용 조회 방법이 없어서 임시)
→ AttachmentSummarizerPort.summarize(placeholder)
   → GeminiSummarizerAdapter → RestClient POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent
   → 실패(RestClientException/빈 응답) 시 AttachmentSummarizationException
→ 성공: ApprovalAttachment.applySummary(summary, now) → summaryStatus=COMPLETED
→ 실패: ApprovalAttachment.markSummaryFailed(now) → summaryStatus=FAILED, ApprovalException(SUMMARY_GENERATION_FAILED) 던짐(502)
→ ApprovalDocumentRepository.save
→ GlobalApiResponse<ApprovalAttachmentSummaryResponse>
```

- 업로드 시 자동 트리거가 아니라 클라이언트가 명시적으로 호출하는 동기 API입니다.
- 실제 첨부파일 내용이 아니라 placeholder 텍스트를 Gemini에 보냅니다 — `file` 모듈이 `fileId → 실제 파일 내용` 조회를 제공하면 교체해야 합니다.

---

## 📝 문서 정보

- 업데이트일: `2026-08-04`
- 변경 사항(요약):
  - 결재 템플릿/문서 도메인 분리 이후의 전체 흐름을 정리했습니다.
  - 학원(academy) 스코프 검증 지점(템플릿 목록 조회, 결재 신청)을 흐름에 표시했습니다.
  - 재상신 중복 방지 로직(`resubmittedAt`)을 반영했습니다.
  - 페이지네이션, 전용 ErrorCode, Web Push 구독, AI 요약(Gemini) 흐름을 추가하고 흐름 번호를 정리했습니다.
