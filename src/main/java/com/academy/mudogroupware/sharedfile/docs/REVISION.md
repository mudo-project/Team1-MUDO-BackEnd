# 🔄 공유파일 도메인 변경 이력

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
