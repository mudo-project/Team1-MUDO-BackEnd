# Task 2 보고서: 워크스페이스 목록 Query Port와 정렬 Adapter

## 구현 커밋

- 구현 및 focused 테스트 커밋: `eee0ad72b32c6dd32443fc91a4a1ce029401b7e5`

## 변경 파일

- `src/main/java/com/academy/mudogroupware/workspace/application/query/WorkspaceListScope.java`
- `src/main/java/com/academy/mudogroupware/workspace/application/query/WorkspaceListItem.java`
- `src/main/java/com/academy/mudogroupware/workspace/application/port/WorkspaceListQueryPort.java`
- `src/main/java/com/academy/mudogroupware/workspace/infrastructure/persistence/workspace/WorkspaceListQueryAdapter.java`
- `src/test/java/com/academy/mudogroupware/workspace/infrastructure/persistence/workspace/WorkspaceListQueryAdapterDataJpaTest.java`

`WorkspaceService.java`, `.gitignore`, `.claude/`, 사용자 생성 `V3.1.3__create_workspace_recent_access_table.sql`은 변경하거나 스테이지하지 않았다.

## TDD 증거

### RED

생산 타입을 추가하기 전에 아래 focused 테스트를 실행했다.

```powershell
.\gradlew.bat test --tests "com.academy.mudogroupware.workspace.infrastructure.persistence.workspace.WorkspaceListQueryAdapterDataJpaTest"
```

`compileTestJava`가 실패했고, 실패 원인은 의도대로 미구현 타입 3건이었다.

- `workspace.application.query.WorkspaceListItem` 패키지 없음
- `WorkspaceListQueryAdapter` 심볼 없음 (필드 주입 지점)
- `WorkspaceListQueryAdapter` 심볼 없음 (`@Import` 지점)

### GREEN

포트, query record/enum, JPQL adapter만 추가한 뒤 아래 명령으로 focused 테스트를 실제 재실행했다.

```powershell
.\gradlew.bat test --rerun-tasks --tests "com.academy.mudogroupware.workspace.infrastructure.persistence.workspace.WorkspaceListQueryAdapterDataJpaTest"
```

결과: `BUILD SUCCESSFUL`, `5 actionable tasks: 5 executed`.

테스트는 다음을 검증한다.

1. 요청자 자신의 최근 접근 시각 내림차순과 현재 참여자 수 집계
2. 미방문 항목의 후순위 배치와 `createdAt` 내림차순, 다른 사용자의 접근 기록 무시
3. `findAll`의 타 학원 및 소프트 삭제 워크스페이스 제외
4. 일반 사용자의 현재 참여 확인과 전체 조회 권한자의 같은 학원 활성 워크스페이스 접근 확인

## 쿼리 설계

- `findMine`: 같은 학원·활성 워크스페이스 중 요청자가 `workspace_member`에 있는 행만 `exists` 서브쿼리로 제한한다.
- `findAll`: 같은 학원·활성 워크스페이스를 참여 여부와 무관하게 조회한다.
- 두 목록 쿼리 모두 `WorkspaceRecentAccessJpaEntity`를 요청자 `userId` 조건으로만 `LEFT JOIN`한다. 따라서 타 사용자의 접근 기록은 정렬에 참여하지 않는다.
- 정렬은 최근 접근 시각이 있는 행 우선, 그 시각 내림차순, 미방문 행은 `workspace.createdAt` 내림차순이다.
- `workspace.members`를 `LEFT JOIN`하고 `count(member)`를 projection에 담아 현재 참여자 수를 반환한다.
- `existsAccessible`: 전체 조회 권한이면 같은 학원 활성 행 존재 여부만, 아니면 현재 참여자 조인까지 포함해 확인한다.

## 자기 검토

- 요구된 `MINE`, `ALL` enum과 `WorkspaceListItem` record, Port 메서드 3개를 모두 추가했다.
- 소프트 삭제 필터는 목록·접근 확인 모든 쿼리에 적용했다.
- 기존 `WorkspaceJpaRepository`와 명시 금지 파일은 변경하지 않았다.
- 커밋 전 staged diff에 `git diff --cached --check`를 실행해 공백 오류가 없음을 확인했다.

## 우려사항

- 검증은 H2 기반 focused Data JPA 테스트다. 운영 MySQL의 실제 데이터 규모에서는 목록 집계·정렬 쿼리 실행 계획과 인덱스 적합성을 별도로 확인할 필요가 있다.
