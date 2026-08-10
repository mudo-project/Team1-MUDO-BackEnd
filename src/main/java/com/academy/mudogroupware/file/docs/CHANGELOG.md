# file 모듈 Changelog

## 2026-08-09 - 업로드/등록/다운로드 URL API 신설

- `POST /api/files/presigned-url`, `POST /api/files`, `GET /api/files/{fileId}/download-url`을 추가했다. 이전엔 파일을 업로드해서 `fileId`를 발급받는 경로가 전혀 없어서, approval의 `fileIds`/notice의 첨부파일 입력 필드를 프론트에서 채울 방법이 없었다.
- `FileMetadataEntity`에 `create()` 팩토리를 추가했다(기존엔 `restore()`만 있어 신규 생성이 불가능했다).
- `FileMetadataEntity`/`FileMetadataJpaRepository`를 `file.infrastructure.approval` 패키지에서 `file.infrastructure.persistence`로 옮겼다. approval 전용이 아니라 notice 등 다른 도메인도 참조하는 공용 클래스이기 때문이다.
- notice의 첨부파일도 이 모듈의 `fileId` 방식으로 통일했다(자세한 내용은 notice CHANGELOG 참고).
