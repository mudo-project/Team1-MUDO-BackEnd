# file 모듈 Changelog

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
