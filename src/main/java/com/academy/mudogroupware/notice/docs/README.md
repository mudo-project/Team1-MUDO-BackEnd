# notice 모듈

## 책임과 범위

공지사항 기능을 담당한다. 작성/목록조회(고정 우선 정렬, 제목 검색)/상세조회(조회수·읽음 처리)/수정/삭제/고정·고정해제를 제공한다.

## 담당자

(팀 확인 필요)

## 소유하는 주요 데이터와 상태

- `Notice` — DB 테이블 `notice` (아직 flyway 마이그레이션 작성 중)
- `NoticeAttachment` — DB 테이블 `notice_attachment` (다중 첨부, 파일 URL/이름/타입을 직접 저장 — approval 모듈처럼 공유 file 테이블의 `file_id`를 참조하는 방식이 아니라, 팀 ERD에 이미 이 구조로 그려져 있어 그대로 따름)
- 읽음 기록 — DB 테이블 `notice_read` (notice_id + user_id 유니크, 조회수/읽은 인원 계산에 사용)

## 외부에 공개하는 Application API

- `CreateNoticeUseCase` — 공지 작성
- `UpdateNoticeUseCase` — 공지 수정 (작성자 본인만)
- `DeleteNoticeUseCase` — 공지 삭제 (현재는 작성자 본인만 — "권한을 가진 사람들"까지 허용하는 부분은 인가정책 확정 후 반영 예정)
- `PinNoticeUseCase` — 고정(작성자 본인만) / 고정 해제(현재는 임시로 전체 허용 — "권한자 모두" 조건은 인가정책 확정 후 반영 예정)
- `NoticeQueryUseCase` — 목록 조회(제목 검색, 고정 우선 정렬), 상세 조회(조회 시 조회수 증가 + 읽음 처리 자동 수행)

## 다른 모듈 또는 외부 시스템에 요청하는 의존성

- **작성자/조회자 정보(이름·역할·소속 학원) 조회**: `NoticeAuthorDirectoryPort`로 추상화. User 도메인 모듈이 아직 없어 `users` 테이블을 직접 읽는 임시 shim(`UserInfoEntity`)으로 구현되어 있다. approval 모듈에도 동일한 성격의 임시 shim이 각자 따로 있다 — User 모듈이 생기면 두 모듈 모두 정식 구현으로 교체해야 한다.
- **전체 대상 인원 수(총 읽음 분모)**: 같은 포트의 `countActiveUsers(academyId)`로 조회 (`users.status = 'ACTIVE'`인 사용자 수. `users` 모듈이 2026-08-04 `V4.1.1` 마이그레이션으로 `resign_date`를 없애고 `status` 컬럼으로 전환하면서 `UserInfoEntity`/`UserInfoJpaRepository`도 함께 맞춰 바꿨다). 특정 role 대상으로만 공지를 보내는 기능이 추가되면 이 계산 로직도 같이 바뀌어야 한다.

## 발행·소비하는 Event

- 현재 없음.

## 변경 시 주의 사항

- **카테고리(인사/시설/업무) 기능은 이번 범위에서 제외했다.** 기능명세서 텍스트에는 있었지만 실제 화면 시안에는 카테고리 필터/태그가 보이지 않아 화면 기준으로 뺐다. 필요하면 `notice`에 `category` 컬럼(또는 별도 분류 테이블) 추가 필요.
- 성공 응답은 `GlobalApiResponse<T>`로 감싸서 반환한다 (`204 No Content`는 본문 없이 그대로).
- 도메인 규칙 위반은 `global.domain.common.exception`의 `BadRequestException`/`NotFoundException`/`ForbiddenException`을 사용한다.
- 목록/상세조회 모두 요청자의 `academyId`로 스코프를 검증한다 (다른 학원 공지가 섞이거나 조회되지 않도록 — approval 모듈에서 겪었던 테넌시 격리 버그를 처음부터 반영).
- 작성 권한(원장/대표, 상황에 따라 직원도 가능)과 삭제·고정해제의 "권한을 가진 사람들" 조건은 `users.role` 값 체계가 확정되기 전까지 미반영 상태다.

## 세부 문서

- [API.md](API.md) — 엔드포인트별 요청/응답 예시, 검증 규칙, 오류 코드
- [API_FLOW.md](API_FLOW.md) — 계층별 호출 흐름
- [REVISION.md](REVISION.md) — 설계 변경 이력과 배경
- [CHANGELOG.md](CHANGELOG.md) — 사용자 관점 변경 요약
