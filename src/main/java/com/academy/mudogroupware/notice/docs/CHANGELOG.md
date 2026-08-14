# 📚 Notice Changelog

## 2026-08-09 · 첨부파일 참조 방식을 fileUrl → fileId로 통일

- `notice_attachment`의 `file_url`/`file_type` 컬럼을 제거하고 `file_id`(공유 `file_metadata` 참조, FK)를 추가했다(`V1.5.5`).
- 이전엔 프론트가 `fileUrl`을 직접 채워서 보내야 했는데, 파일을 업로드해서 URL을 받는 API 자체가 없어 실제로 채울 수 없는 값이었다. approval의 `fileIds`와 동일하게 `file` 모듈에서 발급하는 `fileId`를 참조하도록 통일했다 (`POST /api/files/presigned-url` → S3 업로드 → `POST /api/files`로 fileId 발급).
- `NoticeAttachmentRequest`/`NoticeAttachmentResponse`에서 `fileUrl`/`fileType` 필드를 제거하고 `fileId`를 추가했다. 다운로드 URL이 필요하면 `GET /api/files/{fileId}/download-url`을 별도로 호출한다.

## 2026-08-08 · 공지 권한 정책 반영

- 공지 작성/수정은 `NOTICE:WRITE` 권한으로 제한했습니다.
- 공지 고정/고정 해제는 `NOTICE:PIN` 권한으로 제한했습니다.
- 공지 삭제는 정책대로 작성자 본인만 가능하게 유지했습니다.

---

## 2026-08-04 · 보안 리뷰 반영 (고정 해제 학원 스코프, 읽음 기록 동시성) 🔒

- 다른 학원 소속 사용자가 공지 고정 해제를 할 수 없도록 막았습니다.
- 같은 공지를 여러 탭/기기에서 동시에 처음 열었을 때 오류가 날 수 있던 문제를 고쳤습니다.

자세한 내용은 [REVISION.md](REVISION.md)를 참고해주세요. 🔒

---

## 2026-08-04 · 저장 시각을 한국 시간(KST)으로 고정 🕒

- 공지사항을 작성/수정하거나 읽음 처리될 때 기록되는 시각이, 서버가 실행되는 환경(UTC)에 영향받지 않고 항상 한국 시간으로 저장되도록 고쳤습니다. (approval에서 먼저 고쳤던 것을 notice에도 뒤늦게 반영)
- API 응답 형식은 그대로지만, 실제 시각 값은 이전보다 9시간 보정되어 표시됩니다.

자세한 내용은 [REVISION.md](REVISION.md)를 참고해주세요. 🕒

---

## 2026-08-04 · 오류 코드 정비 및 목록 페이지네이션 📄

- 공지사항 관련 API 오류 응답에 상황별로 구분되는 전용 오류 코드(`NOTICE_...`)가 붙습니다.
- 공지 목록 조회가 `page`/`size` 파라미터로 페이지 단위 조회를 지원합니다.

자세한 내용은 [REVISION.md](REVISION.md)와 [API.md](API.md)를 참고해주세요. 📄

---

## 2026-08-04 · 계정 상태 컬럼 변경 대응 🔧

- 사용자 계정 관리 방식이 "퇴사일 기록"에서 "상태(재직중/퇴사/휴직) 기록"으로 바뀌면서, 공지 읽음 대상 인원 수를 세는 방식도 함께 맞춰 고쳤습니다. 화면에 보이는 값이나 동작은 달라지지 않습니다.

자세한 배경은 [REVISION.md](REVISION.md)를 참고해주세요. 🔧

---

## 2026-08-03 · 공지사항 기능 최초 추가 ✨

- 공지사항 작성/목록조회/상세조회/수정/삭제 기능이 추가되었습니다.
- 공지를 상단에 고정하거나 고정을 해제할 수 있습니다.
- 사진뿐 아니라 PDF 등 파일을 여러 개 첨부할 수 있습니다.
- 상세 화면에서 조회수와 "몇 명이 읽었는지"를 확인하고, 실제로 읽은 사람 목록도 조회할 수 있습니다. 🔍
- 제목으로 공지를 검색할 수 있습니다.
- 다른 학원의 공지는 보이지 않도록 처음부터 분리했습니다. 🔒
- (참고) 공지 카테고리(인사/시설/업무) 분류 기능은 이번에는 포함되지 않았습니다.

자세한 설계 배경은 [REVISION.md](REVISION.md)를 참고해주세요. 📚
# 2026-08-14 notice deletion lifecycle

- Notice delete now stores `deleted_at` and `retention_until` instead of immediately hard-deleting the row.
- General list/detail queries return only notices whose `deleted_at` is null.
- The shared retention scheduler runs `notice_retention` to clean expired notice read rows, attachment links, and notice rows.
- After the notice cleanup transaction commits, attached `fileId`s are sent to the file module; S3 objects and `file_metadata` rows are deleted only when no remaining domain references the file.
