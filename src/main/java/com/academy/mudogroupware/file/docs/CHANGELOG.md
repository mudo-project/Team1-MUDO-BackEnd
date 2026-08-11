# file 모듈 Changelog

## 2026-08-11 (2) - objectKey를 운영 IAM Prefix(tenants/{tenantId}/*)에 맞춤

- **[긴급 수정]** academyId 제거 직후의 objectKey(`uploads/{UUID}-{파일명}`)가 운영 ECS Task Role의
  IAM 정책(`tenants/{tenantId}/*` 경로에만 `s3:PutObject` 허용)과 맞지 않아, presigned URL
  발급은 성공해도 실제 S3 PUT에서 AccessDenied가 날 수 있는 상태였다.
- `GeneratePresignedUploadUrlService`가 `InstanceMetadataProperties`(`TENANT_ID` 환경변수,
  `global.infrastructure.observability`)를 주입받아 objectKey를 `tenants/{tenantId}/files/{UUID}-{파일명}`
  형태로 생성하도록 바꿨다. tenantId는 클라이언트 입력이 아니라 서버 설정에서만 읽는다.
- DB 스키마 변경이나 마이그레이션은 필요 없다.

## 2026-08-11 - 단일 학원 전환: file_metadata academy_id 제거

- **[변경]** `file_metadata`에서 `academy_id` 컬럼을 제거했다(`V1.5.14`). `FileMetadataEntity.create/restore`, `RegisterFileCommand`, `GeneratePresignedUploadUrlCommand`, `GetFileDownloadUrlUseCase.getDownloadUrl(s)`에서 academyId 파라미터를 뺐다. objectKey도 `uploads/{academyId}/...` → `uploads/...`로 바뀌었다.
- 2026-08-10에 IDOR 방지 목적으로 추가했던 academyId 스코프 체크(`findByIdAndAcademyId` 등)를 되돌렸다 — 단일 학원에서는 모든 사용자의 academyId가 항상 같은 값이라 실질적인 격리 효과가 없었다. **다만 진짜 문제(인증만 되면 fileId를 아는 누구나 다운로드 가능)는 academyId와 무관하게 여전히 남아있다.** approval은 자체 리소스 단위 권한 체크(`GetApprovalAttachmentDownloadUrlService`)로 이미 방어하고 있지만, notice 등 나머지 도메인은 아직 없다 — 별도 후속 작업 필요.
- 이 변경으로 messenger의 `SendMessageService`/`ChatMessageQueryService`, approval의 `GetApprovalAttachmentDownloadUrlService`도 academyId 전달을 뺐다.

## 2026-08-11 - approval 첨부파일 요약용 원문 조회에 PDF/이미지/docx 지원 추가

- `ApprovalAttachmentContentAdapter`가 텍스트뿐 아니라 PDF/이미지(jpeg/png/webp/heic/heif)/docx도 읽어서 반환하도록 확장했다. 반환 타입이 `String`에서 `AttachmentContent`(TEXT/BINARY)로 바뀌었다(`AttachmentContentPort` 계약 변경, approval 도메인 소유 타입).
- PDF/이미지는 바이너리 그대로(`AttachmentContent.binary`) 반환한다 — approval의 `GeminiSummarizerAdapter`가 Gemini 멀티모달 입력으로 직접 전달한다. 원본 15MB 초과 시 실패 처리한다.
- docx는 Apache POI(`XWPFDocument`/`XWPFWordExtractor`)로 텍스트를 추출해 기존 텍스트 경로(`AttachmentContent.text`)로 반환한다.
- hwp 등 나머지 형식은 계속 미지원이며 `AttachmentContentUnavailableException`으로 실패한다.

## 2026-08-10 - CodeRabbit 리뷰 반영: academy 스코프 검증 + 배치 입력 상한

- **[보안]** `file_metadata`에 `academy_id`를 추가하고(`V1.5.6`), 등록 시 요청자 학원으로 저장한다. 다운로드 URL 단건/배치 조회 모두 `academyId`가 일치하는 파일만 찾도록 `findByIdAndAcademyId`/`findAllByIdInAndAcademyId`로 변경했다. 이전엔 fileId만 알면(추측/유출) 다른 학원 사용자도 다운로드 URL을 받을 수 있었다(IDOR).
- `BatchFileDownloadUrlRequest.fileIds`에 `@Size(max = 100)`과 원소별 `@NotNull @Positive` 검증을 추가했다. 대량 배치 요청으로 인한 DB/스토리지 부하와 잘못된 값(null, 음수) 유입을 막는다.
- Port/DTO 추상화(`FileMetadataQueryPort` 도입) 제안은 스킵했다 — 이 모듈은 구현체가 하나뿐이라 지금 시점엔 과설계로 판단.
- Markdown heading 중복/계층 지적은 스킵했다 — 팀 공용 `docs/templates/API_SPEC_TEMPLATE.md` 자체가 이 형식(섹션마다 `[request]`/`[response]` 반복)을 표준으로 쓰고 있어, 이 문서만 바꾸면 다른 도메인 문서들과 형식이 어긋난다.

## 2026-08-10 - 다운로드 URL 일괄 조회 API 추가

- `POST /api/files/download-urls`를 추가했다. `GetFileDownloadUrlUseCase.getDownloadUrls(List<Long>)`가 `fileMetadataJpaRepository.findAllById(...)`로 한 번에 조회 후 각각 presign한다.
- messenger가 메시지 목록을 조회할 때 여러 메시지의 첨부파일 fileId를 fileId 개수만큼 반복 호출하지 않고 한 번에 풀어야 해서 요청받아 추가함(messenger 담당자 제안).
- 요청한 fileIds 중 존재하지 않는 ID는 결과 Map에서 조용히 빠진다(전체 요청을 실패시키지 않음).

## 2026-08-09 - 업로드/등록/다운로드 URL API 신설

- `POST /api/files/presigned-url`, `POST /api/files`, `GET /api/files/{fileId}/download-url`을 추가했다. 이전엔 파일을 업로드해서 `fileId`를 발급받는 경로가 전혀 없어서, approval의 `fileIds`/notice의 첨부파일 입력 필드를 프론트에서 채울 방법이 없었다.
- `FileMetadataEntity`에 `create()` 팩토리를 추가했다(기존엔 `restore()`만 있어 신규 생성이 불가능했다).
- `FileMetadataEntity`/`FileMetadataJpaRepository`를 `file.infrastructure.approval` 패키지에서 `file.infrastructure.persistence`로 옮겼다. approval 전용이 아니라 notice 등 다른 도메인도 참조하는 공용 클래스이기 때문이다.
- notice의 첨부파일도 이 모듈의 `fileId` 방식으로 통일했다(자세한 내용은 notice CHANGELOG 참고).
