# 🔄 공유파일 도메인 변경 이력

## ✅ 2026-08-16 · 공유 파일 서비스 시작·완료 로그 추가

### 변경 목적

공유 파일 요청의 처리 시작과 정상 완료를 운영 로그에서 추적할 수 있도록 한다. API 예외는 기존 `GlobalExceptionHandler`가 `event=exception_handled`로 기록하므로 서비스에서 다시 기록하지 않아 중복 로그를 피한다.

### 구현 변경

- 폴더 목록, 파일 상세, 검색, 폴더 생성, 파일 업로드, Google Docs·Sheets·Slides 생성, 파일명 변경·이동, 휴지통 이동, 원본 다운로드·Export 서비스에 `shared_file_*_시작`/`shared_file_*_완료` 로그를 추가했다.
- 완료 로그에는 item ID, 결과 건수, 다음 페이지 여부, 파일 크기 등 운영 추적에 필요한 최소 정보만 남긴다.
- 커서 값, 액세스 토큰, 업로드 파일 본문은 로그에 남기지 않는다.
- `AFTER_COMMIT` 루트 초기화 및 Drive 보상 처리 실패처럼 전역 예외 핸들러까지 도달하지 않는 경로의 기존 실패 로그는 유지한다.

### 검증

- `./gradlew.bat test --tests "com.academy.mudogroupware.sharedfile.application.service.*" --tests "com.academy.mudogroupware.sharedfile.presentation.api.SharedFileControllerTest" --tests "com.academy.mudogroupware.sharedfile.infrastructure.external.google.GoogleDriveAdapterTest"` 통과.
- `git diff --check` 통과.

## ✅ 2026-08-14 · PATCH(이름 변경+이동) 원자성 확보 — UpdateSharedFileItemUseCase 통합 (이슈 #406)

### 변경 목적

PR #404 CodeRabbit 리뷰(Major)에서 지적되어 이슈 #406으로 분리해뒀던 부분 실패 문제를 고친다. `PATCH /api/shared-files/items/{itemId}`에 `name`과 `parentId`를 둘 다 보내면, 예전 `SharedFileController.updateItem()`이 `RenameSharedFileItemUseCase.rename()` → `MoveSharedFileItemUseCase.move()`를 순서대로 호출했다. rename이 Drive에 실제로 반영되고 성공한 뒤 move가 실패하면(잘못된 목적지, 네트워크 오류 등) 이름은 바뀐 채로 남고 클라이언트에는 실패만 반환됐다.

### 구현 변경

- `SharedFileDrivePort.rename()`/`move()`를 제거하고 `updateItem(accessToken, itemId, name, fromParentId, toParentId)` 하나로 합쳤다. `GoogleDriveAdapter`를 보면 두 메서드가 애초에 같은 Drive 엔드포인트(`PATCH /files/{itemId}`, `files.update`)를 치고 있었다 — rename은 body에 `name`을, move는 body 없이 쿼리파라미터 `addParents`/`removeParents`만 썼을 뿐이다. `updateItem()`은 이 둘을 **한 번의 PATCH 요청**에 함께 실어, name이 null이면 body를 생략하고 toParentId가 null이면 쿼리파라미터를 안 붙인다.
- `RenameSharedFileItemUseCase`/`RenameSharedFileItemService`, `MoveSharedFileItemUseCase`/`MoveSharedFileItemService`를 삭제하고, 이름 변경·이동 검증 로직(확장자 동일성 검사, 목적지 폴더·순환·루트자신 검사)을 전부 흡수한 `UpdateSharedFileItemUseCase`/`UpdateSharedFileItemService`를 신설했다. 목적지 검증(순환·유형 확인)을 대상 itemId 자신의 메타데이터 조회보다 먼저 하도록 순서를 유지해, "새 부모가 자기 자신" 같은 요청은 그 메타데이터 조회를 생략하고 즉시 거부한다 — 다만 대상 itemId가 루트 하위인지 확인하는 `SharedFileRootGuard.requireDescendant()` 자체는 어차피 선행 검증이라 이 경우에도 Drive 조회가 발생한다(기존 동작 그대로, Drive를 아예 안 부르는 게 아니라 "대상 메타데이터 재조회"만 생략됨).
- `SharedFileController.updateItem()`이 `updateSharedFileItemUseCase.update(itemId, name, parentId)` 한 번만 호출한다. HTTP 요청/응답 계약(`SHAREDFILE_API.md` 9번)은 그대로다 — 내부 구현만 바뀌었다.
- 이 두 UseCase를 부르는 곳이 Controller뿐이었어서(다른 도메인·API에서 재사용 없음) 안전하게 통째로 교체했다.

