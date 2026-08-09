# 📚 Workspace Changelog

## 2026-08-10

- 업무 상세 조회 API(`GET /api/workspaces/{workspaceId}/tasks/{taskId}`)를 추가했습니다. 제목·등록자·등록일·상태·기한·최종 상태 변경일시를 반환합니다. **최종 상태 변경자(누가 바꿨는지)는 응답에 포함하지 않습니다** — 이력 자체는 계속 저장되지만 노출은 하지 않기로 프론트와 합의했습니다. 한 번도 상태가 바뀌지 않은 업무는 `lastStatusChangedAt` 필드가 응답에서 생략됩니다.
- 댓글 목록 조회 API(`GET /api/workspaces/{workspaceId}/tasks/{taskId}/comments`)를 추가했습니다. 내용·작성자·완료 여부·생성일을 반환하며, `createdAt` 오름차순(오래된 댓글 먼저)으로 페이지네이션됩니다(기본 20개, 무한스크롤 대응). 업무 상세 조회와는 별도 엔드포인트로 분리했습니다(갱신 주기가 다르고, 무한스크롤에 페이지네이션이 필요하기 때문). 완료일시·멘션 목록은 이 응답에 포함하지 않습니다.
- `Task` 도메인 모델에 `createdAt` 필드를 추가했습니다. 기존 8-arg `restore(...)` 호출부를 전부 바꾸는 대신, `createdAt`을 받는 9-arg 오버로드를 추가해 하위 호환을 유지했습니다.
- `TaskRepository`에 락 없는 단건 조회 `findById(workspaceId, taskId)`를 추가했습니다. 조회 전용 API가 매번 비관적 락(`findByIdForUpdate`)을 잡지 않도록 분리했습니다.
- `TaskStatusHistoryRepository`에 `findLatestChangedAt(taskId)`를 추가했습니다. 변경자는 조회하지 않고 시각만 반환합니다.
- `TaskCommentRepository`에 페이지네이션 목록 조회 `findAllByTaskId(taskId, page, size)`를 추가했습니다. `createdAt asc, id asc`로 정렬합니다.
- 댓글 목록 조회의 `page`/`size` 쿼리 파라미터에 `@Min`/`@Max` 검증을 추가했습니다(반복 업무 템플릿 목록 API와 동일한 패턴). 검증이 없으면 `PageRequest.of()`의 `IllegalArgumentException`이 처리되지 않아 `500`으로 응답하는 문제가 있었습니다(코드 리뷰에서 발견).

## 2026-08-09

- 반복 업무 템플릿 삭제 API(`DELETE /api/workspaces/{workspaceId}/recurring-templates/{templateId}`)를 추가했습니다. 하드 삭제이며 복구할 수 없습니다. 템플릿으로 이미 생성된 업무는 삭제되지 않고 일반 업무로 남습니다.
- 워크스페이스 삭제, 참여자 제거, 업무 삭제, 업무 댓글 삭제 API의 응답이 빈 본문(`204 No Content`)에서 성공 메시지가 담긴 본문(`200 OK`)으로 바뀌었습니다.
- 반복 업무 템플릿 수정 API(`PATCH /api/workspaces/{workspaceId}/recurring-templates/{templateId}`)를 추가했습니다. 제목 단독 또는 반복 주기(`recurrenceType`+`recurrenceRule`) 세트로 수정하며, 한쪽만 보내면 다른 쪽은 기존 값을 유지합니다. 둘 다 생략하거나 주기 세트 중 하나만 보내면 `400`입니다.
- 공백만으로 이루어진 제목은 수정 요청에서도 거부합니다(생성 API와 동일한 제약).
- 반복 업무 템플릿 수정·삭제 API는 `findByWorkspaceIdAndIdForUpdate`(비관적 락) 조회를 공유해 동시 요청을 직렬화합니다.
- workspace 도메인 Service의 완료(`_완료`) 로그를 트랜잭션 커밋 이후에만 남기도록 `AfterCommitLogger`를 도입했습니다. 저장 직후 로그를 남기면 이후 커밋 시점에 제약조건 위반 등으로 롤백돼도 성공 로그가 남아 실패를 성공으로 오인할 수 있었습니다.
- workspace 도메인 Service 20개 중 로깅 컨벤션(`docs/LOGGING_CONVENTION.md`)이 적용되지 않았던 17개(comment 4, task 4, workspace 9)에 시작/완료 로그를 소급 적용했습니다.

## 2026-08-08

