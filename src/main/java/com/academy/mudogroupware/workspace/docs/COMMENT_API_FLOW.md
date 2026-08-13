# 💬 업무 댓글·멘션 API 호출 흐름

> `TASK_API_FLOW.md`가 업무(Task) 자체의 호출 흐름을 다루듯, 이 문서는 업무 댓글·멘션 CRUD API(`WorkspaceTaskCommentController`)의 호출 흐름만 모은다. API 명세(요청/응답/에러코드)는 `COMMENT_API.md`를 참고한다.

## 🆕 댓글 생성 API 흐름

```text
POST /api/workspaces/{workspaceId}/tasks/{taskId}/comments
  → Security Filter
  → AuthUser
  → WorkspaceTaskCommentController
  → CreateTaskCommentRequest
  → CreateTaskCommentCommand
  → CreateTaskCommentUseCase
  → CreateTaskCommentService
  → WorkspaceRepository.findById (락 없음)
  → WorkspacePersistenceAdapter
  → TaskRepository.findByIdForUpdate (락 없는 소속 확인 → 비관적 락, 2단계)
  → TaskPersistenceAdapter
  → TaskComment.create (Domain Model)
  → TaskCommentRepository.save
  → TaskCommentPersistenceAdapter
  → TaskCommentJpaRepository / TaskCommentMentionJpaRepository
  → ApplicationEventPublisher.publishEvent (TaskCommentMentionedEvent)
  → 트랜잭션 커밋
  → WorkspaceWebSocketNotifier (AFTER_COMMIT)
  → WebSocketEventPublisher
  → /topic/workspaces/users/{recipientUserId}
```

### 1. 요청 검증과 Command 변환

`CreateTaskCommentRequest`는 `content`를 `@NotBlank`로 검증하고, Compact Constructor에서 미리 trim한다. `mentionedUserIds`는 `null`이면 빈 리스트로 취급한다(생략 가능). `toCommand`가 `CreateTaskCommentCommand`로 변환한다.

### 2. 워크스페이스 존재 확인과 참여자 검증

`CreateTaskCommentService`는 Task API와 동일한 순서를 따른다 — `WorkspaceRepository.findById`(락 없음)로 조회 후 없으면 `WorkspaceNotFoundException`(`404_1`), 참여자가 아니면 `WorkspaceAccessDeniedException`(`403_1`).

### 3. 업무 조회 (워크스페이스 범위 2단계 조회)

`TaskRepository.findByIdForUpdate(workspaceId, taskId)`로 댓글을 달 업무를 조회한다. `TASK_API_FLOW.md`의 수정·삭제 흐름과 동일한 메커니즘 — 락 없는 소속 확인 후 소속이 맞을 때만 비관적 락을 건다. 다른 워크스페이스 소속이거나 존재하지 않는 taskId는 이 시점에 `TaskNotFoundException`(`404_3`)이 발생한다.

### 4. 멘션 대상 검증

`workspace.getMemberIds().containsAll(command.mentionedUserIds())`로 멘션 대상 전원이 워크스페이스 참여자인지 확인한다. 하나라도 아니면 `InvalidMentionedUserException`(`400_6`).

### 5. 도메인 생성과 저장

`TaskComment.create(taskId, authorId, content, mentionedUserIds, now)`가 댓글과 멘션 목록을 함께 만든다(`now`는 `CreateTaskCommentService`가 소유한 `Clock`으로 계산). 내용이 공백만 있으면 도메인이 `BadRequestException`(`COMMON_400_1`)을 던진다(Request 계층 `@NotBlank`와 이중 방어). `TaskCommentRepository.save`는 `TaskCommentPersistenceAdapter`가 구현하며, 신규 댓글이므로 `TaskCommentJpaEntity.create` 후 멘션 목록을 함께 저장한다.

### 6. 멘션 알림 이벤트 발행

