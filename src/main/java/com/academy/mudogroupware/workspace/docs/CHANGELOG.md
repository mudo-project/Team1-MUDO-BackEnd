# 📚 Workspace Changelog

## 2026-08-06

- 워크스페이스 이름 수정 API(`PATCH /api/workspaces/{workspaceId}`)를 추가했습니다.
- 워크스페이스 삭제 API(`DELETE /api/workspaces/{workspaceId}`)를 추가했습니다. 소프트 삭제로 처리됩니다.
- 워크스페이스는 소유자가 없는 동등한 참여자 모델로 정리되었습니다. 생성자 특별 취급은 없어지고, 마지막 남은 참여자만 탈퇴·제거가 제한됩니다.
- 워크스페이스 참여자 추가 API(`POST /api/workspaces/{workspaceId}/members`)를 추가했습니다.
- 워크스페이스 참여자 제거·자진탈퇴 API(`DELETE /api/workspaces/{workspaceId}/members/{userId}`)를 추가했습니다.

## 2026-08-05 · 업무 기한을 날짜 단위로 변경 ✨

- `task.due_at` 컬럼을 `DATETIME(6)`에서 `DATE`로 변경해 업무 기한을 날짜 단위로 저장합니다.
- `TaskJpaEntity.dueAt` 타입을 `LocalDate`로 변경하고, 반복 업무 생성 시각인 `scheduledFor`는 `LocalDateTime`으로 유지합니다.
- 기존 `due_at`의 시각 정보는 날짜로 변환되며, 업무는 기한 날짜가 지난 다음 날부터 지연 업무로 처리할 수 있습니다.
- `V3.1.4__change_task_due_at_to_date.sql` 마이그레이션과 JPA 매핑 회귀 테스트를 추가했습니다.

## 2026-08-05 · 워크스페이스 목록 및 최근 접속 기능 추가 ✨

- `GET /api/workspaces?scope=MINE|ALL`로 참여 중인 워크스페이스 목록 또는 같은 학원의 전체 활성 워크스페이스 목록을 조회할 수 있습니다.
- `scope`를 생략하면 `MINE`으로 처리하며, `scope=ALL`은 `WORKSPACE:READ_ALL` 권한이 필요합니다.
- 목록에는 워크스페이스 이름과 참여자 수를 제공하고, 요청 사용자별 최근 접속 시각이 최신인 순서로 정렬합니다.
- 상세 화면을 정상적으로 연 뒤 `PUT /api/workspaces/{workspaceId}/recent-access`를 호출하면 사용자별 최근 접속 시각을 생성하거나 갱신합니다.
- `workspace_recent_access` 테이블은 사용자·워크스페이스 조합당 한 행만 유지해 접속할 때마다 새 데이터를 계속 쌓지 않습니다.
- 같은 최근 접속 요청이 동시에 들어와도 기록 생성·갱신이 실패하지 않도록 DB upsert 방식으로 처리합니다.
- 늦게 도착한 과거 접속 기록은 더 최신의 최근 접속 시각을 덮어쓰지 않습니다.

자세한 처리 흐름과 응답 형식은 [WORKSPACE_API.md](WORKSPACE_API.md), [WORKSPACE_API_FLOW.md](WORKSPACE_API_FLOW.md), [REVISION.md](REVISION.md)를 참고해주세요. 📚

## 2026-08-05 · 워크스페이스 이름 중복 생성 정책 변경 ✨

- 같은 학원에서 이미 사용 중인 워크스페이스 이름으로 새 워크스페이스를 만들 수 없습니다.
- 중복 이름을 입력하면 시스템이 임의로 `(1)`, `(2)`를 붙이지 않고 이름 중복 오류를 안내합니다.
- 생성자와 추가 참여자는 기존처럼 자동·정상 등록되며, 이름이 중복된 요청은 저장되지 않습니다.
- 워크스페이스 복사 기능이 추가될 때는 복사본에만 자동 번호를 붙이는 정책을 적용할 예정입니다.

자세한 구현 정책과 검증 내용은 [REVISION.md](REVISION.md)를 참고해주세요. 📚
