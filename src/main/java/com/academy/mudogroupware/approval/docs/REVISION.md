# approval Revision

## 2026-08-18 · 결재 문서 낙관적 락(`@Version`) 도입

### 배경

`ApprovalDocument`에는 동시 수정을 막을 장치가 없었다. 같은 문서를 승인/반려/취소하는 요청이 거의 동시에 들어오면, 나중에 저장되는 쪽이 앞선 처리 결과를 조용히 덮어쓸 수 있었다(에러 없는 lost update). `users`(`V4.1.9`)와 `shared_file_root`에 이미 있던 `@Version` 패턴을 그대로 approval에 적용했다.

### 변경

- `ApprovalDocumentEntity`에 `@Version`(`version BIGINT NOT NULL DEFAULT 0`)을 추가했다(`V1.5.20`).
- `ApprovalDocument` 도메인 모델이 `version`을 들고 다니도록 `restore()`에 파라미터를 추가했다(기존 축약 `restore()` 오버로드들은 `version = null`로 위임하므로 테스트 호출부는 변경할 필요가 없었다).
- `ApprovalDocumentRepositoryImpl.save()`가 `saveAndFlush()` + `OptimisticLockingFailureException` 캐치로 바뀌었고, 충돌 시 `ApprovalException(DOCUMENT_VERSION_CONFLICT)`(`APPROVAL_409_10`, 409)로 변환한다.

### 알려진 한계 (과장 금지)

- Hibernate는 merge 시 실제로 UPDATE가 발생하는 엔티티에 한해 버전을 검사한다. `ApprovalDocumentEntity` 자신의 컬럼(`status` 등)이 바뀌지 않고 자식 `approval_step`의 필드만 바뀌는 저장(예: 3명 이상 결재선에서 중간 결재자가 아직 결재 중일 때 같은 라인을 두 번 처리)에서는 부모 버전이 증가하지 않아 충돌이 감지되지 않을 수 있다.
- 실제로 확실히 방어되는 시나리오는 두 요청 중 하나 이상이 문서 상태(`status`)를 바꾸는 경우다. 즉 "승인/반려/취소로 문서 상태가 갈리는 동시 요청"은 안전하게 막히지만, "같은 결재선 하나를 동시에 두 번 승인" 같은 중간 단계 레이스는 이번 변경으로 완전히 커버되지 않는다.
- 결재선 교체(`updateLines`, `getReferenceById` 경로)는 이번 변경의 영향을 받지 않는다. 별도 검토가 필요하다.

### 검증

- `ApprovalDocumentRepositoryImplDataJpaTest#savingStaleDocumentAfterConcurrentDecideThrowsVersionConflict`: 같은 결재선을 동시에 승인/반려하는 상황을 재현해, 나중 요청이 `APPROVAL_409_10`으로 막히고 먼저 커밋된 승인 결과가 그대로 유지되는지 확인했다.
- `./gradlew test` 전체 통과.

## 2026-08-14 · 전자결재 문서 보존 정책

### 배경

전자결재는 단순 게시글이 아니라 학원 내부 승인 증빙이다. 결재 문서, 결재선, 승인/반려 이력, 첨부파일 요약 결과는 나중에 분쟁 대응, 회계 확인, 근태 확인에 쓰일 수 있어 즉시 삭제 중심으로 관리하면 위험하다.

### 결정

- 일반 업무 결재와 휴가/근태 결재는 3년 보존 정책(`GENERAL_BUSINESS`)을 기본으로 둔다.
- 법인카드/비용정산 결재는 세무 증빙 성격이 강하므로 5년 보존 정책(`TAX_EVIDENCE`)을 적용한다.
- 계약, 큰 지출, 운영 핵심 의사결정은 추후 유형이 생기면 10년 보존 정책(`IMPORTANT_BUSINESS`)으로 연결한다.
- 보존기한이 지났다고 바로 삭제하지 않고, `legal_hold`와 `archived_at`을 통해 자동 정리 예외와 장기 보관 전환을 구분한다.

### 변경

- `ApprovalRetentionPolicy`를 추가했다.
- `ApprovalDocument`가 생성 시 `sourceType`에 따라 `retentionPolicy`와 `retentionUntil`을 계산한다.
- `approval_document`에 `retention_policy`, `retention_until`, `legal_hold`, `archived_at` 컬럼을 추가했다.
- 기존 운영 DB에 `source_type`이 있는 경우 법인카드 정산 문서는 마이그레이션에서 5년 보존으로 보정한다.