댓글 저장이 성공하면 요청자 자신을 제외한 멘션 사용자 ID를 입력 순서대로 중복 제거해 `TaskCommentMentionedEvent`를 발행한다. 외부 수신자가 없으면 이벤트를 발행하지 않는다. `WorkspaceWebSocketNotifier`는 트랜잭션 커밋 이후(`AFTER_COMMIT`) 이벤트를 받아 수신자마다 `/topic/workspaces/users/{recipientUserId}`로 `TASK_COMMENT_MENTIONED` payload를 전송한다. 댓글 트랜잭션이 롤백되면 WebSocket 알림도 전송되지 않는다.

```json
{
  "eventType": "TASK_COMMENT_MENTIONED",
  "workspaceId": 1,
  "taskId": 101,
  "taskTitle": "상담 일지 작성",
  "commentId": 501,
  "actorUserId": 10,
  "recipientUserId": 11,
  "occurredAt": "2026-08-12T10:30:00"
}
```

payload는 수신자별로 생성되므로 `recipientUserId`는 구독 topic의 사용자 ID와 일치한다. 한 수신자의 WebSocket 발행이 실패하면 실패 정보를 로그로 남기고 다음 수신자 발행을 계속한다. 별도 영속 알림과 재시도 큐는 이번 범위에 포함하지 않는다.

### 7. 응답

성공하면 Controller가 `GlobalApiResponse.created(WorkspaceResponseCode.TASK_COMMENT_CREATED, ...)`로 HTTP `201 Created`와 `TaskCommentResponse`(댓글 전체 필드 + `mentionedUserIds`)를 반환한다.

## 🔍 댓글 목록 조회 API 흐름

```text
GET /api/workspaces/{workspaceId}/tasks/{taskId}/comments?page=&size=
  → Security Filter
  → AuthUser
  → WorkspaceTaskCommentController (@Validated, page @Min(0), size @Min(1) @Max(100))
  → TaskCommentListQueryUseCase
  → TaskCommentListQueryService @Transactional(readOnly = true)
  → WorkspaceRepository.findById (락 없음)
  → WorkspacePersistenceAdapter
  → TaskRepository.findById (락 없음, 워크스페이스 범위)
  → TaskPersistenceAdapter
  → TaskCommentRepository.findAllByTaskId (페이지네이션, createdAt asc·id asc)
  → TaskCommentPersistenceAdapter
  → WorkspaceUserInfoPort.findUserInfo (작성자 이름 일괄 조회)
  → WorkspaceUserInfoAdapter (users 도메인)
```

### 1. 페이지 파라미터 검증

`page`(`@Min(0)`)·`size`(`@Min(1) @Max(100)`)를 컨트롤러에서 Bean Validation으로 먼저 검증한다. 검증에 실패하면 `ConstraintViolationException` → `GlobalExceptionHandler`가 `400 COMMON_400_1`로 응답한다(검증 없이 `PageRequest.of()`에 그대로 넘기면 `IllegalArgumentException`이 처리되지 않아 `500`으로 새어나간다).

### 2. 워크스페이스 존재 확인과 참여자 검증

다른 Task/Comment API와 동일한 순서 — 존재 확인(`404_1`) → 참여자 확인(`403_1`).

### 3. 업무 조회 (락 없음)

`TaskRepository.findById(workspaceId, taskId)`로 업무 소속을 확인한다. 다른 워크스페이스 소속이거나 없는 taskId는 `TaskNotFoundException`(`404_3`).

### 4. 댓글 페이지 조회

`TaskCommentRepository.findAllByTaskId(taskId, page, size)`가 `createdAt asc, id asc`(tie-break) 정렬로 `PageResult<TaskComment>`를 반환한다. 멘션 목록은 이 조회에서 채우지 않는다(목록 응답에 불필요).

### 5. 작성자 이름 일괄 조회

페이지에 담긴 모든 댓글의 `authorId`를 `Set`으로 모아 `WorkspaceUserInfoPort.findUserInfo(...)`를 **한 번만** 호출한다(N+1 방지). `PageResult.map(...)`으로 `TaskComment` → `TaskCommentListItem`(이름 해석 완료)으로 변환한다.

### 6. 응답

성공하면 Controller가 `SliceResponse.from(...)`으로 `PageResult`를 `content`/`page`/`size`/`hasNext` 4필드 응답으로 조립하고, `GlobalApiResponse.ok(WorkspaceResponseCode.TASK_COMMENT_LIST_RETRIEVED, ...)`로 `200 OK`를 반환한다.

