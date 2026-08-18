# 🔄 워크스페이스 생성 이름 중복 정책 단순화

## ✅ 2026-08-18 · WebSocket 토픽 구독 시 LazyInitializationException 수정

### 변경 목적

바로 아래 "업무·댓글 실시간 브로드캐스트(WebSocket) 추가" PR(#600) 머지 후 프론트 실연동 중, `/topic/workspaces/{workspaceId}` 토픽을 구독(SUBSCRIBE)하면 매번 STOMP `ERROR` 프레임(`Failed to send message to ExecutorSubscribableChannel[clientInboundChannel]`) + 연결종료가 재현됐다. 이슈 [#606](https://github.com/mudo-project/Team1-MUDO-BackEnd/issues/606), PR [#607](https://github.com/mudo-project/Team1-MUDO-BackEnd/pull/607).

### 원인

`WorkspacePersistenceAdapter.findById()`에 `@Transactional`이 없었다. 기존 호출자는 전부 `@Transactional` Service 안에서만 이 메서드를 불러서 문제가 안 드러났는데, `JwtChannelInterceptor.authorizeWorkspaceTopicSubscription()`(트랜잭션 없는 최초의 호출자)가 부르면서 노출됐다. `findById()`가 `workspace` 행을 조회하는 순간 트랜잭션이 끝나고, 그 다음 `.map(mapper::toDomain)`이 `WorkspaceJpaEntity.members`(lazy `@OneToMany`)를 읽으려다 세션이 이미 닫혀 `LazyInitializationException`(Spring이 `JpaSystemException`으로 감쌈)이 발생했다.

로컬에서 Node.js `ws`로 실제 STOMP 클라이언트를 만들어 재현해 확보한 스택트레이스:
```
org.springframework.orm.jpa.JpaSystemException: failed to lazily initialize a collection of role:
WorkspaceJpaEntity.members: could not initialize proxy - no Session
	at ...WorkspacePersistenceAdapter$$SpringCGLIB$$0.findById(<generated>)
	at ...JwtChannelInterceptor.authorizeWorkspaceTopicSubscription(...)
```

### 구현 변경

- `WorkspacePersistenceAdapter.findById()`에 `@Transactional(readOnly = true)` 추가 — 이 메서드 자체가 트랜잭션 경계를 가지면 트랜잭션 있는/없는 호출자 모두 안전하게 `memberIds`까지 매핑된다.

### 수용한 한계

- `findByIdForUpdate()`도 동일 구조(트랜잭션 없이 lazy 컬렉션 매핑)라 잠재적으로 같은 위험이 있다. 다만 현재 모든 호출자가 `@Transactional` Service 안에서만 부르고 있어 이번 범위에서는 건드리지 않았다 — 트랜잭션 없는 새 호출자가 생기면 그때 같이 고칠 것.

### 검증

- 로컬에서 재현 스크립트로 수정 전(100% ERROR+연결종료)/후(정상 구독, `workspace_member` 조회 쿼리까지 실행) 비교 확인.
- `JwtChannelInterceptorTest`(12개) 및 workspace 도메인 전체 테스트 통과, 회귀 없음.

## ✅ 2026-08-18 · 업무·댓글 실시간 브로드캐스트(WebSocket) 추가

### 변경 목적

워크스페이스는 댓글 멘션 알림(2026-08-12) 하나만 실시간으로 push되고, 업무 생성/상태변경/삭제·댓글 생성/수정/완료토글/삭제는 새로고침해야 반영됐다. 프론트가 폴링 방식을 검토했으나 상시 서버 부하 우려로 채택하지 못해 실시간 반영 자체가 안 되는 상태였다. 이벤트 기반 WebSocket 브로드캐스트로 폴링 없이(=상시 부하 증가 없이) 실시간성을 확보한다. 이슈 [#601](https://github.com/mudo-project/Team1-MUDO-BackEnd/issues/601), PR [#600](https://github.com/mudo-project/Team1-MUDO-BackEnd/pull/600). 설계는 [2026-08-18-workspace-realtime-broadcast-design.md](../../../../../../../../docs/superpowers/specs/2026-08-18-workspace-realtime-broadcast-design.md), 계획은 [2026-08-18-workspace-realtime-broadcast.md](../../../../../../../../docs/superpowers/plans/2026-08-18-workspace-realtime-broadcast.md) 참고.

### 구현 변경

- 업무 생성/상태·마감일변경/삭제, 댓글 생성/수정/완료토글/삭제 총 7개 액션을 도메인 이벤트(`workspace/domain/event/`) 7종으로 발행한다 — `TaskCreatedEvent`/`TaskUpdatedEvent`/`TaskDeletedEvent`, `CommentCreatedEvent`/`CommentUpdatedEvent`/`CommentToggledEvent`/`CommentDeletedEvent`.
- 신규 `WorkspaceRealtimeNotifier`(`workspace/infrastructure/websocket/`)가 `@TransactionalEventListener(AFTER_COMMIT)`로 위 7개 이벤트를 받아 `/topic/workspaces/{workspaceId}` 단일 토픽으로 브로드캐스트한다. 액션별로 토픽을 나누지 않는다. 기존 멘션 전용 `WorkspaceWebSocketNotifier`는 완전히 무변경, 독립적으로 공존한다.
- 페이로드는 변경된 리소스 전체 데이터를 포함한다(id만 보내고 프론트가 재조회하는 방식이 아님) — 프론트가 REST 응답과 동일한 필드로 React Query 캐시를 바로 갱신할 수 있게 하기 위함. 다만 `createdBy`/`authorId`/`completedBy`는 userId만 싣고 이름 등 부가정보는 담지 않는다(YAGNI — 프론트가 이미 가진 참여자 목록에서 매핑 가능).
- 변경을 실행한 본인에게도 동일하게 브로드캐스트한다(제외 로직 없음) — REST 응답으로 이미 최신값을 받은 뒤 같은 값을 한 번 더 덮어쓸 뿐이라 무해하며, 세션 추적 로직이 필요 없어진다.
- `JwtChannelInterceptor`(global)에 `/topic/workspaces/(\d+)$` 패턴 구독 인가를 추가했다 — 워크스페이스 참여자만 구독 가능하도록 `WorkspaceRepository.findById`로 확인한다. 이 인터셉터가 처음으로 순수 토큰 검증에서 DB 조회까지 책임이 넓어졌다(구독은 화면 진입당 1회뿐이라 성능 영향 없음). global 패키지 파일이라 워크스페이스 도메인 소유 범위 밖이지만, 이번 변경은 사용자와 명시적으로 합의된 예외다.
- 이벤트 전송 실패는 전부 `try/catch(RuntimeException)`으로 감싸 로그만 남긴다(DB 트랜잭션에 영향 없음). 놓친 이벤트에 대한 재전송 큐는 만들지 않고, 프론트 재연결 시 전체 재조회로 동기화하는 것으로 대체한다(YAGNI).
- `SimpleBroker`(인메모리)로 충분하다 — 학원별 EC2 인스턴스·DB 스키마가 물리적으로 분리된 구조라 인스턴스 간 pub/sub 동기화가 필요 없다.

### 범위 밖

- 워크스페이스 이름변경, 참여자 추가/제거, 반복 업무 템플릿 CRUD의 실시간화 — 빈도가 낮아 후속 라운드로 미룸.
- 프론트엔드 연동(React Query `setQueryData`/`invalidateQueries`)과 실제 브라우저 E2E 확인 — 이 저장소는 백엔드 전용, 별도 진행.
- 폴링 vs 이벤트 push 정량 비교 k6 테스트 — 별도 스파이크로 분리.
- STOMP heartbeat 미설정(좀비 소켓/FD 누수 위험) — 이번 브레인스토밍 중 발견했으나 메신저·알림을 포함한 WS 인프라 전체(`WebSocketConfig`, global 소유)에 걸친 이슈라 이번 범위에서 제외, 별도 이슈로 분리.

### 검증

- `WorkspaceRealtimeNotifierTest`(신규) — 7개 이벤트 각각의 토픽·페이로드 발행, 전송 실패 시 예외 미전파를 검증.
- `JwtChannelInterceptorTest` — 워크스페이스 토픽 참여자 허용/비참여자 거부/존재하지 않는 워크스페이스/숫자 오버플로/미인증 5케이스 추가(기존 7개 + 신규 5개).
- 업무·댓글 Service 7개 테스트에 이벤트 발행 검증(`ArgumentCaptor`) 추가. CodeRabbit 리뷰로 완료토글의 완료해제(`completed=false`) 경로, 업무의 마감일단독변경 경로에도 이벤트 검증을 보강했다.
- 전체 `./gradlew test` 통과(회귀 없음).

## ✅ 2026-08-12 · 업무 댓글 멘션 WebSocket 알림 연동

- 댓글 생성 시 요청자를 제외한 멘션 사용자에게 알림 이벤트를 발행한다.
- 댓글 수정 시 새로 추가된 멘션 사용자에게만 알림 이벤트를 발행한다.
- 트랜잭션 커밋 이후 `/topic/workspaces/users/{recipientUserId}`로 사용자별 payload를 전송한다.
- 한 수신자의 WebSocket 발행 실패는 로그로 격리하고 나머지 수신자 발행을 계속한다.
- Web Push 저장·전송과 영속 알림 읽음 상태는 이번 범위에 포함하지 않는다.
- 사용자 topic의 SUBSCRIBE 인가는 global WebSocket 보안 과제로 분리한다. → 후속 작업으로 착수. 설계는 [2026-08-12-mention-subscribe-authorization-design.md](../../../../../../../../docs/superpowers/specs/2026-08-12-mention-subscribe-authorization-design.md) 참고, `global/infrastructure/security/websocket/JwtChannelInterceptor.java`에서 처리(workspace 도메인 변경 없음).
## ✅ 2026-08-12 · 워크스페이스 상세 조회의 누적 미완료 업무 정책 유지

### 현재 동작

- 상세 조회 날짜의 일별 데이터와 함께, 완료되지 않은 일반 업무는 마감일이 지난 경우에도 계속 노출한다.
- 사용자는 지연 업무를 상세 화면에서 완료하거나 상태를 변경할 때까지 추적할 수 있다.

### 확인한 위험

- 장기간 미완료 업무를 누적한 워크스페이스는 상세 조회 결과와 후속 집계 비용이 계속 증가할 수 있다.
- 업무·댓글·담당자 등 연관 조회가 결합되므로 데이터 누적 시 응답 시간과 DB 부하가 커질 수 있다.

### 검토한 해결안

- 상세 조회의 일반 업무를 선택 날짜의 `dueAt`에 해당하는 업무로 제한한다.
- 지연 업무는 기존 `GET /api/tasks/me?status=DELAYED`에서 별도로 모아 본다.

### 결정

- 프론트엔드에서 지연 업무의 상태 변경이 가능하고 실제 누적량이 크지 않을 것으로 예상되어 현재 조회 정책을 유지한다.
- 연동 테스트에서 사용자 흐름과 데이터 누적량을 확인하고, 부하 테스트에서 상세 조회의 쿼리 수·응답 시간·조회 행 수를 측정한 뒤 정책 변경 여부를 다시 검토한다.

## ✅ 2026-08-10 · 업무 상세 조회 & 댓글 목록 조회 API 추가

### 변경 목적

업무 목록 조회는 워크스페이스 상세 조회에 이미 카드 형태로 포함되어 있었지만, 업무 하나를 클릭해 들어가는 상세 화면과 그 화면의 댓글 영역을 채울 API가 없었다. Figma 목업 기준으로 필요한 항목을 확인해 두 개의 독립된 조회(Query) API를 추가했다(이슈 #277, #278 / PR #279).

브레인스토밍 과정에서 두 가지를 프론트와 합의했다: ① 최종 상태 변경자(누가 바꿨는지)는 기록은 유지하되 응답에는 노출하지 않는다. ② 코멘트 목록은 무한스크롤을 전제로 업무 상세와 별도 API로 분리한다.

### 구현 변경

- `GET /api/workspaces/{workspaceId}/tasks/{taskId}` — 업무 상세 조회. 제목·등록자·등록일·상태·기한·최종 상태 변경일시를 반환한다. `TaskDetailQueryUseCase`/`Service` 신규 작성.
- `GET /api/workspaces/{workspaceId}/tasks/{taskId}/comments` — 댓글 목록 조회. 내용·작성자·완료 여부·생성일을 반환하며 `createdAt asc, id asc`로 페이지네이션(기본 20개)한다. `TaskCommentListQueryUseCase`/`Service` 신규 작성.
- `Task` 도메인 모델에 `createdAt`을 추가했다. 기존 8-arg `restore(...)`를 11개 파일이 호출하고 있어 시그니처를 바꾸지 않고, `createdAt`을 받는 9-arg `restore(...)` 오버로드만 추가했다(기존 8-arg는 내부적으로 9-arg에 `createdAt=null`로 위임).
- `TaskRepository.findById(workspaceId, taskId)`(락 없음)을 신규 추가했다. 기존 유일한 단건 조회인 `findByIdForUpdate`는 비관적 락을 잡아서, 조회 전용 API에 그대로 쓰면 불필요한 락 경합을 유발한다.
- `TaskStatusHistoryRepository.findLatestChangedAt(taskId)`을 신규 추가했다. 전체 이력이 아니라 가장 최근 1건의 `createdAt`만 `Pageable(0, 1)`로 가져온다(MySQL 5.7 호환을 위해 `LIMIT` 서브쿼리 대신 페이지네이션 사용). **변경자(`changedBy`)는 조회하지 않는다** — 이력 자체는 계속 저장되므로 나중에 노출이 필요해지면 조회 메서드만 추가하면 된다.
- `TaskCommentRepository.findAllByTaskId(taskId, page, size)`를 신규 추가했다. `createdAt`만으로는 동시간대 저장 시 정렬이 비결정적일 수 있어 `id`를 2차 정렬 기준으로 추가했다(반복 업무 템플릿 목록과 동일한 tie-break 이유).
- `WorkspaceResponseCode`에 `WORKSPACE_200_16`(업무 상세 조회), `WORKSPACE_200_17`(댓글 목록 조회)을 추가했다.

### 코드 리뷰 반영 (CodeRabbit)

whole-PR 리뷰에서 6건이 나왔고, 4건 반영 + 2건 반박했다.

- **반영**: `WorkspaceTaskCommentController.getComments`의 `page`/`size`에 `@Validated` + `@Min`/`@Max` 검증을 추가했다. `GlobalExceptionHandler`에 `IllegalArgumentException` 전용 핸들러가 없어서, 검증 없이 두면 `page=-1`/`size=0` 요청이 `PageRequest.of()`의 `IllegalArgumentException` → `500`으로 새어나가는 걸 확인했다(형제 API인 반복 업무 템플릿 목록은 이미 이 패턴을 쓰고 있었다).
- **반영**: 댓글 정렬 테스트(`findAllByTaskIdReturnsOldestFirstWithinPageSize`)가 실제로는 `createdAt` 정렬을 검증하지 못하고 있었다 — `TaskCommentJpaEntity.create()`가 `createdAt`을 받지 않고 `@CreatedDate`가 실제 저장 시각을 찍기 때문에, save 호출 순서(→ id tie-break)로 우연히 통과하던 테스트였다. insertion 순서와 의도한 시간 순서를 반대로 만들고 `jdbcTemplate`으로 `created_at`을 직접 덮어쓴 뒤 `entityManager.clear()`로 1차 캐시를 비워, 실제 컬럼 기준 정렬을 검증하도록 수정했다.
- **반영**: 두 컨트롤러 테스트에 누락된 `createdAt` 응답 필드 assertion을 추가했다.
- **반박**: `Task.restore()`에 반복 업무 불변식(`recurringTemplateId != null`인데 `scheduledFor == null`인 조합 차단) 검증 추가 제안. 이 불변식은 원래 8-arg `restore()`에도 없던 검증이고, 현재 코드베이스에 반복 업무 템플릿으로부터 실제 occurrence를 생성하는 서비스가 아직 없어 이 조합이 만들어질 프로덕션 경로가 없다. `restore()`에서 막으면 나중에 occurrence 생성 로직이 실수로 이 조합을 만들었을 때 조회 시점에 늦게 500으로 터진다 — 생성 시점에 검증하는 별도 이슈로 미뤘다.
- **반박**: 댓글 목록의 offset(page/size) → keyset(cursor) 페이지네이션 전환 제안. 브레인스토밍 때 이미 결정한 방향이고 형제 API(반복 업무 템플릿 목록)도 동일 패턴이다. 업무당 댓글 수가 소셜 피드처럼 커질 상황이 아니라 YAGNI로 보류했다.

### 검증

- `TaskTest`, `TaskPersistenceAdapterDataJpaTest`, `TaskDetailQueryServiceTest`, `WorkspaceTaskControllerTest`(신규 4케이스: 정상/이력없음/404/403)로 업무 상세 조회를 검증했다.
- `TaskCommentPersistenceAdapterDataJpaTest`, `TaskCommentListQueryServiceTest`, `WorkspaceTaskCommentControllerTest`(신규 케이스: 정상 페이지/커스텀 page·size/404/403 + page/size 검증 3케이스)로 댓글 목록 조회를 검증했다.
- 전체 `./gradlew build` 통과, 기존 8-arg `Task.restore(...)` 호출부(11개 파일) 회귀 없음 확인.

> API 계약은 [TASK_API.md](TASK_API.md)/[COMMENT_API.md](COMMENT_API.md), 호출 흐름은 [TASK_API_FLOW.md](TASK_API_FLOW.md)/[COMMENT_API_FLOW.md](COMMENT_API_FLOW.md)에 반영했다. 📚

## ✅ 2026-08-09 · 반복 업무 템플릿 삭제 API와 삭제 응답 형식 표준화

### 변경 목적

반복 업무 템플릿 생성(#224)·목록 조회(#234)·수정(#236)에 이어 삭제 API를 추가합니다. 수정 API 라운드에서 `REVISION.md`에 남긴 계획대로, 템플릿 조회를 비관적 락(`findByWorkspaceIdAndIdForUpdate`)으로 전환해 수정·삭제 두 Service가 공유하도록 했습니다. 이 작업 중 workspace 도메인의 기존 삭제 API 4개가 전부 `204 No Content`로 응답해 성공 메시지를 확인할 수 없다는 점도 함께 발견해, 삭제 API 5개(신규 1개 + 기존 4개)를 전부 `200 OK` + `GlobalApiResponse` 봉투로 통일했습니다.

### 구현 변경

- `DELETE /api/workspaces/{workspaceId}/recurring-templates/{templateId}`를 추가했습니다. 하드 삭제이며 `recurring_task_skip`도 함께 삭제됩니다.
- `RecurringTaskTemplateRepository.findByWorkspaceIdAndIdForUpdate`를 추가했습니다 — `Task.findByIdForUpdate`와 동일한 2단계 패턴(락 없는 소속 확인 → 비관적 락)이며, `UpdateRecurringTaskTemplateService`도 기존 락 없는 조회에서 이 메서드로 전환해 수정·삭제 두 Service가 같은 락을 공유합니다.
- 템플릿 삭제 시 이미 생성된 Task는 삭제하지 않고 `recurring_template_id`만 `NULL`로 남깁니다(운영 마이그레이션의 `ON DELETE SET NULL` 그대로). `TaskJpaEntity.recurringTemplate`에 `@OnDelete(action = OnDeleteAction.SET_NULL)`을 추가해 이 동작을 `@DataJpaTest`(H2)에도 재현했습니다 — 이 어노테이션이 없으면 아직 생성된 업무가 남아있는 템플릿을 삭제할 때 H2에서 FK 제약 위반이 발생합니다.
- 워크스페이스 삭제, 참여자 제거, 업무 삭제, 업무 댓글 삭제, 반복 업무 템플릿 삭제 API 5개를 전부 `ResponseEntity<GlobalApiResponse<Void>>` + `200 OK`로 통일했습니다. `WorkspaceResponseCode`에 `WORKSPACE_200_11`~`WORKSPACE_200_15` 5개를 추가했습니다. Service/UseCase의 `delete`/`removeMember`/`deleteComment` 시그니처는 바꾸지 않고 Controller 레이어에서만 봉투로 감쌌습니다.

### 검증

- `DeleteRecurringTaskTemplateServiceTest`로 성공/워크스페이스 없음/미참여자/템플릿 없음을 검증했습니다.
- `RecurringTaskTemplatePersistenceAdapterDataJpaTest`에 `findByWorkspaceIdAndIdForUpdate`의 워크스페이스 범위 검증과, 템플릿 삭제 시 생성된 Task의 `recurring_template_id`가 `NULL`로 바뀌는지 검증하는 테스트를 추가했습니다.
- `WorkspaceRecurringTaskTemplateControllerTest`로 200/403/404를 검증했습니다.
- 기존 `WorkspaceControllerTest`·`WorkspaceTaskControllerTest`·`WorkspaceTaskCommentControllerTest`의 삭제 관련 테스트를 200 + 응답 바디 검증으로 갱신했습니다.
- 전체 `./gradlew test`를 통과했습니다.

> API 계약은 [RECURRING_TASK_API.md](RECURRING_TASK_API.md), 호출 흐름은 [RECURRING_TASK_API_FLOW.md](RECURRING_TASK_API_FLOW.md)에 반영했습니다. 📚

## ✅ 2026-08-09 · 반복 업무 템플릿 수정 API와 로깅 커밋 타이밍 개선

### 변경 목적

반복 업무 템플릿 생성(#224)·목록 조회(#234)에 이어 수정 API(#236)를 추가합니다. 이번 라운드는 이 프로젝트에서 처음으로 superpowers 브레인스토밍→스펙→계획→subagent-driven-development→최종 whole-branch 리뷰 사이클을 끝까지 완주한 사례이기도 합니다. 최종 리뷰와 코드래빗(CodeRabbit) 리뷰에서 나온 지적을 계기로, 이번 수정 API뿐 아니라 workspace 도메인 전체의 로깅 컨벤션 적용 범위와 로그 타이밍도 함께 개선했습니다.

### 구현 변경

- `PATCH /api/workspaces/{workspaceId}/recurring-templates/{templateId}`를 추가했습니다. `title` 단독 또는 `recurrenceType`+`recurrenceRule` 세트 중 최소 하나가 필요하며, 누락된 쪽은 기존 값을 유지합니다. `recurrenceType`·`recurrenceRule`은 항상 세트로만 받습니다 — 타입이 바뀌면 규칙의 유효한 모양도 바뀌므로(`WEEKLY`의 `daysOfWeek` vs `MONTHLY`의 `dayOfMonth`), 부분 수정을 허용하면 검증 실패나 예측 불가능한 동작으로 이어질 수 있습니다.
- `RecurringTaskTemplate.changeRecurrence`를 재호출해 병합된 값을 다시 검증합니다 — 값이 바뀌지 않은 필드도 여전히 유효한 규칙인지 재확인되는 의도된 부수 효과입니다.
- 삭제 API가 아직 없어 수정 조회(`findByWorkspaceIdAndId`)에는 비관적 락을 걸지 않았습니다. 삭제 API를 추가할 때 `findByWorkspaceIdAndIdForUpdate`로 전환하고 수정·삭제 두 Service가 그 조회를 공유하도록 반드시 바꿔야 합니다(`Task.findByIdForUpdate`와 동일 패턴).
- 최종 whole-branch 리뷰에서 공백 제목이 trim 후 빈 문자열로 저장 가능하다는 결함을 발견해 `@AssertTrue` 검증을 추가했습니다 — 생성 API는 `@NotBlank`로 이미 막고 있었는데 수정 API에는 빠져 있었습니다. 4개의 개별 태스크 리뷰는 각자 통과했지만 교차 태스크 이슈라 whole-branch 리뷰에서만 잡혔습니다.
- `AfterCommitLogger`(`global.infrastructure.logging`)를 추가했습니다. `TransactionSynchronizationManager.registerSynchronization`으로 완료(`_완료`) 로그를 트랜잭션 커밋 이후로 지연시킵니다. 저장 직후 로그를 남기면 이후 커밋 시점에 제약조건 위반·deadlock 등으로 롤백돼도 성공 로그만 남아 실패를 성공으로 오인할 수 있다는 코드래빗 지적을 반영했습니다. attendance/approval 도메인은 별도 PR이라 이번 범위에서 제외하고, workspace 도메인 완료 로그 12곳(이번에 추가한 8곳 + 기존 3곳 + `DelayOverdueTasksService`)에만 적용했습니다.
- `AddWorkspaceMembersService`가 추가할 신규 참여자가 없어 조기 반환하는 경로에서도 `addedCount=0`으로 완료 로그를 남기도록 수정했습니다. 이전에는 조기 반환 시 완료 로그 자체가 생략되어 no-op 요청과 실패를 로그만으로 구분할 수 없었습니다.
- workspace 도메인 Service 20개 중 로깅 컨벤션이 적용되지 않았던 17개(comment 4, task 4, workspace 9)에 시작/완료 로그를 소급 적용했습니다. GitHub 이슈 #223("workspace 도메인 기존 Service에 로깅 컨벤션 소급 적용")이 CLOSED 상태였지만 실제 코드에는 반영되어 있지 않아 이번에 다시 처리했습니다(#262). 변경(mutation) 작업 12곳의 시작 로그에는 `requesterId`(또는 `creatorId`)를 추가해 "누가 했는지"를 추적할 수 있게 했고, `RemoveWorkspaceMemberService`에는 자진탈퇴·타인제거를 구분하는 `selfWithdrawal` 플래그도 추가했습니다.

### 수용한 한계

- 자유텍스트(업무 제목, 워크스페이스 이름)를 로그에 남기는 문제와, `DelayOverdueTasksService`만 시작 로그 없이 완료 로그만 있는 비대칭은 컨벤션 문서 차원의 논의가 필요해 이번 범위에서 제외하고 후속 이슈로 남겼습니다.

### 검증

- Application 계층 테스트(제목만/주기만 변경, 워크스페이스 없음, 비참여자, 템플릿 없음, 주기 불일치)와 Controller 테스트(정상/빈 바디/세트 불완전/공백 제목/템플릿 없음/403/`400_7`)를 추가했습니다.
- 로깅 리트로핏은 새 테스트를 추가하지 않고 각 서비스의 기존 테스트가 그대로 통과하는지만 확인했습니다(로그 출력을 검증하는 테스트 인프라가 없고, 기존에 로깅이 있던 서비스들도 동일).
- `AfterCommitLogger`는 트랜잭션 동기화가 없을 때 즉시 실행되는지, 트랜잭션이 있을 때 커밋 전에는 실행되지 않다가 `afterCommit` 호출 시 실행되는지 단위 테스트로 검증했습니다.
- 전체 `./gradlew test`를 통과했습니다.

> API 계약은 [RECURRING_TASK_API.md](RECURRING_TASK_API.md), 호출 흐름은 [RECURRING_TASK_API_FLOW.md](RECURRING_TASK_API_FLOW.md)에 반영했습니다. 📚

## ✅ 2026-08-07 · 업무 CRUD 추가와 Task 도메인 계층 도입

### 변경 목적

업무 생성·상태변경·삭제 API를 구현하면서 `Task`의 도메인 모델과 Repository 포트를 처음으로 만듭니다. 이전 라운드(업무 자동 지연 스케줄러)에서 `Task` 도메인 모델이 없어 의도적으로 미뤄둔 계층 위반 — `DelayOverdueTasksService`가 `TaskJpaRepository`·`TaskJpaEntity`에 직접 의존하던 문제 — 를 같은 라운드에서 함께 해소합니다. 도메인 모델을 두 번 설계하지 않기 위해 세 API를 하나의 스펙으로 묶었습니다.

### 구현 변경

- `Task` 불변 도메인 모델을 추가했습니다. 초기 상태 결정(마감일이 오늘 이전이면 `DELAYED`)과 상태 전이 규칙 두 개를 소유합니다. `Clock`을 주입받지 않고 `today`를 파라미터로 받아 `docs/ARCHITECTURE.md`의 "Domain은 Spring·JPA에 의존하지 않는다" 규칙을 지키고, 규칙 전체를 순수 단위 테스트로 검증할 수 있게 했습니다.
- 상태 전이를 4×4 전이표 하드코딩이 아니라 두 규칙으로 표현했습니다. ① `DELAYED`로의 전환은 미완료 상태에서만 가능(`COMPLETED → DELAYED`만 금지) ② 미완료 상태로 전환할 때 현재 마감일이 오늘 이전이면 오늘 이후의 새 마감일이 필요. 규칙 ②는 기존 "지연 업무를 진행 중으로 바꾸려면 미래의 새 마감일" 규칙을 일반화한 것으로, `DELAYED` 경유든 `COMPLETED` 경유든 마감일 정합성을 동일하게 지킵니다. 새 마감일 경계는 생성 규칙(오늘 마감은 `WAITING`)과 대칭이 되도록 오늘을 허용합니다.
- `TaskRepository`·`TaskStatusHistoryRepository`·`RecurringTaskSkipRepository` 포트 3개와 어댑터를 추가했습니다. `recurring_task_skip`은 `task`가 아니라 `recurring_task_template`에 FK를 가진 별개 Aggregate이므로 포트를 분리했습니다.
- `WorkspaceRepository`에 락 없는 `findById`를 추가했습니다. 업무 API 3개는 워크스페이스를 참여자 검증용으로 읽기만 하므로, 기존 `findByIdForUpdate`를 재사용하면 같은 워크스페이스에서 동시에 업무를 만들 때 불필요하게 직렬화됩니다.
- 업무 하드 삭제를 DB의 `ON DELETE CASCADE`에 의존하지 않고 어댑터가 자식 → 부모 순서로 명시적으로 수행합니다(멘션 → 댓글 → 상태 이력 → 업무). `@DataJpaTest`의 H2 스키마는 엔티티에서 생성되어 cascade가 없으므로, DB cascade에만 의존하면 삭제 동작을 MySQL Testcontainers 없이는 검증할 수 없습니다. DB cascade는 안전망으로 남습니다.
- `DelayOverdueTasksService`를 포트 기반으로 이관하고 TODO 주석을 제거했습니다. 상태만 직접 바꾸던 이전 엔티티 메서드는 참조처가 사라져 삭제하고, 도메인 모델이 결정한 값을 반영하는 `updateStatusAndDueAt(status, dueAt)`으로 대체했습니다.

### 수용한 한계

- 스케줄러 실행과 사용자의 상태 변경이 정확히 같은 시각에 겹치면 한쪽 변경이 덮이거나 이력이 어긋날 수 있습니다. 스케줄러는 벌크 조회 후 순회하는 구조라 대상 업무 전체를 잠그는 비용이 과해 락을 걸지 않았습니다. KST 00:00 트래픽이 사실상 없다는 전제로 수용하며, 다중 인스턴스 대응으로 분산 락을 도입할 때(GitHub 이슈 #138) 함께 해결합니다.
- 반복 업무 회차 삭제 시 남기는 `recurring_task_skip` 기록은 소비자(반복 업무 생성 스케줄러)가 아직 없어 당장 아무도 읽지 않습니다. 스케줄러 도입 전에 삭제된 회차가 되살아나는 것을 막기 위해 미리 기록합니다.

### 검증

- `TaskTest`에서 초기 상태 결정(어제/오늘/내일 마감일)과 상태 전이 전수, 규칙 ② 경계(현재 마감일 어제 × 새 마감일 없음/어제/오늘/내일), 반복 업무 면제를 `Clock` 없이 검증했습니다.
- 세 서비스 테스트에서 검증 순서(`404_1` → `403_1` → `404_3` → `400_x`)를 고정하고, 저장된 이력의 `previousStatus`·`currentStatus`·`changedBy`를 `ArgumentCaptor`로 직접 단언했습니다. 같은 상태 전이 시 이력을 저장하지 않는 것도 검증합니다.
- `TaskPersistenceAdapterDataJpaTest`에서 도메인 ↔ 엔티티 왕복(일반·반복 양쪽)과 삭제 시 댓글·멘션·이력 4종이 실제로 사라지는지, `saveIfAbsent`의 멱등성을 확인했습니다.
- 모든 테스트의 고정 `Clock`을 KST 날짜 경계를 실제로 넘는 시각(UTC 전날 15:00)으로 맞췄습니다.

> 상태 전이 규칙과 삭제 정책은 [BUSINESS_RULES.md](BUSINESS_RULES.md)에, API 계약은 [WORKSPACE_API.md](WORKSPACE_API.md)에, 스케줄러 흐름 변경은 [TASK_API_FLOW.md](TASK_API_FLOW.md)에 반영했습니다. 📚

## ✅ 2026-08-06 · 업무 자동 지연 스케줄러 최종 리뷰 반영

### 변경 목적

업무 자동 지연 처리 스케줄러(`WorkspaceTaskDelayScheduler`) 구현 완료 후 진행한 전체 브랜치 코드 리뷰에서 나온 지적사항 중, 정확성·성능·테스트 커버리지에 영향을 주는 항목을 반영합니다. Application 계층이 JPA Repository/Entity에 직접 의존하는 계층 위반은 이번 범위에서 제외했습니다 — `Task`는 아직 도메인 모델·Repository 인터페이스가 없어(다른 워크스페이스 하위 Aggregate와 달리 JPA Entity로만 존재), 지금 Port 하나만 임시로 만들면 향후 Task 도메인 모델을 정식으로 만들 때 다시 갈아엎어야 합니다. Task 도메인을 만드는 라운드(예: 업무 CRUD 구현)로 미룹니다.

### 구현 변경

- `TaskJpaRepository.findOverdueRecurringTasks`의 `function('DATE', t.scheduledFor) < :today` 술어를 `t.scheduledFor < :startOfToday`(`LocalDateTime`) 비교로 변경했습니다. 컬럼에 함수를 적용하지 않아 인덱스를 탈 수 있고, MySQL 전용 `DATE()` 함수 의존이 사라져 방언 이식성 문제도 함께 해소됩니다. `DelayOverdueTasksService`는 `today.atStartOfDay()`를 계산해 넘깁니다.
- `findOverdueRegularTasks`/`findOverdueRecurringTasks` 두 쿼리 모두에 `t.workspace.deletedAt is null` 조건을 추가했습니다. 소프트 삭제된 워크스페이스의 업무는 자동 지연 대상에서 제외합니다(정책 확정: 삭제 기간 중 지연 이력이 쌓인 채로 워크스페이스가 복구되는 시나리오를 방지).
- `DelayOverdueTasksService`에 `@Slf4j`를 추가하고, 지연 전환 완료 후 처리 건수(일반/반복)를 INFO 레벨로 한 줄 로그합니다. 이 프로젝트의 첫 `@Scheduled` 배치 잡이라 실패해도 아무도 모르는 상태를 막기 위한 최소 관측성입니다. 청크 처리·분산 락 등 운영 성숙도 항목은 실제 데이터량·인스턴스 수가 확인될 때로 미뤘습니다.
- `DelayOverdueTasksService`에 Task 도메인 모델 부재로 JPA Repository에 직접 의존한다는 사실과, Task 도메인 모델을 만들 때 Port/Adapter로 전환한다는 `TODO` 주석을 남겼습니다.
- `TaskJpaRepositoryOverdueQueryDataJpaTest`에서 DATE() 함수 제거에 따라 불필요해진 `@AutoConfigureTestDatabase(replace = Replace.NONE)`(H2를 MySQL 호환 모드로 유지하던 임시 조치)를 제거했습니다. 기본 `@DataJpaTest` 격리 H2로 돌아가, 다른 `@SpringBootTest`와 이름 있는 인메모리 DB를 공유하던 잠재적 테스트 오염 위험도 함께 해소됩니다.

### 검증

- `TaskJpaRepositoryOverdueQueryDataJpaTest`에 소프트 삭제된 워크스페이스의 업무가 일반/반복 쿼리 각각에서 제외되는지 검증하는 테스트를 추가했습니다.
- `DelayOverdueTasksServiceTest`의 고정 `Clock`을 KST 날짜 경계를 실제로 넘는 시각(UTC 전날 15:00)으로 바꾸고, `LocalDate.now(clock)`이 의도한 KST 날짜로 계산되는지 단언을 추가했습니다. 기존 값은 UTC와 KST 날짜가 우연히 같아 시간대 버그가 있어도 테스트를 통과시켰습니다.
- 같은 테스트에서 `ArgumentCaptor`로 저장된 이력 행을 캡처해 `previousStatus`(전환 전 상태)·`currentStatus`(`DELAYED`)·`changedBy`(`NULL`)를 직접 단언하도록 강화했습니다. 기존에는 `save()` 호출 횟수만 검증해 이력 내용 자체는 검증하지 못했습니다.
- 전체 `./gradlew test`를 통과했습니다(389/389).

> 자동 지연 조건에 워크스페이스 소프트 삭제 제외 규칙을 추가한 내용은 [BUSINESS_RULES.md](BUSINESS_RULES.md)에도 반영했습니다. 📚

## ✅ 2026-08-05 · 워크스페이스 상세 조회 코드 리뷰 반영

### 변경 목적

워크스페이스 상세 조회 API에 대한 코드 리뷰(CodeRabbit) 지적사항 중 재현 가능성·정보 일관성에 영향을 주는 항목을 반영합니다. 성능 최적화 방향은 맞지만 지금 데이터량에서 급하지 않은 항목(중첩 서브쿼리 재작성, projection 도입)과, 이미 의도적으로 분리한 설계(`findActiveWorkspaceName`의 존재 확인·접근 권한 확인 분리)는 이번 범위에서 제외했습니다.

### 구현 변경

- `WorkspaceDetailQueryService.TASK_ORDER`에 업무 번호(`taskId`)를 마지막 tie-break로 추가했습니다. 상태·기한·생성 시각이 모두 같은 업무가 있어도 항상 같은 순서를 반환합니다.
- 참여자·업무 생성자의 표시 이름을 조회하지 못하면 `null` 대신 `"알 수 없음"`으로 대체하도록 `resolveName`을 추가했습니다. 목록에서 제외하지 않으므로 `memberCount`/`taskCount`와 실제 배열 길이가 항상 일치합니다.
- `TaskJpaRepository.findVisibleRegularTasks`/`findVisibleRecurringTasks`의 날짜 비교를 `cast(... as date) = :date`에서 `>= startOfDay and < endOfDay` 범위 비교로 변경했습니다. 컬럼에 함수를 적용하지 않아 인덱스를 탈 수 있습니다. 두 메서드는 `LocalDate` 대신 `LocalDateTime` 시작·끝 두 개를 파라미터로 받도록 시그니처를 바꿨고, `LocalDate` → 시작/끝 시각 변환은 `WorkspaceDetailQueryAdapter`가 담당합니다.
- `WorkspaceJpaRepository.findMemberUserIds`에 `order by member.id.userId asc`를 추가해 참여자 목록 순서를 고정했습니다.

### 검증

- `WorkspaceDetailQueryServiceTest`에 tie-break 정렬, 이름 조회 실패 fallback 테스트를 추가했습니다.
- `TaskJpaRepositoryDataJpaTest`를 새 시그니처(`LocalDateTime` 시작/끝)에 맞게 갱신했고, 기존 표시 규칙 검증(당일 완료 업무만 노출, 재완료 시 최신 이력만 사용, 반복 업무 당일 회차만 노출)이 그대로 통과하는 것으로 회귀가 없음을 확인했습니다.
- `WorkspaceJpaRepositoryDetailQueryDataJpaTest`의 참여자 조회 검증을 `containsExactlyInAnyOrder`에서 `containsExactly`로 강화했습니다.
- 전체 `./gradlew test`를 통과했습니다.

> 외부 API 계약에 영향을 주는 변경(참여자 표시 이름의 `"알 수 없음"` fallback, 업무 카드 정렬의 `taskId` tie-break)은 [WORKSPACE_API.md](WORKSPACE_API.md), [WORKSPACE_API_FLOW.md](WORKSPACE_API_FLOW.md)에도 반영했습니다. 📚

## ✅ 2026-08-05 · 워크스페이스 목록·최근 접속 조회 추가

### 변경 목적

워크스페이스 페이지 진입 시 사용자가 참여 중인 공간을 바로 확인하고, 공용 공간에서도 각 사용자가 최근에 확인한 워크스페이스를 빠르게 다시 열 수 있도록 목록 조회와 사용자별 최근 접속 기록을 추가했습니다.

### 구현 변경

- `GET /api/workspaces?scope=MINE|ALL` 목록 조회 API를 추가했습니다.
- 기본 조회 범위는 요청 사용자가 참여한 같은 학원의 활성 워크스페이스(`MINE`)입니다.
- `WORKSPACE:READ_ALL` 권한이 있는 사용자는 같은 학원의 전체 활성 워크스페이스(`ALL`)를 조회할 수 있습니다.
- 목록 응답은 `workspaceId`, `name`, `memberCount`만 제공하며, 요청 사용자 기준 최근 접속 시각 내림차순으로 정렬합니다. 미접속 워크스페이스는 생성 시각 내림차순으로 뒤에 배치합니다.
- `PUT /api/workspaces/{workspaceId}/recent-access` 최근 접속 기록 API를 추가했습니다. 상세 화면을 정상적으로 연 뒤 호출하며, 요청 본문 없이 `204 No Content`를 반환합니다.
- `workspace_recent_access`는 `(user_id, workspace_id)` 복합 PK를 사용해 사용자별·워크스페이스별 한 행만 유지하고, 재접속 시 `last_accessed_at`을 갱신합니다.
- 최근 접속 기록은 MySQL 단일 upsert로 처리해 같은 최초 접속 요청이 동시에 들어와도 중복 키 오류 없이 생성 또는 갱신합니다.

### 검증

- 목록 조회의 `MINE`·`ALL` 범위, 같은 학원 제한, `WORKSPACE:READ_ALL` 권한 경계를 검증했습니다.
- 최근 접속 기록의 생성·갱신, 사용자별 정렬, 미접속 항목의 후순위 정렬을 검증했습니다.
- Testcontainers MySQL에서 동일한 `(user_id, workspace_id)`에 대한 동시 최초 upsert 요청이 한 행으로 저장되고 모두 성공하는지 검증했습니다.
- 더 최신 접속 시각을 저장한 뒤 더 과거의 요청이 도착해도 최신 시각이 유지되는지 검증했습니다.
- 전체 Gradle 테스트와 `git diff --check`를 통과했습니다.

## ✅ 2026-08-05 · 도메인 생성·복원 경로 분리

### 변경 목적

워크스페이스 신규 생성 규칙과 DB 복원 규칙을 같은 Builder 경로로 처리하면, DB 복원 시에도 생성자 자동 참여 규칙이 적용될 수 있습니다. 신규 생성과 복원의 책임을 Domain Model에 명확히 분리합니다.

### 구현 변경

- `Workspace.create(...)`를 추가해 신규 워크스페이스 생성만 생성자 자동 참여 규칙을 적용하도록 했습니다.
- `Workspace.restore(...)`를 추가해 DB에 저장된 ID·생성자·참여자 상태를 변경 없이 복원하도록 했습니다.
- `Workspace`의 Builder를 제거해 외부 생성 경로를 `create`와 `restore`로 제한했습니다.
- `WorkspaceService`는 Builder 대신 `Workspace.create(...)`를 호출하고, 기존 참여자 검증과 이름 중복 확인 흐름을 유지합니다.
- `WorkspacePersistenceMapper.toDomain(...)`은 default 메서드에서 `Workspace.restore(...)`를 호출하도록 변경했습니다.
- MapStruct 생성 구현체는 `toEntity(...)`만 생성하며, `toDomain(...)`은 Mapper의 default 복원 로직을 사용합니다.

### 규칙

- `create(...)`는 전달받은 추가 참여자를 방어적으로 복사하고 생성자를 추가합니다.
- 생성자가 추가 참여자 목록에 포함되어도 Set으로 한 번만 보관합니다.
- `restore(...)`는 생성자를 참여자 목록에 다시 추가하지 않습니다.
- 별도 Policy를 만들지 않고 `WorkspaceService`가 `WorkspaceMemberDirectoryPort`를 직접 호출합니다.

### 검증

- `Workspace.create()`의 생성자 자동 참여, 생성자 중복 제거, 방어적 복사를 검증했습니다.
- `Workspace.restore()`가 저장된 참여자 목록을 그대로 복원하는지 검증했습니다.
- `WorkspacePersistenceMapper.toDomain()`이 복원 과정에서 생성자를 추가하지 않는지 검증했습니다.
- `clean compileJava` 후 생성된 `WorkspacePersistenceMapperImpl`이 `toEntity()`만 구현하는 것을 확인했습니다.
- `WorkspaceTest`, `WorkspaceServiceTest`, `WorkspacePersistenceMapperTest`, `WorkspacePersistenceAdapterTest`, `WorkspacePersistenceAdapterDataJpaTest`, `CreateWorkspaceRequestTest`를 통과했습니다.

> 외부 API와 사용자 정책은 변경되지 않아 CHANGELOG에는 별도 항목을 추가하지 않았습니다.

## ✅ 2026-08-05 · 생성 시 자동 접미사 제거

### 변경 목적

초기 워크스페이스 생성에서 중복 이름을 자동으로 변경하면 사용자가 의도하지 않은 이름으로 생성될 수 있습니다. 생성과 이름 수정의 중복 정책을 동일하게 맞추고, 이름 결정·재시도 로직을 단순화합니다.

### 정책 변경

- 같은 학원의 활성 워크스페이스 이름은 고유합니다.
- 생성 또는 이름 수정 시 동일한 활성 이름이 존재하면 자동으로 `(1)`을 붙이지 않고 `409 Conflict`를 반환합니다.
- 이름 비교 전 앞뒤 공백을 제거합니다.
- 자동 번호 부여는 추후 워크스페이스 복사 기능에만 적용합니다.
- 삭제된 워크스페이스 복구 시의 자동 번호 부여 정책은 유지합니다.

### 구현 변경

- `WorkspaceCreationTransaction`을 제거했습니다.
- `WorkspaceService`가 단일 트랜잭션에서 참여자 검증, 활성 이름 확인, 워크스페이스 저장을 처리합니다.
- 활성 이름이 존재하면 저장 전에 `WorkspaceNameConflictException`을 발생시킵니다.
- 동시 생성으로 사전 확인 이후 DB unique 제약이 충돌해도 `WorkspacePersistenceAdapter`가 `WorkspaceNameConflictException`으로 변환합니다.
- `WORKSPACE_409_1`은 사전 중복 확인과 DB unique 제약 충돌에 동일하게 사용합니다.

### 영향 범위

| 구분 | 변경 내용 |
| --- | --- |
| Application | 자동 접미사 탐색 및 재시도 제거, 생성 트랜잭션을 `WorkspaceService`로 통합 |
| Domain Exception | 기존 `WorkspaceNameConflictException`을 생성 중복 응답으로 사용 |
| Persistence | `(academy_id, active_name)` unique 제약 충돌의 예외 변환 유지 |
| Presentation | Swagger의 `409` 설명을 동일 활성 이름 충돌로 갱신 |
| Documentation | 비즈니스 정책, API 명세, 처리 흐름을 새 정책과 동기화 |

### 검증

- 중복된 활성 이름으로 생성 요청하면 `WorkspaceNameConflictException`이 발생하고 저장하지 않는 서비스 테스트를 추가했습니다.
- `WorkspaceServiceTest`, `WorkspacePersistenceAdapterTest`, `WorkspacePersistenceAdapterDataJpaTest`를 통과했습니다.
- `compileJava`와 `git diff --check`를 통과했습니다.

> 사용자 관점의 변경 이력은 [CHANGELOG.md](CHANGELOG.md)를 참고해주세요. 📚
