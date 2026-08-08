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

### 6. 응답

성공하면 Controller가 `GlobalApiResponse.created(WorkspaceResponseCode.TASK_COMMENT_CREATED, ...)`로 HTTP `201 Created`와 `TaskCommentResponse`(댓글 전체 필드 + `mentionedUserIds`)를 반환한다.

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
```

### 1~3. 요청 검증, 워크스페이스·업무 확인

댓글 생성과 동일하다 — `@NotBlank` 검증, 존재·참여자 확인(`404_1`/`403_1`), 업무 조회(`404_3`, 2단계 워크스페이스 범위 락).

### 4. 댓글 조회와 소속 검증

`TaskCommentRepository.findById(commentId)`로 댓글을 조회한다(락 없음 — 댓글 자체는 별도 잠금 대상이 아니고, 부모 업무의 락이 이미 동시 수정을 막아준다). 없으면 `TaskCommentNotFoundException`(`404_4`). 있어도 `comment.belongsTo(taskId)`가 `false`면(다른 업무 소속 댓글) 존재를 노출하지 않기 위해 마찬가지로 `404_4`를 반환한다.

### 5. 멘션 대상 검증과 전체 교체

댓글 생성과 동일하게 멘션 대상을 검증한 뒤(`400_6`), `comment.updateContent(content, mentionedUserIds, now)`로 content와 멘션을 함께 교체한 새 `TaskComment`를 만든다(불변 도메인 모델). `TaskCommentRepository.save`는 기존 멘션을 전부 삭제하고 새 목록으로 재삽입한다(diff 없음 — 멘션 단독 CRUD가 없어 최적화하지 않음).

### 6. 응답

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

성공하면 `204 No Content`, 본문 없음.

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
  → TaskCommentRepository.save
  → TaskCommentPersistenceAdapter
```

### 1~4. 검증 순서

댓글 수정과 동일 — 요청 본문 없음, 존재·참여자 확인, 업무 조회, 댓글 조회와 소속 검증.

### 5. 완료↔취소 반전

`comment.toggleComplete(requesterId, now)`가 호출마다 완료 상태를 반전한다. 완료로 전환되면 `completedBy = requesterId`, `completedAt = now`. 취소로 전환되면 둘 다 `null`로 초기화된다. **호출마다 반전**하므로, 두 참여자가 거의 동시에 같은 댓글을 토글하면 나중 트랜잭션이 앞선 트랜잭션의 결과를 반전시킨다(예: A가 완료시킨 걸 B가 취소로 되돌림) — 이는 "워크스페이스가 실시간으로 공유되고 있음"을 드러내는 의도된 동작이며 버그가 아니다.

### 6. 응답

성공하면 `TASK_COMMENT_COMPLETE_TOGGLED`(`200_8`)와 갱신된 `TaskCommentResponse`를 반환한다.

## 📚 관련 문서

- [COMMENT_API.md](COMMENT_API.md) — 댓글·멘션 API 명세(요청/응답/에러코드)
- [TASK_API_FLOW.md](TASK_API_FLOW.md) — 업무(Task) 자체의 호출 흐름, 2단계 락 조회 메커니즘 상세
- [WORKSPACE_PERMISSIONS.md](WORKSPACE_PERMISSIONS.md) — 참여자 기반 권한 검증 정책과 `WORKSPACE:CREATE` TODO 현황
