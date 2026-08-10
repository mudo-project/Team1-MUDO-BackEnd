# memo 모듈

## 책임과 범위

개인메모(personal memo) 기능을 담당한다. 사용자가 자신만 보는 메모를 생성·조회·수정·삭제하며, 보드 화면에서 자유롭게 드래그·리사이즈(자유배치)할 수 있다. 다른 사용자와 공유되지 않는 순수 개인 데이터다.

## 담당자

(팀 확인 필요)

## 소유하는 주요 데이터와 상태

- `Memo` — DB 테이블 `personal_memo` (user_id, title(최대 100자, 필수), content(nullable), color, position_x/position_y/width/height(nullable — 자유배치 전에는 비어있음), created_at, updated_at)
- `color`는 ERD상 ENUM이지만 실제 DB는 `VARCHAR(10)`으로 구현했다(messenger의 `chat_room.type` 등과 동일 컨벤션 — 네이티브 ENUM 대신 문자열).

## 외부에 공개하는 Application API

- `CreateMemoUseCase` — 메모 생성
- `MemoQueryUseCase` — 내 메모 목록 조회 (정렬: 최신순/오래된순)
- `UpdateMemoContentUseCase` — 제목/내용 수정
- `UpdateMemoColorUseCase` — 색상 변경
- `UpdateMemoPositionUseCase` — 위치·크기 변경 (자유배치 드래그·리사이즈 결과 저장)
- `DeleteMemoUseCase` — 메모 삭제

## 다른 모듈 또는 외부 시스템에 요청하는 의존성

- 현재 없음. 개인 데이터만 다루므로 messenger처럼 다른 사용자 이름을 조회하는 등의 Port가 필요하지 않다.

## 발행·소비하는 Event

- 현재 없음.

## 변경 시 주의 사항

- 생성 API는 별도 PR로 먼저 올리고, 나머지 5개(목록조회/수정/색상변경/위치변경/삭제)는 CodeRabbit 무료 리뷰 쿼터 제한(짧은 시간에 PR을 여러 번 올리면 소진됨) 때문에 하나의 PR로 묶어서 진행했다.
- 색상변경과 위치변경은 저장 시점이 서로 다르고(메뉴 클릭 1회 vs 드래그 중 연속 호출) Command를 액션 단위로 좁게 쓰는 팀 컨벤션에 맞춰 수정(내용)과는 별도 API로 분리했다.
- 수정/색상변경/위치변경/삭제는 대상 메모를 `findById`로 조회한 뒤 `Memo.isOwnedBy(userId)`로 소유권을 검증한다. 소유자가 아니면 `NOT_MEMO_OWNER`(403), 대상이 없으면 `MEMO_NOT_FOUND`(404).
- 위치·크기(`position_x/y`, `width`, `height`)는 메모 생성 시점엔 서버가 관여하지 않고 `null`로 둔다. 사용자가 보드 화면에서 "자유배치" 모드로 드래그·리사이즈한 뒤에만 별도 API로 값이 채워진다.
- `createdAt`/`updatedAt`은 JPA Auditing(`BaseTimeEntity`)을 쓰지 않고, messenger 패턴을 따라 애플리케이션 서비스가 주입받은 `Clock`으로 직접 계산해 도메인 팩토리에 전달한다.
- 도메인 규칙 위반은 `memo.domain.exception.MemoErrorCode`(→ `MemoException`, `BusinessException` 상속)로 던진다(`MEMO_{status}_{n}` 코드 체계).
- 마이그레이션 담당자번호는 `be6`(다른 messenger 마이그레이션과 동일 담당자)을 쓴다. 단 최초 커밋(`V7.1.1__create_personal_memo_table.sql`)은 번호를 잘못 매겨 이미 develop에 머지된 상태라 그대로 두고, 이후 memo 마이그레이션부터 `be6/V6.1.3`으로 이어간다.

## 세부 문서

- API.md, API_FLOW.md, CHANGELOG.md, REVISION.md는 전체 엔드포인트 구현과 테스트가 끝난 뒤 실제 코드 기준으로 추가할 예정이다.
