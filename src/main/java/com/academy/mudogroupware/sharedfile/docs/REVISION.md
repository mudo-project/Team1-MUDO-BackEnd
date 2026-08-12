# 🔄 공유파일 도메인 변경 이력

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
