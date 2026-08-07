# file 모듈

S3 기반 파일 저장소 접근과 파일 메타데이터 조회를 담당한다.

## 책임과 범위

- `FileStoragePort`: objectKey 기준 presigned URL 생성, 다운로드, 삭제.
- `file_metadata`: `fileId -> objectKey/contentType` 조회용 메타데이터.
- `ApprovalAttachmentContentAdapter`: approval의 `AttachmentContentPort`를 구현한다.

## approval 연동

첨부파일 AI 요약 시 approval은 file 저장 구조를 직접 알지 않는다.

```text
approval SummarizeApprovalAttachmentService
-> AttachmentContentPort.loadContent(fileId)
-> file ApprovalAttachmentContentAdapter
-> file_metadata 조회
-> S3 objectKey 다운로드
-> UTF-8 텍스트 반환
```

현재 지원 contentType:

- `text/*`
- `application/json`
- `application/xml`
- `application/csv`
- `application/x-www-form-urlencoded`

미지원 파일(PDF 등), 메타데이터 없음, S3 다운로드 실패는 `AttachmentContentUnavailableException`으로 변환되어 approval에서 `APPROVAL_409_7`로 처리된다.
