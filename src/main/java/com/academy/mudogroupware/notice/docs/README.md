# notice 모듈

## 책임과 범위

공지사항 기능을 담당한다. 작성/목록조회(고정 우선 정렬, 제목 검색)/상세조회(조회수·읽음 처리)/수정/삭제/고정·고정해제를 제공한다.

## 담당자

(팀 확인 필요)

## 소유하는 주요 데이터와 상태

- `Notice` — DB 테이블 `notice` (`V1.3.1__create_notice_tables.sql`)
- `NoticeAttachment` — DB 테이블 `notice_attachment` (다중 첨부, `file_id`(공유 `file_metadata` 참조) + `file_name`을 저장. 2026-08-09 이전엔 `file_url`을 직접 입력받는 구조였으나, 업로드 API 자체가 없어 실제로 채울 수 없는 값이었다. approval과 동일하게 `file` 모듈이 발급하는 `fileId`를 참조하는 방식으로 통일했다 — `V1.5.5`, 자세한 내용은 [CHANGELOG.md](CHANGELOG.md))
- 읽음 기록 — DB 테이블 `notice_read` (notice_id + user_id 유니크, 조회수/읽은 인원 계산에 사용)

## 외부에 공개하는 Application API

- `CreateNoticeUseCase` — 공지 작성 (`NOTICE:WRITE` 필요)
- `UpdateNoticeUseCase` — 공지 수정 (`NOTICE:WRITE` 필요 + 작성자 본인만)
- `DeleteNoticeUseCase` — 공지 삭제 (작성자 본인만)
- `PinNoticeUseCase` — 고정/고정 해제 (`NOTICE:PIN` 필요)
- `NoticeQueryUseCase` — 목록 조회(제목 검색, 고정 우선 정렬), 상세 조회(조회 시 조회수 증가 + 읽음 처리 자동 수행)

## 다른 모듈 또는 외부 시스템에 요청하는 의존성

- **작성자/조회자 정보(이름·역할·소속 학원) 조회**: `NoticeAuthorDirectoryPort`로 추상화. User 도메인 모듈이 아직 없어 `users` 테이블을 직접 읽는 임시 shim(`UserInfoEntity`)으로 구현되어 있다. approval 모듈에도 동일한 성격의 임시 shim이 각자 따로 있다 — User 모듈이 생기면 두 모듈 모두 정식 구현으로 교체해야 한다.
- **전체 대상 인원 수(총 읽음 분모)**: 같은 포트의 `countActiveUsers(academyId)`로 조회 (`users.status = 'ACTIVE'`인 사용자 수. `users` 모듈이 2026-08-04 `V4.1.1` 마이그레이션으로 `resign_date`를 없애고 `status` 컬럼으로 전환하면서 `UserInfoEntity`/`UserInfoJpaRepository`도 함께 맞춰 바꿨다). 특정 role 대상으로만 공지를 보내는 기능이 추가되면 이 계산 로직도 같이 바뀌어야 한다.

## 발행·소비하는 Event

- `NoticeAttachmentFilesCleanupRequestedEvent`: 공지 retention 배치가 만료된 공지의 읽음 기록·첨부 연결·본문을 정리한 뒤, 정리 대상 첨부 `fileId` 목록을 파일 모듈에 전달하기 위해 발행한다. 파일 모듈은 이 이벤트를 커밋 후 소비해 다른 도메인 참조 여부를 확인하고, 더 이상 참조되지 않는 S3 객체와 `file_metadata`를 정리한다.

## 변경 시 주의 사항

- **카테고리(인사/시설/업무) 기능은 이번 범위에서 제외했다.** 기능명세서 텍스트에는 있었지만 실제 화면 시안에는 카테고리 필터/태그가 보이지 않아 화면 기준으로 뺐다. 필요하면 `notice`에 `category` 컬럼(또는 별도 분류 테이블) 추가 필요.
- 성공 응답은 `GlobalApiResponse<T>`로 감싸서 반환한다 (`204 No Content`는 본문 없이 그대로).
- 도메인 규칙 위반은 `notice.domain.exception.NoticeErrorCode`(→ `NoticeException`, `BusinessException` 상속)로 던진다. `users`/`auth`, approval 모듈의 선례를 따랐다 (`NOTICE_{status}_{n}` 코드 체계, [API.md](API.md) 참고).
- 목록/상세조회 모두 요청자의 `academyId`로 스코프를 검증한다 (다른 학원 공지가 섞이거나 조회되지 않도록 — approval 모듈에서 겪었던 테넌시 격리 버그를 처음부터 반영).
- 목록 조회(`getNotices`)는 `page`/`size` 쿼리 파라미터 기반 Slice 페이지네이션을 지원한다 (`API_CONTRACT.md` 규칙 반영).
- 작성/수정은 `NOTICE:WRITE`, 고정/고정 해제는 `NOTICE:PIN` 권한으로 제한한다. 삭제는 별도 관리자 권한 없이 작성자 본인만 가능하다.
- `Notice.create()`/`update()`, `NoticeReadRepositoryImpl.markRead()`는 `LocalDateTime.now()`를 직접 호출하지 않고 `Clock`(`Asia/Seoul` 고정) 기반 시각을 파라미터로 받는다 — approval 모듈에서 먼저 고친 서버 시간대(UTC) 버그를 notice에도 동일하게 반영했다 ([REVISION.md](REVISION.md) 참고).

## 데이터 생명주기 정책

- 공지사항은 삭제 요청 후 바로 완전 삭제하지 않고 일정 기간 일반 조회에서 숨긴 뒤 정리하는 도메인이다.
- 현재 삭제 API는 즉시 하드 삭제하지 않고 `deleted_at`, `retention_until`을 채워 일반 조회에서 숨긴다.
- 공통 retention 배치가 `retention_until`이 지난 공지의 읽음 기록·첨부 연결·본문을 정리하고, 첨부 `fileId`는 커밋 후 파일 모듈에 S3 객체/메타데이터 정리를 요청한다.
- 파일 모듈은 해당 `fileId`가 공지·결재·템플릿·메신저 어디에서도 참조되지 않을 때만 S3 객체와 `file_metadata`를 함께 삭제한다.
- 담당 도메인 기준은 [DATA_LIFECYCLE_POLICY.md](../../../../../../../../docs/DATA_LIFECYCLE_POLICY.md)를 따른다.

## 세부 문서

- [API.md](API.md) — 엔드포인트별 요청/응답 예시, 검증 규칙, 오류 코드
- [API_FLOW.md](API_FLOW.md) — 계층별 호출 흐름
- [REVISION.md](REVISION.md) — 설계 변경 이력과 배경
- [CHANGELOG.md](CHANGELOG.md) — 사용자 관점 변경 요약