## ✏️ 댓글 수정 API 흐름

```text
PATCH /api/workspaces/{workspaceId}/tasks/{taskId}/comments/{commentId}
  → Security Filter
  → AuthUser
  → WorkspaceTaskCommentController
  → UpdateTaskCommentRequest
  → UpdateTaskCommentCommand
  → UpdateTaskCommentUseCase
  → UpdateTaskCommentService
  → WorkspaceRepository.findById (락 없음)
  → WorkspacePersistenceAdapter
  → TaskRepository.findByIdForUpdate (락 없는 소속 확인 → 비관적 락, 2단계)
  → TaskPersistenceAdapter
  → TaskCommentRepository.findById (락 없음)
  → TaskCommentPersistenceAdapter
  → TaskComment.updateContent (Domain Model)
  → TaskCommentRepository.save
  → TaskCommentPersistenceAdapter
  → ApplicationEventPublisher.publishEvent (신규 멘션이 있을 때만)
  → 트랜잭션 커밋
  → WorkspaceWebSocketNotifier (AFTER_COMMIT)
  → WebSocketEventPublisher
  → /topic/workspaces/users/{recipientUserId}
```

### 1~3. 요청 검증, 워크스페이스·업무 확인

댓글 생성과 동일하다 — `@NotBlank` 검증, 존재·참여자 확인(`404_1`/`403_1`), 업무 조회(`404_3`, 2단계 워크스페이스 범위 락).

### 4. 댓글 조회와 소속 검증

`TaskCommentRepository.findById(commentId)`로 댓글을 조회한다(락 없음 — 댓글 자체는 별도 잠금 대상이 아니고, 부모 업무의 락이 이미 동시 수정을 막아준다). 없으면 `TaskCommentNotFoundException`(`404_4`). 있어도 `comment.belongsTo(taskId)`가 `false`면(다른 업무 소속 댓글) 존재를 노출하지 않기 위해 마찬가지로 `404_4`를 반환한다.

### 5. 멘션 대상 검증과 전체 교체

댓글 생성과 동일하게 멘션 대상을 검증한 뒤(`400_6`), 수정 전 멘션 ID 집합을 보존하고 `comment.updateContent(content, mentionedUserIds, now)`로 content와 멘션을 함께 교체한 새 `TaskComment`를 만든다(불변 도메인 모델). `TaskCommentRepository.save`는 기존 멘션을 전부 삭제하고 새 목록으로 재삽입한다. 영속성은 전체 교체를 유지하고, 알림 수신자 계산에만 수정 전후 멘션 차집합을 사용한다.

### 6. 새로 추가된 멘션 알림

알림 수신자는 `수정 요청 멘션 - 수정 전 멘션 - 요청자 자신`으로 계산하고 입력 순서대로 중복을 제거한다. 유지된 멘션은 재알림하지 않고, 제거된 멘션과 요청자 자신에게도 알리지 않는다. 신규 외부 멘션이 있을 때만 `TaskCommentMentionedEvent`를 발행하며, 생성 흐름과 동일하게 커밋 이후 사용자별 topic으로 전송한다.

### 7. 응답

작성자 본인 여부는 확인하지 않는다 — 현재 워크스페이스 참여자면 누구나 수정할 수 있다. 성공하면 `TASK_COMMENT_UPDATED`(`200_7`)와 갱신된 `TaskCommentResponse`를 반환한다.

## 🗑️ 댓글 삭제 API 흐름

```text
DELETE /api/workspaces/{workspaceId}/tasks/{taskId}/comments/{commentId}
  → Security Filter
  → AuthUser
  → WorkspaceTaskCommentController
  → DeleteTaskCommentCommand
  → DeleteTaskCommentUseCase
  → DeleteTaskCommentService
  → WorkspaceRepository.findById (락 없음)
  → WorkspacePersistenceAdapter
  → TaskRepository.findByIdForUpdate (락 없는 소속 확인 → 비관적 락, 2단계)
  → TaskPersistenceAdapter
  → TaskCommentRepository.findById (락 없음)
  → TaskCommentPersistenceAdapter
  → TaskCommentRepository.deleteById
  → TaskCommentPersistenceAdapter → TaskCommentJpaRepository / TaskCommentMentionJpaRepository
```

