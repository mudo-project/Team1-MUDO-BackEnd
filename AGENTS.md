# AGENTS.md

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. 절대 규칙 (Absolute Rules)

아래 항목은 원칙적으로 반드시 지킨다. 단, 각 항목에 명시된 사전 정의 예외만 적용할 수 있으며, 사용자 승인만으로 새로운 예외를 만들지 않는다. 예외 적용 전에는 다시 한번 확인할 것.

- **시크릿 파일 커밋 금지**: `.env`, `cert/*.json` 등 자격증명·비밀키가 담긴 파일은 절대 Git에 커밋하지 않는다.
- **GitHub 직접 push 금지**: 어떤 브랜치든 원격 저장소로 직접 push하지 않는다.
- **프로덕션 DB 직접 쿼리 금지**: 프로덕션 데이터베이스에 직접 쿼리를 실행하지 않는다. 실행이 필요한 쿼리는 채팅으로 제시하고, 실제 실행은 사용자가 직접 하도록 한다.
- **타 도메인 코드 직접 수정 금지**: 현재 구현 중인 도메인 패키지가 아닌 다른 도메인의 코드 수정·추가·삭제가 필요하면 직접 변경하지 않는다. 단, 도메인 간 조회용 Port·응답 DTO와 이를 구현하는 대상 도메인의 조회 Adapter, Domain Repository, Persistence Adapter, Spring Data JPA Repository, 최소 JPA 조회 코드는 대상 도메인 담당자의 사전 동의가 있을 때 수정할 수 있다. Persistence Adapter는 Domain Repository와 Spring Data JPA Repository를 연결하는 대상 도메인의 영속성 구현체를 의미한다. 요청 도메인은 Port·응답 DTO를 정의하고, 데이터를 소유한 대상 도메인은 자기 영속성 구현을 호출하는 Adapter로 이를 구현한다. 이 경우 Adapter 메서드에는 소비 도메인·용도·관련 PR 번호(선택)를 주석으로 남기고, 기존 도메인 로직은 수정하지 않는다. 그 외 변경이 필요하면 대상 도메인, 대상 코드, 필요한 변경 유형과 이유를 사용자 또는 해당 도메인 담당자에게 명확히 알린다.

## 2. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:

- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them — don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 3. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 4. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:

- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it — don't delete it.

When your changes create orphans:

- Remove imports, variables, and functions that **your changes** made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 5. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:

- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:

```text
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

## 6. 문서 우선 참조 (Project Docs Routing)

코드를 탐색하거나 구현하기 전에, 작업 성격에 맞는 문서를 먼저 확인한다. 문서에 정의된 책임 경계·계약·규칙을 임의로 변경하지 않는다.

| 작업 유형 | 우선 참조 문서 | 확인할 내용 |
|---|---|---|
| 전체 구조, 패키지 위치, 의존 방향 | `docs/ARCHITECTURE.md` | 하프 헥사고날 구조, 레이어 간 의존 방향, 외부 시스템 경계 |
| 도메인 기능 추가·수정, 도메인 간 협업 | `docs/MODULES.md` | 도메인 소유권, 담당 범위, 공개 UseCase 또는 Facade |
| Controller API 추가·수정 | `docs/API_CONTRACT.md` | URI, HTTP Method, 요청·응답 DTO, 인증·인가, 버전 규칙 |
| API 명세서 작성 | `docs/templates/API_SPEC_TEMPLATE.md`, `docs/API_CONTRACT.md` | Notion 복사용 양식, 실제 Controller·DTO·Security·예외 코드 기준의 요청·응답·실패 코드 |
| FastAPI 등 외부 서버 연동 | `docs/INTEGRATION.md` | 호출 책임, 인증 전달, 타임아웃, 재시도, 오류 변환 |
| Entity, Repository, 테이블, 트랜잭션 변경 | `docs/DATABASE.md` | 데이터 소유 도메인, Entity 직접 참조 금지, 트랜잭션 경계 |
| 예외·오류 응답 변경 | `docs/ERROR_HANDLING.md` | `ErrorCode`, 예외 계층, HTTP 응답 형식 |
| Service 메서드 로깅 추가·수정 | `docs/LOGGING_CONVENTION.md` | 이벤트명 규칙(`_시작`/`_완료`/`_실패`), 적용 대상, 파라미터 규칙 |

문서가 없거나 현재 코드와 불일치하면, 추측으로 구현하지 않는다. 불일치 사항과 영향을 받는 도메인을 먼저 알리고 방향을 확인한다.

API 명세서 작성 요청을 받으면 `docs/templates/API_SPEC_TEMPLATE.md`를 먼저 읽고, 기능 구현과 관련 테스트가 모두 통과한 뒤 실제 코드 기준으로 명세서를 작성한다.

## 7. 답변 언어

답변은 한글로 작성한다.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.
