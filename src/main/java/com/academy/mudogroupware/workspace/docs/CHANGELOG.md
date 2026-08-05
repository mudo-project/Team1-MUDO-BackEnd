# 📚 Workspace Changelog

## 2026-08-05 · 워크스페이스 목록 및 최근 접속 기능 추가 ✨

- `GET /api/workspaces?scope=MINE|ALL`로 참여 중인 워크스페이스 목록 또는 같은 학원의 전체 활성 워크스페이스 목록을 조회할 수 있습니다.
- `scope`를 생략하면 `MINE`으로 처리하며, `scope=ALL`은 `WORKSPACE:READ_ALL` 권한이 필요합니다.
- 목록에는 워크스페이스 이름과 참여자 수를 제공하고, 요청 사용자별 최근 접속 시각이 최신인 순서로 정렬합니다.
- 상세 화면을 정상적으로 연 뒤 `PUT /api/workspaces/{workspaceId}/recent-access`를 호출하면 사용자별 최근 접속 시각을 생성하거나 갱신합니다.
- `workspace_recent_access` 테이블은 사용자·워크스페이스 조합당 한 행만 유지해 접속할 때마다 새 데이터를 계속 쌓지 않습니다.

자세한 처리 흐름과 응답 형식은 [WORKSPACE_API.md](WORKSPACE_API.md), [WORKSPACE_API_FLOW.md](WORKSPACE_API_FLOW.md), [REVISION.md](REVISION.md)를 참고해주세요. 📚

## 2026-08-05 · 워크스페이스 이름 중복 생성 정책 변경 ✨

- 같은 학원에서 이미 사용 중인 워크스페이스 이름으로 새 워크스페이스를 만들 수 없습니다.
- 중복 이름을 입력하면 시스템이 임의로 `(1)`, `(2)`를 붙이지 않고 이름 중복 오류를 안내합니다.
- 생성자와 추가 참여자는 기존처럼 자동·정상 등록되며, 이름이 중복된 요청은 저장되지 않습니다.
- 워크스페이스 복사 기능이 추가될 때는 복사본에만 자동 번호를 붙이는 정책을 적용할 예정입니다.

자세한 구현 정책과 검증 내용은 [REVISION.md](REVISION.md)를 참고해주세요. 📚
