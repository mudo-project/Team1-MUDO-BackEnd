# 워크스페이스 모듈 권한 정의

> 권한 담당자용 시드 목록. 권한 모듈 연동 시 다음 3개를 `permission` 테이블에 추가하세요.

## 1. `WORKSPACE:READ_ALL` — 전체 워크스페이스 조회

| 항목 | 설명 |
|------|------|
| **코드** | `WORKSPACE:READ_ALL` |
| **설명** | 같은 학원의 모든 활성 워크스페이스를 조회하고 관리할 수 있습니다. |
| **적용 대상** | 학원 관리 권한자(원장, 관리자) |
| **구현 상태** | ✅ 구현 완료 (조회 API만, 쓰기는 아직 TODO) |
| **적용 API** | `GET /api/workspaces?scope=ALL` |
| **비고** | 이 권한이 없으면 자신이 참여한 워크스페이스(`scope=MINE`)만 조회 가능 |

**관련 코드:**
- `WorkspaceController.queryWorkspaces()` — `@PreAuthorize("!request.isAll() or hasAuthority('WORKSPACE:READ_ALL')")`
- 향후 업무/댓글 조회도 이 권한 범위로 확장 예정

---

## 2. `WORKSPACE:CREATE` — 워크스페이스 생성/수정

| 항목 | 설명 |
|------|------|
| **코드** | `WORKSPACE:CREATE` |
| **설명** | 워크스페이스를 생성하고, 이름을 수정하며, 참여자를 추가·제거할 수 있습니다. |
| **적용 대상** | 워크스페이스 생성 권한자(선생님, 팀장) |
| **구현 상태** | 🟡 구조 완성, 시드 대기 (TODO 주석 6곳) |
| **적용 API** | `POST /api/workspaces`, `PATCH /api/workspaces/{id}`, `POST /api/workspaces/{id}/members`, `DELETE /api/workspaces/{id}/members/{userId}`, `POST /api/workspaces/{id}/recover` |
| **적용 API (업무)** | `POST /api/workspaces/{id}/tasks` |
| **비고** | 현재는 "현재 참여자" 조건만 검증. 시드 후 이 권한도 함께 검증됨. 자진 탈퇴는 권한 없이 항상 허용. |

**관련 코드 (TODO 위치):**
- `WorkspaceController.create()` — `@PreAuthorize`
- `WorkspaceController.rename()` — `@PreAuthorize`
- `AddWorkspaceMembersService` — 참여자 조건 추가
- `RemoveWorkspaceMemberService` — 타인 제거 시에만 권한 검증
- `WorkspaceTaskController.createTask()` — 업무 생성
- `RecoverWorkspaceService` — 워크스페이스 복구

---

## 3. `WORKSPACE:DELETE` — 워크스페이스 삭제

| 항목 | 설명 |
|------|------|
| **코드** | `WORKSPACE:DELETE` |
| **설명** | 워크스페이스를 삭제(소프트 삭제)할 수 있습니다. |
| **적용 대상** | 워크스페이스 삭제 권한자(선생님, 팀장) |
| **구현 상태** | 🟡 구조 완성, 시드 대기 (TODO 1곳) |
| **적용 API** | `DELETE /api/workspaces/{id}` |
| **비고** | `WORKSPACE:CREATE`와 의도적으로 분리함. 본인이 유일한 참여자인 상태의 삭제는 권한 없이 허용(자진 탈퇴 동등성). |

**관련 코드 (TODO 위치):**
- `DeleteWorkspaceService` — 참여자 2인 이상일 때만 권한 검증

---

## 업무, 댓글, 멘션에 대한 권한

| 영역 | 현재 상태 |
|------|---------|
| **업무 조회** | 워크스페이스 접근 권한으로 통일 (별도 권한 없음) |
| **업무 생성/수정/삭제** | `WORKSPACE:CREATE` 권한 또는 "현재 참여자" |
| **댓글 작성/수정/삭제** | "현재 워크스페이스 참여자"만 가능 (별도 권한 없음) |
| **멘션** | 댓글과 동일. 멘션된 사용자가 제거되어도 기록은 유지하되 알림/접근 권한 없음 |

---

## 권한 모듈 연동 체크리스트

- [ ] `WORKSPACE:READ_ALL` 코드 시드
- [ ] `WORKSPACE:CREATE` 코드 시드
- [ ] `WORKSPACE:DELETE` 코드 시드
- [ ] 권한 조회 API 또는 `AuthUser`에 보유 권한 반영
- [ ] 관련 서비스의 TODO 주석 제거 및 `@PreAuthorize` 적용
- [ ] 통합 테스트로 권한별 접근 제한 검증

---

## 참고 문서

- 구현 상세: [`BUSINESS_RULES.md`](BUSINESS_RULES.md) § 접근 권한
- API 명세: [`WORKSPACE_API.md`](WORKSPACE_API.md) § 인증 및 권한
- 호출 흐름: [`WORKSPACE_API_FLOW.md`](WORKSPACE_API_FLOW.md), [`TASK_API_FLOW.md`](TASK_API_FLOW.md)
