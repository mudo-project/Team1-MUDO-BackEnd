# 워크스페이스 모듈 권한 정의

> 2026-08-10 갱신 — 최초 설계(2026-08-04)의 3개 권한(`READ_ALL`/`CREATE`/`DELETE`, "참여자 AND 권한" 방식)을 재검토해 2개로 좁혔다. 결정 근거는 `docs/superpowers/specs/2026-08-10-workspace-permissions-design.md` 참고.

## 1. `WORKSPACE:READ_ALL` — 참여 여부와 무관한 조회 확장

| 항목 | 설명 |
|------|------|
| **코드** | `WORKSPACE:READ_ALL` |
| **설명** | 같은 학원의 모든 활성 워크스페이스와 그 하위(업무 상세·댓글 목록·반복 업무 템플릿 목록)를 참여 여부와 무관하게 조회할 수 있습니다. |
| **적용 대상** | 학원 관리 권한자(원장, 관리자) |
| **구현 상태** | ✅ 구현 완료 |
| **적용 API** | `GET /api/workspaces?scope=ALL`, `GET /api/workspaces/{id}`, `GET /api/workspaces/{id}/tasks/{taskId}`, `GET /api/workspaces/{id}/tasks/{taskId}/comments`, `GET /api/workspaces/{id}/recurring-templates` |
| **구현 방식** | Controller가 `Authentication.getAuthorities()`에서 `canReadAll` boolean을 뽑아 UseCase에 전달 → Service가 "참여자다 OR canReadAll" 조건으로 판단 |

## 2. `WORKSPACE:CREATE` — 워크스페이스 생성

| 항목 | 설명 |
|------|------|
| **코드** | `WORKSPACE:CREATE` |
| **설명** | 새 워크스페이스를 생성할 수 있습니다. |
| **적용 대상** | 워크스페이스 생성 권한자. 학원마다 역할을 자유 조합할 수 있어(예: 알바/매니저/행정직원), 특정 역할만 생성을 막을 수 있다. |
| **구현 상태** | ✅ 구현 완료 |
| **적용 API** | `POST /api/workspaces` **하나만** |
| **구현 방식** | `WorkspaceController.createWorkspace()`에 `@PreAuthorize("hasAuthority('WORKSPACE:CREATE')")` |

## 3. 그 외 모든 관리 액션 — 권한 체크 없음, 참여자만

| 영역 | 규칙 |
|------|------|
| 워크스페이스 이름변경 | 참여자만 |
| 참여자 추가·제거(자진 탈퇴 포함) | 참여자만 |
| 워크스페이스 삭제(소프트 삭제) | 참여자만(인원수 무관) — 복구 가능, 내부용이라 저위험으로 판단 |
| 워크스페이스 복구 | 삭제 당시 참여자만 |
| 업무 생성/수정/삭제 | 참여자만 |
| 댓글 생성/수정/삭제/완료토글 | 참여자만 |
| 반복 업무 템플릿 생성/수정/삭제 | 참여자만 |

**`WORKSPACE:DELETE`는 만들지 않았다.** 삭제는 소프트 삭제(복구 가능)이고 학원 내부 직원 전용 기능이라 별도 권한이 필요 없다고 판단했다.

## 4. 구현 패턴 — 두 가지 방식을 함께 쓴다

권한 체크 위치가 권한마다 다르다. 아래 두 패턴을 구분해서 이해해야 새 권한을 추가할 때 어느 쪽을 따를지 판단할 수 있다.

### 패턴 A — `@PreAuthorize` (단순 게이트, 예외 없음)

`WORKSPACE:CREATE`에 사용. 참여자 여부와 무관하게 "이 권한이 있는가"만 보면 되는 단순한 경우에 쓴다. 다른 도메인(공지사항 `NOTICE:WRITE`, 캘린더 `CALENDAR:MANAGE`, 결재 `APPROVAL:SUBMIT`) 전체가 이 패턴이다.

```java
@PreAuthorize("hasAuthority('WORKSPACE:CREATE')")
@PostMapping
public ResponseEntity<...> createWorkspace(...) { ... }
```

요청이 컨트롤러 메서드 진입 전에 Spring Security가 가로채서 권한을 확인하고, 없으면 `403`으로 바로 끝난다. DB 조회가 필요 없다.

### 패턴 B — Controller가 boolean을 뽑아 넘김 (참여자 조건과 결합해야 하는 경우)

`WORKSPACE:READ_ALL`에 사용. "참여자다 OR 이 권한이 있다"처럼, 리소스(워크스페이스)를 실제로 읽어봐야 판단할 수 있는 조건과 결합해야 할 때 쓴다. `@PreAuthorize`는 DB 조회 전에 평가되므로 이 조건을 표현할 수 없다.

```text
① 요청 도착
② Controller가 Authentication.getAuthorities()에서
   "WORKSPACE:READ_ALL" 권한 보유 여부를 boolean으로 추출
③ boolean을 UseCase 메서드의 마지막 파라미터로 그대로 전달
④ Service가 워크스페이스를 DB에서 로드
⑤ Service가 (참여자다 OR ③에서 받은 boolean) 조건으로 최종 판단
   → 둘 다 거짓이면 WorkspaceAccessDeniedException(403)
```

```java
// ②③ Controller
boolean canReadAll =
    authentication.getAuthorities().stream()
        .anyMatch(authority -> "WORKSPACE:READ_ALL".equals(authority.getAuthority()));
TaskDetail detail =
    taskDetailQueryUseCase.getTaskDetail(workspaceId, taskId, authUser.userId(), canReadAll);

// ④⑤ Service
Workspace workspace = workspaceRepository.findById(workspaceId).orElseThrow(...);
if (!workspace.getMemberIds().contains(requesterId) && !canReadAll) {
  throw new WorkspaceAccessDeniedException();
}
```

이 패턴은 원래 워크스페이스 상세 조회(`WorkspaceController.getWorkspaceDetail()`)에 이미 있던 걸 이번에 업무 상세·댓글 목록·반복 업무 템플릿 목록 3곳에 그대로 확장한 것이다. Command 레코드가 없는 조회성 UseCase는 boolean을 메서드 파라미터에 직접 추가하고, Command가 있는 쓰기성 UseCase라면 Command 필드에 추가하는 식으로 확장한다(이번 3곳은 전부 Command 없는 조회형이라 파라미터로 추가).

**새 권한을 추가할 때 어느 패턴을 쓸지 판단하는 기준**: 그 권한 체크가 "리소스를 안 읽어도 판단 가능한가"(패턴 A) vs "리소스 상태(참여자 목록 등)와 결합해야 하는가"(패턴 B).

## 참고 문서

- 결정 근거: `docs/superpowers/specs/2026-08-10-workspace-permissions-design.md`
- 구현 상세: [`BUSINESS_RULES.md`](BUSINESS_RULES.md) § 접근 권한
- API 명세: [`WORKSPACE_API.md`](WORKSPACE_API.md) § 인증 및 권한
