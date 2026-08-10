# file 모듈 Changelog

## 2026-08-10 - 다운로드 URL 일괄 조회 API 추가

- `POST /api/files/download-urls`를 추가했다. `GetFileDownloadUrlUseCase.getDownloadUrls(List<Long>)`가 `fileMetadataJpaRepository.findAllById(...)`로 한 번에 조회 후 각각 presign한다.
- messenger가 메시지 목록을 조회할 때 여러 메시지의 첨부파일 fileId를 fileId 개수만큼 반복 호출하지 않고 한 번에 풀어야 해서 요청받아 추가함(messenger 담당자 제안).
- 요청한 fileIds 중 존재하지 않는 ID는 결과 Map에서 조용히 빠진다(전체 요청을 실패시키지 않음).

## 2026-08-09 - 업로드/등록/다운로드 URL API 신설

- `POST /api/files/presigned-url`, `POST /api/files`, `GET /api/files/{fileId}/download-url`을 추가했다. 이전엔 파일을 업로드해서 `fileId`를 발급받는 경로가 전혀 없어서, approval의 `fileIds`/notice의 첨부파일 입력 필드를 프론트에서 채울 방법이 없었다.
- `FileMetadataEntity`에 `create()` 팩토리를 추가했다(기존엔 `restore()`만 있어 신규 생성이 불가능했다).
- `FileMetadataEntity`/`FileMetadataJpaRepository`를 `file.infrastructure.approval` 패키지에서 `file.infrastructure.persistence`로 옮겼다. approval 전용이 아니라 notice 등 다른 도메인도 참조하는 공용 클래스이기 때문이다.
- notice의 첨부파일도 이 모듈의 `fileId` 방식으로 통일했다(자세한 내용은 notice CHANGELOG 참고).