### 검증

- `GoogleDriveAdapterTest`에 `updateItem()` 검증 3건(name만/parent만/**둘 다 한 PATCH 요청에 함께 실림**) — 마지막 테스트가 원자성의 직접 증거다.
- `UpdateSharedFileItemServiceTest`(12건)로 기존 `RenameSharedFileItemServiceTest`(5)+`MoveSharedFileItemServiceTest`(7) 커버리지를 이관하고, `updatesNameAndParentInASingleDrivePortCallWhenBothGiven`으로 `sharedFileDrivePort.updateItem()`이 정확히 1번만 호출됨을 검증.
- `SharedFileControllerTest`에 둘 다 준 경우 UseCase가 1번만 호출되는 걸 검증하는 테스트 추가.
- 전체 `./gradlew test --tests "com.academy.mudogroupware.sharedfile.*"` 통과, 전체 프로젝트 `./gradlew test` 회귀 없이 통과.

## ✅ 2026-08-14 · PR #492 CodeRabbit 리뷰 반영

### 변경 목적

PR #492(루트 id 노출 + parentId 생략 허용)에 대한 CodeRabbit 리뷰 3건을 반영한다.

### 구현 변경

- **parentId 빈 값 처리 불일치**: `parentId == null`일 때만 루트로 대체하고 `""`/공백은 걸러지지 않아, `SharedFileRootGuard`가 빈 id로 Drive 조회를 시도하며 엉뚱한 예외(`SharedFileItemNotFoundException` 등)로 새던 문제를 고쳤다. `CreateSharedFolderService`/`CreateGoogleWorkspaceFileService`/`UploadSharedFileService` 3곳에 `parentId != null && parentId.isBlank()`일 때만 `BadRequestException`을 던지는 체크를 다시 넣었다. `@NotBlank`로는 "null 허용, blank 거부"를 동시에 표현할 수 없어 DTO가 아니라 서비스 레이어에 뒀다. `ListSharedFileItemsService`에도 같은 gap이 있지만 이번 PR이 건드리지 않은 기존 코드라 범위에서 제외했다.
- **빈 parentId 경계값 테스트 누락**: 위 3개 서비스 테스트에 parentId가 `" "`일 때 여전히 400인지 검증하는 케이스를 추가했다.
- **`ready=false` 응답의 HTTP 레벨 테스트 누락**: `SharedFileControllerTest`에 `ready=false`일 때 `rootId`가 JSON에 `null`로 내려가는지 검증하는 테스트를 추가했다. `SharedFileRootResponse`에 `@JsonInclude(NON_NULL)`이 없어 필드가 생략되지 않고 `null`로 직렬화되는 걸 먼저 확인한 뒤, 프로젝트에 이미 있는 패턴(`GoogleAccountConnectionControllerTest`)대로 `jsonPath(...).value(nullValue())`를 썼다.

### 검증

- 전체 `./gradlew test --tests "com.academy.mudogroupware.sharedfile.*"` 통과(134개), 전체 프로젝트 `./gradlew test` 회귀 없이 통과.

## ✅ 2026-08-14 · 루트 id 노출 + 생성·업로드 API의 parentId 생략(=루트) 허용

### 변경 목적

프론트에서 최상위(시스템 루트)에 폴더·파일을 만들 수 없다는 피드백을 받았다. 생성 3개 API(`POST /folders`, `POST /google-files`, `POST /items/upload`)는 `parentId`가 필수였는데, `GET /root`는 `ready` 상태만 내려주고 루트 자신의 id는 알려주지 않아서, 프론트가 "루트 밑에 만들어달라"는 요청 자체를 표현할 방법이 없었다. 목록 조회(`GET /items`)만 `parentId` 생략 시 루트를 기본값으로 쓰는 fallback이 있었다.

추가로, 하위 폴더에 있던 항목을 다시 루트 바로 아래로 이동(`PATCH /items/{itemId}`)시키는 시나리오도 같은 이유(루트 id를 모름)로 막혀 있어서, 루트 id 노출 자체가 생성 3개의 fallback만으로는 완전히 대체되지 않는다고 판단했다. 두 가지를 함께 적용한다.

### 구현 변경

- `SharedFileRootView`/`SharedFileRootResponse`에 `rootId`를 추가했다. `ready=false`면 `null`이다(도메인 모델의 `markFailed()`/`failed()`가 `googleRootFolderId`를 항상 비워두므로 별도 분기 없이 자연스럽게 null이 된다).
- `RecreateSharedFileRootService.recreate()`도 새로 만든 루트 폴더의 id를 `rootId`로 채워 반환한다.
- `CreateSharedFolderRequest`/`CreateGoogleWorkspaceFileRequest`의 `parentId` `@NotBlank`를 제거했고, `uploadItem`의 `@RequestParam String parentId`도 `required = false`로 바꿨다.
- `CreateSharedFolderService`/`CreateGoogleWorkspaceFileService`/`UploadSharedFileService` 3곳 모두, `parentId`가 null이면 즉시 `BadRequestException`을 던지던 부분을 `ListSharedFileItemsService`와 동일한 `parentId == null ? rootId : parentId` 패턴으로 바꿨다. 목적지가 루트 자신이면 Guard 검증을 생략하는 기존 규칙(`if (!targetParentId.equals(rootId))`)이 그대로 적용된다.
- Move(이동) API는 이번 변경 대상에서 제외했다 — `UpdateSharedFileItemRequest.parentId` 생략은 이미 "이동 안 함"(이름 변경 전용 요청)이라는 의미로 쓰이고 있어서, 같은 필드에 "생략=루트"까지 얹으면 의미가 충돌한다. 루트로 되돌리는 이동은 이번에 노출한 `rootId`를 프론트가 명시적으로 `parentId`에 넣어 호출하는 방식으로 이미 해결된다(추가 API 변경 불필요, `MoveSharedFileItemService`는 원래도 목적지가 루트 자신이면 Guard를 생략하도록 설계돼 있었다).

### 검증

- `GetSharedFileRootServiceTest`/`RecreateSharedFileRootServiceTest`에 `rootId` 케이스 추가.
- `CreateSharedFolderServiceTest`/`CreateGoogleWorkspaceFileServiceTest`/`UploadSharedFileServiceTest`의 "parentId null이면 400" 테스트를 "parentId 생략 시 루트 아래 생성 + Guard 미호출 검증"으로 교체.
- `SharedFileControllerTest`에 3개 API 전부 parentId 생략 시 201을 검증하는 테스트 추가, `GET /root`·재생성 응답의 `rootId` JSON 필드 검증 추가.
- 전체 `./gradlew test --tests "com.academy.mudogroupware.sharedfile.*"` 통과(129개), 전체 프로젝트 `./gradlew test` 회귀 없이 통과.

## ✅ 2026-08-14 · 낙관적 락 실효성 버그 수정 + Initializer 버전 유실·보상 정책 통일

### 변경 목적

PR #369에서 동시 초기화 경합(더블클릭 등)을 막으려 도입한 `@Version`이 실제로는 충돌을 전혀 감지하지 못하던 버그를 고친다. `SharedFileRootPersistenceAdapter.save()`가 저장 직전에 행을 다시 조회해 그 자리에서 수정하는 방식이라, merge 시점에 비교할 버전이 항상 최신값이 되어버려 낙관적 락이 무력화돼 있었다.

### 구현 변경

- `SharedFileRoot`(도메인)에 `version`(nullable) 필드와 영속성 복원 전용 `restore(status, folderId, version)` 팩토리를 추가했다. `ready()`/`failed()`는 아직 저장되지 않은 인스턴스이므로 `version=null`을 유지한다.
- `SharedFileRootEntity.update()` 인스턴스 메서드를 제거하고, 호출자가 조회 시점에 들고 있던 version으로 detached 엔티티를 만드는 `forUpdate(version, status, folderId)` 정적 팩토리로 교체했다.
- `SharedFileRootPersistenceAdapter.save()`가 저장 직전 재조회를 하지 않고 `root.getVersion()`으로 insert(`create()`)/update(`forUpdate()`)를 분기한다. Spring Data JPA는 `@Version` 필드(`Long`)가 `null`이면 새 엔티티로 판단하므로 이 분기만으로 `em.persist()`/`em.merge()`가 올바르게 갈린다. `toDomain()`도 `ready()/failed()` 대신 `restore()`로 바꿔 조회 시 버전을 항상 보존한다.
- `SharedFileRootInitializer.handle()`이 `find()`로 읽은 기존 루트를 버리고 매번 새 `SharedFileRoot.ready()/failed()`를 만들던 부분을 고쳤다 — 어댑터를 위처럼 고친 뒤에는 이 상태로 두면 이미 있는 행에도 매번 insert를 시도해 PK 충돌로 실패한다. `RecreateSharedFileRootService`와 동일하게 기존 객체를 `replaceWith()`/`markFailed()`로 바꿔 version을 유지한 채 저장한다.
- `SharedFileRootInitializer`의 DB 저장 실패 처리를 `RecreateSharedFileRootService`와 통일했다 — 지금까지는 DB 저장이 실패해도 방금 만든 Drive 폴더를 그대로 두고 로그만 남겼는데(orphan), 이제 동일하게 trash로 보상을 시도한다. "실패로 덮어쓰기를 재시도하지 않는다"는 기존 정책 자체는 유지한다.

### 검증

- 신규 `SharedFileRootPersistenceAdapterDataJpaTest`(3): insert, 정상 버전으로 update, **오래된 버전으로 저장 시 낙관적 락 충돌**(수정 전 코드에서는 예외 없이 조용히 덮어써지던 것을 그대로 재현하는 회귀 테스트).
- `SharedFileRootTest`에 version 관련 케이스 3건 추가.
- `SharedFileRootInitializerTest`에 version 보존 2건 + Drive 폴더 보상(trash) 검증 1건 추가, 기존 8개 테스트 전부 통과.
- 전체 `./gradlew clean compileJava compileTestJava test --tests "com.academy.mudogroupware.sharedfile.*"` 통과(126개), 전체 프로젝트 `./gradlew test`도 회귀 없이 통과.

## ✅ 2026-08-12 · HTTP API·권한·문서 구현 (Task6)

### 변경 목적

`2026-08-10-sharedfile-implementation.md` 계획서의 Task6(HTTP API·권한·문서·통합 검증)을 구현한다. 이 라운드가 끝나면 Task1~6 계획이 모두 완료된다.

### 구현 변경

- `SharedFileController`에 설계서의 11개 엔드포인트를 전부 구현했다. `SHAREDFILE:MANAGE`가 콘텐츠 API(3~11번)를, `SHAREDFILE:ROOT_MANAGE`가 재생성(2번)만 보호한다.
- **설계 갭 해결**: `POST /api/shared-files/root/recreation`을 담당할 `RecreateSharedFileRootUseCase`/`RecreateSharedFileRootService`를 신설했다. AFTER_COMMIT 이벤트 리스너인 `SharedFileRootInitializer`와 달리 Drive 호출 실패를 삼키지 않고 그대로 던져 요청자에게 알리며, 이미 `READY`인 루트에 재생성을 요청하면 `IllegalStateException`(→ `COMMON_409_1`)으로 거부한다.
- 요청 DTO(`CreateSharedFolderRequest`, `CreateGoogleWorkspaceFileRequest`, `UpdateSharedFileItemRequest`)와 응답 DTO(`SharedFileRootResponse`, `SharedFileItemResponse`, `SharedFileItemsResponse`)를 추가했다. `SharedFileItemsResponse`는 다른 도메인의 offset 기반 공통 페이지 응답 대신 Drive page token 그대로인 `cursor`/`hasNext`/`nextCursor`를 쓴다.
- PATCH(이름 변경·이동)는 `name`·`parentId`를 하나의 요청으로 받아 있는 필드만 순서대로(이름 변경 → 이동) 적용한다. 둘 다 없으면 `IllegalArgumentException`(→ `COMMON_400_2`)으로 거부한다.
- 업로드는 `dataimport` 도메인과 동일하게 `@RequestPart MultipartFile` + `consumes = MULTIPART_FORM_DATA_VALUE`로 받아 `byte[]`로 변환해 `UploadSharedFileUseCase`에 위임한다.
- 다운로드는 `timetable` 도메인의 export 엔드포인트와 동일하게 `ResponseEntity<byte[]>` + `Content-Disposition: attachment`로 응답하며, `GlobalApiResponse`로 감싸지 않는다.
- `GetGoogleAccessTokenUseCase`가 던지는 google 도메인 예외(`GOOGLE_409_1` 등)는 별도 `SHAREDFILE_409_2`로 감싸지 않고 그대로 전파한다 — `ApplicationException`을 상속하므로 `GlobalExceptionHandler`가 자동으로 처리한다. Task2~3에서 남겨뒀던 "Task4에서 추가 예정" 메모는 실제로는 필요 없었다.

### 계획서와 다르게 처리한 부분

- `SharedFileSecurityIntegrationTest`(전체 컨텍스트 통합 테스트)를 별도로 만들지 않고, `@WebMvcTest` + `@Import(@EnableMethodSecurity 설정)` 슬라이스 테스트(`SharedFileControllerTest`) 안에서 `SHAREDFILE:MANAGE`/`SHAREDFILE:ROOT_MANAGE` 상호 배제와 미인증 401을 함께 검증했다. `WorkspaceController` 계열은 이미 이 방식으로 `@PreAuthorize`를 검증해왔고(`WorkspaceControllerTest`), 별도 `@SpringBootTest` 통합 테스트를 추가하는 것은 이 프로젝트의 `@EnableMethodSecurity`가 슬라이스 컨텍스트에서도 동일하게 동작하는 한 중복이라 생략했다(YAGNI).

### 검증

- 신규 테스트 19개(`RecreateSharedFileRootServiceTest` 3, `SharedFileControllerTest` 16).
- 전체 `./gradlew compileJava compileTestJava test --tests "com.academy.mudogroupware.sharedfile.*"` 통과(115개), `--tests "com.academy.mudogroupware.google.*"` 통과(회귀 없음).

## ✅ 2026-08-12 · V3.1.8 마이그레이션을 테이블·권한 시딩으로 분리

### 변경 목적

로컬에서 `V3.1.8__create_shared_file_root_and_permissions.sql`을 이미 실행한 상태였다. 앞으로 권한 카탈로그만 별도로 추가·수정할 수 있도록, 이미 적용된 `V3.1.8`을 편집하는 대신 권한 INSERT 부분만 새 `V3.1.9`로 분리했다.

### 구현 변경

- `V3.1.8__create_shared_file_root_and_permissions.sql` → `V3.1.8__create_shared_file_root.sql`로 이름을 바꾸고 `shared_file_root` 테이블 생성만 남겼다.
- `SHAREDFILE:MANAGE`/`SHAREDFILE:ROOT_MANAGE` 권한 INSERT 두 건을 신규 `V3.1.9__seed_shared_file_permissions.sql`로 옮겼다. `NOT EXISTS` 가드는 그대로 유지해 이미 권한이 있는 환경에서 재실행해도 안전하다.

### 주의

- 이미 develop에 병합된 `V3.1.8`을 수정하는 것이라 체크섬이 바뀐다. 이 브랜치 기준으로 로컬 DB를 이미 마이그레이션한 사람은 `flyway repair` 또는 `flyway_schema_history`에서 `V3.1.8` 행을 지우고 `V3.1.8`+`V3.1.9`를 다시 적용해야 한다. CI·신규 환경은 처음부터 두 파일을 순서대로 적용하므로 영향이 없다.

## ✅ 2026-08-11 · PR #391 CodeRabbit 리뷰 반영

### 변경 목적

PR #391(Task4+5)에 대한 CodeRabbit 리뷰 11건을 반영한다.

### 구현 변경

- **null 입력 방어**: `CreateSharedFolderService`/`UploadSharedFileService`/`CreateGoogleWorkspaceFileService`의 `parentId`, `CreateGoogleWorkspaceFileService`의 `type`이 null이면 어댑터까지 전달돼 NPE가 나던 문제를 `BadRequestException`으로 막았다.
- **업로드 크기 검증**: 호출자가 선언한 `size` 값 대신 실제 `content.length`로만 100MB를 검사하도록 바꿨다. 선언 크기를 속이는 방식의 우회가 불가능해졌다. `UploadSharedFileUseCase`에서 `size` 파라미터 자체를 제거했다(어차피 항상 `content.length`와 같아야 하므로 별도로 신뢰할 이유가 없음).
- **조회 전용 트랜잭션 경계**: `GetSharedFileRootService`/`GetSharedFileItemService`/`ListSharedFileItemsService`/`SearchSharedFileItemsService`에 `@Transactional(readOnly = true)`를 추가했다(다른 도메인 조회 서비스 컨벤션과 통일).
- **Move 목적지 검증**: `MoveSharedFileItemService`가 목적지가 폴더인지, 이동 대상 자신을 목적지로 지정했는지, 이동 대상의 자손을 목적지로 지정해 순환이 생기는지까지 검증한다.
- **Search 페이지네이션**: 원본 Drive 페이지를 필터링(type·루트 하위)한 결과가 요청한 `size`보다 적으면, 원본 페이지가 남아있는 동안 계속 더 가져오도록 바꿨다. 이전에는 필터링 전 크기만 보고 실제로는 결과가 더 있는데도 적게(심지어 0건) 응답할 수 있었다. 마지막으로 처리한 페이지 안에서 `size`를 넘겨 남는 매칭은 원본 커서가 페이지 단위라 다시 가져올 수 없어 버리고 `hasNext=true`만 알린다(알려진 한계).
- **업로드 멀티파트 보안**: `GoogleDriveAdapter.upload()`의 boundary를 고정 문자열에서 요청마다 발급하는 랜덤 값(UUID)으로 바꿔 파일 내용에 같은 문자열이 있을 때 파트 경계가 깨지는 문제를 막았다. `contentType`도 `MediaType.parseMediaType()`으로 파싱·정규화해 CRLF가 섞인 값이 헤더에 그대로 삽입되는 인젝션을 막았다(파싱 실패 시 `application/octet-stream`으로 대체).
- `SHAREDFILE_API_FLOW.md`의 "10개 API" 표기를 11개로 정정.

### 반영하지 않은 항목

- **멀티파트 본문 스트리밍**: 100MB까지 메모리에 전부 올리는 현재 방식을 스트리밍으로 바꾸자는 제안은, Port/UseCase 시그니처까지 바꿔야 하는 아키텍처 변경이라 이번 라운드에는 반영하지 않았다. 100MB 캡이 있고 동시 업로드가 잦지 않은 내부 도구라 우선순위를 낮게 봤다.

### 검증

- 신규·수정 테스트 12개(null 검증 4, 업로드 크기 2, Move 목적지 검증 3, Search 페이지네이션 2, 업로드 보안 1).
- 전체 `./gradlew clean compileJava compileTestJava test --tests "com.academy.mudogroupware.sharedfile.*"` 통과(96개).

## ✅ 2026-08-11 · 이름변경·이동·삭제·다운로드 UseCase 구현 (Task5)

### 변경 목적

계획서 Task5를 구현한다. 이 라운드가 끝나면 Task1~5로 공유파일 UseCase 계층(HTTP API 제외) 전체가 완성된다.

### 구현 변경

- `RenameSharedFileItemUseCase`: 일반 업로드 파일은 확장자가 바뀌면 거부(`SharedFileInvalidNameException`), 폴더·Google 파일은 이름을 그대로 받는다.
- `MoveSharedFileItemUseCase`: PATCH 요청에 새 parentId만 오므로 현재 parentId는 `SharedFileDrivePort.getItem()`으로 직접 조회한다. 목적지가 시스템 루트 자신이면 Guard 검증을 생략한다(생성·목록조회와 동일한 원칙).
- `TrashSharedFileItemUseCase`: Guard 검증 후 `SharedFileDrivePort.trash()` 호출.
- `DownloadSharedFileUseCase`: `format` 없으면 원본 다운로드, 있으면 `DriveItem.workspaceType()`으로 원본이 Docs/Sheets/Slides 중 무엇인지 확인해 `GoogleWorkspaceExportFormat.valueOf(유형_format)`으로 매핑한다. 존재하지 않는 조합(enum 상수 없음)은 `IllegalArgumentException`을 잡아 `SharedFileInvalidExportFormatException`으로 변환한다.
- `DriveItem`에 `workspaceType()`/`isRegularFile()` 추가 — Google MIME type 문자열 비교를 Port 값 객체 안에 가둬, Rename·Download 서비스가 Google 고유 문자열을 직접 다루지 않게 했다.

### 검증

- 신규 테스트 22개(RenameSharedFileItemServiceTest 5, MoveSharedFileItemServiceTest 5, TrashSharedFileItemServiceTest 3, DownloadSharedFileServiceTest 5, DriveItemTest 4).
- 전체 `./gradlew clean compileJava compileTestJava test --tests "com.academy.mudogroupware.sharedfile.*"` 통과(84개).

## ✅ 2026-08-11 · 조회·검색·생성 UseCase 구현 (Task4)

### 변경 목적

계획서 Task4를 구현한다. 이 라운드가 끝나면 시스템 루트 하위 폴더·파일을 실제로 조회·검색·생성·업로드할 수 있다. 이름 변경·이동·삭제·다운로드(Task5)와 HTTP API(Task6)는 이 범위 밖이다.

### 구현 변경

- `GetSharedFileRootUseCase`, `ListSharedFileItemsUseCase`, `GetSharedFileItemUseCase`, `SearchSharedFileItemsUseCase`, `CreateSharedFolderUseCase`, `UploadSharedFileUseCase`, `CreateGoogleWorkspaceFileUseCase` 7개 구현.
- `parentId`가 시스템 루트 자신이면(목록 조회, 생성) `SharedFileRootGuard` 검증을 생략한다 — Guard는 "루트 자신을 대상으로 지정하는 것"을 거부하도록 설계돼 있어, 대신 "루트 자신을 부모로 지정하는 것"(정상적인 최상위 생성/조회)까지 막지 않기 위함이다.
- `GoogleDriveAdapter.upload()`를 실제 구현했다. RestClient가 `multipart/related`를 직접 지원하지 않아, 메타데이터(JSON) 파트와 파일 내용 파트를 하나의 boundary로 수동 구성한다.
- `DriveItem`에 `isFolder()`를 추가해, mimeType 문자열 비교를 Adapter/Port 경계 안쪽에 가뒀다(검색 type 필터가 애플리케이션 계층에서 Google MIME type을 직접 비교하지 않도록).
- 검색에 `SharedFileItemType`(FILE/FOLDER) 필터를 추가했다 — 설계서의 `type?` 입력에 대응한다.

### 계획서와 다르게 처리·발견한 부분

- **설계 갭 발견**: `POST /api/shared-files/root/recreation`(시스템 루트 재생성)을 담당할 UseCase가 계획서 Task1~6 어디에도 명시돼 있지 않다. Task6에서 신설 여부를 결정해야 한다. 자세한 내용은 `SHAREDFILE_API_FLOW.md`의 2번 항목 참고.
- **설계 갭 발견**: 10개 API 목록에 다운로드 전용 엔드포인트가 없어 `GET /api/shared-files/items/{itemId}/download`를 11번째로 신설하기로 결정(설계서에도 반영, git 미추적 로컬 문서).

### 검증

- 신규 서비스 테스트 27개(GetSharedFileRootServiceTest 3, ListSharedFileItemsServiceTest 4, GetSharedFileItemServiceTest 4, SearchSharedFileItemsServiceTest 5, CreateSharedFolderServiceTest 4, CreateGoogleWorkspaceFileServiceTest 4, UploadSharedFileServiceTest 5) + `GoogleDriveAdapterTest`에 업로드 케이스 1건 추가.
- 전체 `./gradlew clean compileJava compileTestJava test --tests "com.academy.mudogroupware.sharedfile.*"` 통과(62개).

## ✅ 2026-08-11 · PR #369 CodeRabbit 리뷰 반영

### 변경 목적

PR #369에 대한 CodeRabbit 리뷰 4건을 반영한다.

### 구현 변경

- **동시 초기화 경합**: `SharedFileRootEntity`에 `@Version`을 추가했다. 학원 1개=jar 1개+스키마 1개 구조가 테넌트 *간* 경합은 막아주지만, 같은 학원 안에서 관리자가 연동을 짧은 시간에 두 번 트리거하면(더블클릭 등) `@TransactionalEventListener`가 스레드별로 동기 실행돼 같은 싱글턴 행을 동시에 갱신할 수 있다는 점은 막지 못한다. `SharedFileRootInitializer`를 Drive 호출 결과를 계산하는 `resolveRoot()`와 DB에 반영하는 `persist()`로 분리하고, `persist()`에서 `DataAccessException`(낙관적 락 충돌 또는 PK 충돌)이 나면 "실패로 덮어쓰기"를 재시도하지 않고 로그만 남긴 채 포기한다 — 먼저 커밋한 결과를 신뢰한다.
- **`DriveItem.parentIds` 방어적 복사**: compact constructor에서 `List.copyOf()`로 불변 스냅샷을 만든다.
- **`createWorkspaceFile`의 Google MIME type 노출**: `SharedFileDrivePort.createWorkspaceFile()`이 `String workspaceMimeType` 대신 신규 `GoogleWorkspaceFileType`(DOCS/SHEETS/SLIDES) enum을 받는다. Google MIME type 매핑은 `GoogleDriveAdapter` 안으로 옮겼다.
- **`listFiles`의 URI 문자열 연결**: `UriComponentsBuilder.queryParam()` + `URI` 객체 전달로 바꿨다. 검색어에 `&`가 있으면 `q` 파라미터가 끊기고, `{`/`}`가 있으면 `RestClient`가 URI 템플릿으로 오인해 예외를 던지던 문제를 고쳤다.

### 검증

- `SharedFileRootInitializerTest`에 동시 저장 충돌 시 재덮어쓰기하지 않는 케이스 추가.
- `GoogleDriveAdapterTest`에 `createWorkspaceFile` MIME 매핑, 검색어 `&`/`{}` 케이스 추가.
- 전체 `./gradlew clean test --tests "com.academy.mudogroupware.sharedfile.*"` 통과(32개).

## ✅ 2026-08-11 · 시스템 루트 자동 생성과 Drive 연동 하부구조 구현 (Task2+3)

### 변경 목적

`2026-08-10-sharedfile-implementation.md` 계획서의 Task2(루트 영속성·이벤트 구독)와 Task3(Drive Port·경로 검증·오류 매핑)을 구현한다. 이 라운드가 끝나면 "Google 계정 연결 성공 → 시스템 루트 자동 생성·DB 저장"까지 실제로 동작한다. 조회·생성·수정·삭제·다운로드 UseCase와 HTTP API(Task4~6)는 이 범위 밖이다.

### 구현 변경

- `SharedFileRoot`/`SharedFileRootStatus` 도메인 모델과 `V3.1.8` 마이그레이션(`shared_file_root` 테이블 + `SHAREDFILE:MANAGE`/`ROOT_MANAGE` 권한)을 추가했다(Task1).
- `SharedFileRootGuard`: 클라이언트가 전달한 itemId를 그대로 믿지 않고 Drive의 parentIds를 실제로 따라 올라가며 시스템 루트 하위인지 검증한다.
- `SharedFileDrivePort`/`GoogleDriveAdapter`: Drive REST API v3(files.get/list/create/update/export)를 직접 호출하는 하부구조. `upload()`(multipart 업로드)만 실제 호출부(Task4의 `UploadSharedFileUseCase`)가 생기는 시점으로 구현을 미뤘다.
- `SharedFileRootInitializer`: 기존 `GoogleAccountConnectedEvent`를 `AFTER_COMMIT`으로 구독해 시스템 루트를 생성·유지·재생성한다. 같은 계정 재연결 + 기존 루트 READY면 유지하고, 그 외(최초 연결·계정 교체·FAILED 루트)는 Drive에 재생성을 시도한다. Drive 호출이 실패해도 예외를 던지지 않고 FAILED로 저장한다(Google 연결 자체는 이미 커밋된 뒤이므로 영향을 주지 않음).
- Drive 하위 폴더 생성(`createFolder`)과 시스템 루트 자체 생성(`createRootFolder`)을 별도 메서드로 분리했다 — 전자는 항상 부모가 필요하고, 후자는 Drive 최상위에 부모 없이 생성해야 하기 때문이다.

### 계획서와 다르게 처리한 부분

- `InitializeSharedFileRootUseCase` 인터페이스를 별도로 만들지 않았다. Controller가 호출하는 게 아니라 순수 내부 이벤트 리스너라 위임 대상이 `SharedFileRootInitializer` 하나뿐이라 인터페이스를 추가하면 껍데기만 남는 구조였다(YAGNI).
- `SharedFileErrorCode`에 `SHAREDFILE_409_2`(Google 계정 미연결·만료)를 아직 추가하지 않았다. `GetGoogleAccessTokenUseCase`의 예외를 감싸는 시점(Task4)에 추가한다.

### 로컬 실행 시 참고

- `RestClient` 빈이 이 프로젝트에 여러 개(`geminiRestClient`, `googleOAuthRestClient`, `googleDriveRestClient`) 존재해, 생성자 파라미터명을 빈 이름과 정확히 일치시켜야 한다(`@Qualifier` 대신 이름 매칭 컨벤션).
- Google OAuth 콜백은 두 단계로 리다이렉트된다: ① `GOOGLE_REDIRECT_URI`(이 서버의 `/callback`) ② 처리 완료 후 `GOOGLE_OAUTH_FRONTEND_REDIRECT_URI`(프론트엔드). 로컬에서 프론트엔드를 안 띄운 상태로 테스트하면 ②에서 "사이트에 연결할 수 없음"이 뜨지만, 백엔드 처리(연동 저장·루트 생성 시도)는 이미 끝난 상태다.

### 검증

- `SharedFileRootTest`(6), `SharedFileRootGuardTest`(5), `GoogleDriveAdapterTest`(13, `MockRestServiceServer`로 실제 HTTP 요청·응답 검증), `SharedFileRootInitializerTest`(5) — 총 29개, 전부 GREEN.
- `FlywayFreshDatabaseMigrationTest`는 로컬 Docker 미가용으로 스킵했다(계획서에 명시된 검증 경계와 동일).
- 전체 `./gradlew compileJava compileTestJava test` 통과.