### 검증

- `ApprovalDocumentTest`
- `ApprovalDocumentRepositoryImplDataJpaTest`

## 2026-08-07 · 전체 조회, 내 결재 이력, 신청 취소, 이력 숨김

### 배경

프론트 화면에서 원장/관리자가 학원 전체 결재를 확인할 수 있는 탭과, 일반 사용자가 본인이 처리한 결재 이력을 따로 보는 탭이 필요해졌다. 또한 사용자가 신청한 결재를 철회하는 기능과, 이미 처리한 결재 이력을 개인 목록에서 정리하는 기능도 필요했다.

### 결정

- 전체 결재 조회는 `APPROVAL:READ_ALL` 권한을 가진 사용자만 가능하게 했다.
- 전체 조회 권한이 있어도 `academyId`가 같은 문서만 조회/상세조회할 수 있다.
- 내 결재 이력은 “내 결재선 상태가 `APPROVED` 또는 `REJECTED`인 문서”로 정의했다.
- 내 결재 이력 삭제는 문서 원본 삭제가 아니라 개인별 숨김 처리로 구현했다.
- 신청 취소는 신청자 본인만 가능하며, 아직 어떤 결재선도 승인/반려 처리되지 않은 `IN_PROGRESS` 문서만 허용한다.
- 취소된 문서는 `CANCELLED` 상태로 남긴다.
- 휴가 결재 취소 시 attendance의 pending 휴가도 취소될 수 있도록 `ApprovalDocumentDecidedEvent(status=CANCELLED)`를 발행한다. 기존 attendance 연동 호환을 위해 이벤트의 `approved()` 메서드는 유지한다.

### 변경

- `ApprovalStatus.CANCELLED` 추가.
- `ApprovalDocument.cancel()`, `ApprovalDocument.hasDecidedLine()` 추가.
- `CancelApprovalDocumentService`, `HideApprovalHistoryService` 추가.
- `approval_history_hidden` 테이블 추가.
- `APPROVAL:READ_ALL` 권한 카탈로그 시드 추가.
- `GET /api/approvals`, `GET /api/approvals/me/history`, `DELETE /api/approvals/me/history/{documentId}`, `POST /api/approvals/{documentId}/cancel` 추가.

### 검증

- `ApprovalDocumentTest`
- `CancelApprovalDocumentServiceTest`
- `HideApprovalHistoryServiceTest`
- `ApprovalQueryServiceTest`

## 2026-08-06 · users/file 공식 Adapter 연결

### 배경

approval 내부에 users 테이블 직접 조회 shim이 있었고, 첨부파일 AI 요약은 실제 파일 원문을 읽을 공식 경로가 없었다. 모듈 경계를 맞추기 위해 approval이 필요한 Port를 정의하고, 데이터 소유 모듈이 Adapter를 구현하는 방식으로 정리했다.

### 변경

- users 모듈이 `ApprovalApproverDirectoryAdapter`를 추가해 `ApproverDirectoryPort`를 구현했다.
- approval 내부 `UserNameEntity`, `UserNameJpaRepository`, `ApproverDirectoryPortAdapter` shim을 제거했다.
- file 모듈이 `file_metadata` 조회 Entity/Repository와 `ApprovalAttachmentContentAdapter`를 추가했다.
- AI 요약은 텍스트 계열 파일 원문을 실제로 읽을 수 있을 때만 Gemini를 호출한다.
- 미지원 파일 또는 메타데이터 없음은 `APPROVAL_409_7`로 실패한다.

### 검증

- `ApprovalApproverDirectoryAdapterTest`
- `ApprovalAttachmentContentAdapterTest`
- `SummarizeApprovalAttachmentServiceTest`

## 2026-08-06 · WebSocket 알림 고정 및 Web Push 미사용

- 결재 실시간 알림은 WebSocket/STOMP로 고정한다.
- Web Push 발송, VAPID, web-push 라이브러리 연동은 구현하지 않는다.
- PushSubscription API는 호환성 때문에 남긴다.

## 2026-08-04 · 첨부파일 AI 요약 초안

- 최초 구현은 placeholder 요약 방식이었다.
- 현재는 실제 원문이 없으면 Gemini를 호출하지 않는다.