### 1~4. 검증 순서

댓글 수정과 동일한 순서 — 존재·참여자 확인, 업무 조회(2단계 워크스페이스 범위 락), 댓글 조회와 소속 검증(`404_4`).

### 5. 삭제

`TaskCommentRepository.deleteById(commentId)`가 멘션을 먼저 지우고(`TaskCommentMentionJpaRepository.deleteAllByCommentId`) 댓글을 지운다. 하드 삭제이며 복구할 수 없다. 작성자 본인 여부는 확인하지 않는다.

### 6. 응답

성공하면 Controller가 `GlobalApiResponse.ok(WorkspaceResponseCode.TASK_COMMENT_DELETED)`로 `200 OK`를 반환한다.

## ✅ 완료 토글 API 흐름

```text
PATCH /api/workspaces/{workspaceId}/tasks/{taskId}/comments/{commentId}/complete
  → Security Filter
  → AuthUser
  → WorkspaceTaskCommentController
  → ToggleTaskCommentCompleteCommand
  → ToggleTaskCommentCompleteUseCase
  → ToggleTaskCommentCompleteService
  → WorkspaceRepository.findById (락 없음)
  → WorkspacePersistenceAdapter
  → TaskRepository.findByIdForUpdate (락 없는 소속 확인 → 비관적 락, 2단계)
  → TaskPersistenceAdapter
  → TaskCommentRepository.findById (락 없음)
  → TaskCommentPersistenceAdapter
  → TaskComment.toggleComplete (Domain Model)
  → TaskCommentRepository.updateCompletion
  → TaskCommentPersistenceAdapter
```

### 1~4. 검증 순서

댓글 수정과 동일 — 요청 본문 없음, 존재·참여자 확인, 업무 조회, 댓글 조회와 소속 검증.

### 5. 완료↔취소 반전

`comment.toggleComplete(requesterId, now)`가 호출마다 완료 상태를 반전한다. 완료로 전환되면 `completedBy = requesterId`, `completedAt = now`. 취소로 전환되면 둘 다 `null`로 초기화된다. **호출마다 반전**하므로, 두 참여자가 거의 동시에 같은 댓글을 토글하면 나중 트랜잭션이 앞선 트랜잭션의 결과를 반전시킨다(예: A가 완료시킨 걸 B가 취소로 되돌림) — 이는 "워크스페이스가 실시간으로 공유되고 있음"을 드러내는 의도된 동작이며 버그가 아니다.

토글은 `TaskCommentRepository.save` 대신 전용 메서드 `updateCompletion`을 쓴다(2026-08-13, [#462](https://github.com/mudo-project/Team1-MUDO-BackEnd/issues/462)). `save`는 update 분기에서 멘션을 항상 전체 삭제 후 재삽입하는데, 토글은 멘션을 전혀 바꾸지 않아 삭제 전 동일 값을 재삽입하다 유니크 제약(`uk_task_comment_mention_comment_user`) 위반이 났다. `updateCompletion`은 `completed`/`completedBy`/`completedAt`만 갱신하고 멘션 테이블은 조회 외에 건드리지 않는다.

### 6. 응답

성공하면 `TASK_COMMENT_COMPLETE_TOGGLED`(`200_8`)와 갱신된 `TaskCommentResponse`를 반환한다.

## 📚 관련 문서

- [COMMENT_API.md](COMMENT_API.md) — 댓글·멘션 API 명세(요청/응답/에러코드)
- [TASK_API_FLOW.md](TASK_API_FLOW.md) — 업무(Task) 자체의 호출 흐름, 2단계 락 조회 메커니즘 상세
- [WORKSPACE_PERMISSIONS.md](WORKSPACE_PERMISSIONS.md) — 참여자 기반 권한 검증 정책과 `WORKSPACE:CREATE` TODO 현황