- 업무 수정·삭제·댓글 API의 비관적 락 조회를 워크스페이스 범위로 제한했습니다. 다른 워크스페이스의 업무 번호로는 락 자체가 잡히지 않아, 무관한 워크스페이스 간 락 경합이 발생하지 않습니다.
- 위 조회를 2단계로 세분화했습니다 — ① 락 없는 일반 조회로 워크스페이스 소속을 먼저 확인하고(다른 트랜잭션의 배타 락을 기다리지 않음), ② 소속이 확인된 업무에 대해서만 비관적 락을 겁니다. `WHERE task_id = ? AND workspace_id = ?` 형태의 단일 락 조회는 PK(`task_id`) 조회 특성상 대상 행이 이미 다른 트랜잭션에 잠겨 있으면 workspace 일치 여부를 확인하기 전에 대기할 수 있어(코드래빗 리뷰 지적), 이를 근본적으로 막기 위한 조치입니다.

## 2026-08-07

- 업무 생성 API(`POST /api/workspaces/{workspaceId}/tasks`)를 추가했습니다. 제목과 마감일을 입력하며, 마감일이 오늘 이전이면 최초 상태를 `DELAYED`로, 그 외에는 `WAITING`으로 저장합니다.
- 업무를 생성하면 상태 이력 1건을 함께 저장합니다. `previous_status = NULL`, `changed_by = 생성자`입니다.
- 일반 업무의 마감일은 필수입니다. 과거 날짜도 지정할 수 있습니다.
- 업무 상태·마감일 수정 API(`PATCH /api/workspaces/{workspaceId}/tasks/{taskId}`)를 추가했습니다. 상태와 마감일 중 최소 하나를 보내야 합니다.
- 업무 상태 전이 규칙을 두 문장으로 정리했습니다. ① 완료된 업무는 지연으로 바꿀 수 없습니다. ② 기한이 지난 업무를 대기·진행 중으로 되돌릴 때는 오늘 이후의 새 마감일을 함께 입력해야 합니다. 그 외 전이는 모두 허용합니다.
- 완료된 업무를 대기·진행 중으로 되돌릴 수 있습니다. 잘못 누른 완료를 복구할 수 있습니다.
- 사용자가 미완료 업무를 직접 `DELAYED`로 옮길 수 있습니다. 업무를 의도적으로 미루는 경우를 상태로 표현하고, 누가 미뤘는지 이력에 남습니다.
- 마감일만 수정하면 상태는 바뀌지 않습니다. 같은 상태로의 전이 요청은 성공하되 상태 이력을 남기지 않습니다.
- 반복 업무는 마감일을 쓰지 않으므로 마감일 수정을 허용하지 않고, 새 마감일 요구 규칙도 면제합니다.
- 업무 삭제 API(`DELETE /api/workspaces/{workspaceId}/tasks/{taskId}`)를 추가했습니다. 하드 삭제이며 업무 댓글·댓글 멘션·상태 변경 이력이 함께 삭제되고 복구할 수 없습니다.
- 반복 업무의 회차를 삭제하면 같은 트랜잭션에서 `recurring_task_skip`에 기록을 남겨 이후 스케줄러가 같은 회차를 다시 만들지 않도록 합니다. 같은 기록이 이미 있으면 멱등적으로 처리합니다.
- 업무 자동 지연 처리 스케줄러를 Task 도메인 모델·포트 기반으로 이관했습니다. 동작은 그대로이며, Application 계층이 JPA Repository·Entity를 직접 참조하던 계층 위반이 해소되었습니다.

## 2026-08-06

- 워크스페이스 이름 수정 API(`PATCH /api/workspaces/{workspaceId}`)를 추가했습니다.
- 워크스페이스 삭제 API(`DELETE /api/workspaces/{workspaceId}`)를 추가했습니다. 소프트 삭제로 처리됩니다.
- 워크스페이스는 소유자가 없는 동등한 참여자 모델로 정리되었습니다. 생성자 특별 취급은 없어지고, 마지막 남은 참여자만 탈퇴·제거가 제한됩니다.
- 워크스페이스 참여자 추가 API(`POST /api/workspaces/{workspaceId}/members`)를 추가했습니다.
- 워크스페이스 참여자 제거·자진탈퇴 API(`DELETE /api/workspaces/{workspaceId}/members/{userId}`)를 추가했습니다.
- 워크스페이스 복구 API(`POST /api/workspaces/{workspaceId}/recover`)를 추가했습니다. 삭제 당시 참여자만 복구할 수 있고, 이름이 충돌하면 시각 접미사를 붙입니다.
- 업무 자동 지연 처리 스케줄러를 추가했습니다. 매일 KST 00:00에 기한이 지난 미완료 일반 업무와 예정일이 지난 미완료 반복 업무를 `DELAYED`로 자동 전환하고, 전환마다 시스템 상태 이력(`changed_by = NULL`)을 한 건 저장합니다. 이미 `DELAYED`인 업무는 다시 전환하지 않으므로 재실행해도 상태 이력이 중복되지 않습니다.

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
