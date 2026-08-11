# file 모듈

S3 기반 파일 저장소 접근, 업로드/등록, 파일 메타데이터 조회를 담당한다. 특정 도메인(공지, 결재 등)에 종속되지 않는 공용 모듈이다.

## 책임과 범위

- `FileStoragePort`: objectKey 기준 presigned URL 생성(업로드/다운로드), 다운로드, 삭제.
- `file_metadata`: `fileId -> objectKey/contentType` 저장 및 조회용 메타데이터. 프로젝트가 단일 학원으로 전환되면서 `academyId` 컬럼은 제거했다(`V1.5.14`, 원래 `V1.5.6`에서 IDOR 방지용으로 추가했었다).
- `POST /api/files/presigned-url`, `POST /api/files`, `GET /api/files/{fileId}/download-url`: 업로드 → 등록 → 다운로드 흐름을 제공하는 공용 API. 자세한 내용은 [API.md](API.md) 참고.
- `ApprovalAttachmentContentAdapter`: approval의 `AttachmentContentPort`를 구현한다.

## 업로드 흐름 (2026-08-09 추가)

파일을 직접 서버로 전송하지 않고, S3 presigned URL로 클라이언트가 직접 업로드하는 방식이다.

```text
1. POST /api/files/presigned-url  { fileName, contentType }
   -> objectKey, uploadUrl 반환 (이 시점엔 file_metadata row 없음)
2. 클라이언트가 uploadUrl로 S3에 파일을 직접 PUT
3. POST /api/files  { objectKey, contentType }
   -> file_metadata row 생성, fileId 반환
4. 이후 다른 기능(결재 신청 fileIds, 공지 첨부 fileId 등)에서 이 fileId를 참조
```

- 3번 등록 시점에 실제 S3 객체 존재 여부를 검증하지 않는다. 잘못된 objectKey로 등록하면 이후 다운로드/요약 시점에 실패로 드러난다.
- objectKey는 `uploads/{UUID}-{파일명}` 형태로 서버가 생성한다. 클라이언트가 임의 경로를 지정할 수 없다.
- 다운로드 URL 조회(단건/배치)는 fileId만으로 조회한다. **인증만 되면 fileId를 아는(추측/유출된) 누구나 다운로드 URL을 받을 수 있다.** approval처럼 리소스 단위 권한 체크가 필요한 도메인은 자체 엔드포인트를 앞단에 둬야 한다(`approval.GetApprovalAttachmentDownloadUrlService` 참고). notice 등 나머지 도메인은 아직 이런 체크가 없다 — 알려진 갭.

## approval 연동

첨부파일 AI 요약 시 approval은 file 저장 구조를 직접 알지 않는다.

```text
approval SummarizeApprovalAttachmentService
-> AttachmentContentPort.loadContent(fileId)
-> file ApprovalAttachmentContentAdapter
-> file_metadata 조회
-> S3 objectKey 다운로드
-> contentType에 따라 AttachmentContent.text(...) 또는 AttachmentContent.binary(...) 반환
```

contentType별 처리 (2026-08-11 확장):

| 구분 | contentType | 처리 |
| --- | --- | --- |
| 텍스트 | `text/*`, `application/json`, `application/xml`, `application/csv`, `application/x-www-form-urlencoded` | UTF-8 디코딩 후 `AttachmentContent.text` |
| docx | `application/vnd.openxmlformats-officedocument.wordprocessingml.document` | Apache POI(`XWPFDocument`)로 텍스트 추출 후 `AttachmentContent.text` |
| PDF/이미지 | `application/pdf`, `image/jpeg`, `image/png`, `image/webp`, `image/heic`, `image/heif` | 바이너리 그대로 `AttachmentContent.binary`(15MB 초과 시 실패) |

미지원 contentType(hwp 등), 메타데이터 없음, S3 다운로드 실패, PDF/이미지 15MB 초과, docx 텍스트 추출 실패는 모두 `AttachmentContentUnavailableException`으로 변환되어 approval에서 `APPROVAL_409_7`로 처리된다. `AttachmentContent.binary`로 반환된 바이너리는 approval의 `GeminiSummarizerAdapter`가 Gemini 멀티모달 입력(inline base64)으로 그대로 전달한다.

## notice 연동

공지 첨부파일(`notice_attachment`)도 이 모듈이 발급한 `fileId`를 참조한다(`file_id` 컬럼). 다운로드 URL이 필요하면 `GET /api/files/{fileId}/download-url`을 호출한다. 이전엔 `file_url`을 직접 입력받는 방식이었으나, 업로드 API가 없어 실제로 채울 수 없는 값이었기 때문에 이번에 fileId 방식으로 통일했다(자세한 내용은 [notice CHANGELOG](../../notice/docs/CHANGELOG.md) 참고).
