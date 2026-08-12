# SHAREDFILE_API_FLOW.md

## 목적

공유파일 11개 API 각각의 `Controller → UseCase → Service → Domain → Port/Adapter` 호출 흐름을 정리한다. `AGENTS.md`가 정한 표준 호출 흐름 설명 방식을 그대로 따른다.

- 이 문서는 **구현 진행 중 계속 갱신**한다. Task4/5/6이 끝날 때마다 해당 API 행의 구현 상태와 클래스명을 실제 코드에 맞춰 고친다.
- 요청·응답 DTO, HTTP 상태 코드 같은 API 계약 상세는 다루지 않는다. 그건 Task6 완료 후 `SHAREDFILE_API.md`(신규)가 담당한다.
- 구현 상태 표기: ✅ 구현됨 · 🚧 계획(클래스명 미확정 가능) · — 해당 없음

## 공통 하부구조 (Task1~3에서 이미 구현됨)

모든 API가 공유하는 조각이라 API별 표에서는 반복하지 않는다.

```text
GetGoogleAccessTokenUseCase(google 도메인)  → Drive 접근 토큰 발급
SharedFileRootGuard                          → 대상이 시스템 루트 하위인지 Drive parentIds로 검증
SharedFileDrivePort → GoogleDriveAdapter      → Drive REST API v3 실호출
SharedFileRootRepository → SharedFileRootPersistenceAdapter → shared_file_root 조회
```

## API별 호출 흐름

### 1. GET /api/shared-files/root — 시스템 루트 상태 조회

```text
SharedFileController(🚧)
  → GetSharedFileRootUseCase(✅) / GetSharedFileRootService(✅)
    → SharedFileRootRepository.find() ✅
```

### 2. POST /api/shared-files/root/recreation — 시스템 루트 재생성

> **미해결 갭**: 계획서 Task1~6 어디에도 이 API를 담당하는 UseCase가 명시돼 있지 않다. Task2의 `SharedFileRootInitializer`는 이벤트 리스너라 Controller가 직접 호출하는 구조가 아니다. Task6(Controller) 작업 시 `RecreateSharedFileRootUseCase`를 신설할지, `SharedFileRootInitializer`의 재생성 로직을 재사용 가능한 형태로 뽑아낼지 결정 필요.

```text
SharedFileController(🚧)
  → RecreateSharedFileRootUseCase(🚧, 미착수) / RecreateSharedFileRootService(🚧, 미착수)
    → SharedFileRootRepository.find() ✅ (FAILED 상태 확인)
    → GetGoogleAccessTokenUseCase.getAccessToken() ✅(google 도메인)
    → SharedFileDrivePort.createRootFolder() ✅
    → SharedFileRootRepository.save() ✅
```

### 3. GET /api/shared-files/items — 현재 폴더 목록

```text
SharedFileController(🚧)
  → ListSharedFileItemsUseCase(✅) / ListSharedFileItemsService(✅)
    → SharedFileRootRepository.find() ✅ (READY 아니면 SharedFileRootUnavailableException ✅)
    → parentId 없으면 루트 ID를 기본값으로 사용, 있으면 SharedFileRootGuard.requireDescendant() ✅
    → GetGoogleAccessTokenUseCase.getAccessToken() ✅
    → SharedFileDrivePort.listChildren() ✅
```

### 4. GET /api/shared-files/items/{itemId} — 파일·폴더 상세 조회

```text
SharedFileController(🚧)
  → GetSharedFileItemUseCase(✅) / GetSharedFileItemService(✅)
    → SharedFileRootRepository.find() ✅ (READY 확인)
    → GetGoogleAccessTokenUseCase.getAccessToken() ✅
    → SharedFileRootGuard.requireDescendant() ✅
    → SharedFileDrivePort.getItem() ✅
```

### 5. GET /api/shared-files/items/search — 시스템 루트 전체 검색

```text
SharedFileController(🚧)
  → SearchSharedFileItemsUseCase(✅) / SearchSharedFileItemsService(✅)
    → SharedFileRootRepository.find() ✅ (READY 확인)
    → GetGoogleAccessTokenUseCase.getAccessToken() ✅
    → SharedFileDrivePort.searchByName() ✅
    → 후보마다 SharedFileItemType(FILE/FOLDER) 필터 → SharedFileRootGuard.requireDescendant() ✅ 적용해 루트 밖 결과 제외
```

### 6. POST /api/shared-files/folders — 하위 폴더 생성

```text
SharedFileController(🚧)
  → CreateSharedFolderUseCase(✅) / CreateSharedFolderService(✅)
    → 이름 검증 실패 시 SharedFileInvalidNameException ✅
    → SharedFileRootRepository.find() ✅ (READY 확인)
    → GetGoogleAccessTokenUseCase.getAccessToken() ✅
    → SharedFileRootGuard.requireDescendant() ✅ (parentId, 루트 자신이면 생략)
    → SharedFileDrivePort.createFolder() ✅
```

### 7. POST /api/shared-files/items/upload — 로컬 파일 업로드

```text
SharedFileController(🚧)
  → UploadSharedFileUseCase(✅) / UploadSharedFileService(✅)
    → 100MB 초과 시 SharedFileUploadTooLargeException ✅
    → 이름 검증 실패 시 SharedFileInvalidNameException ✅
    → SharedFileRootRepository.find() ✅ (READY 확인)
    → GetGoogleAccessTokenUseCase.getAccessToken() ✅
    → SharedFileRootGuard.requireDescendant() ✅ (parentId, 루트 자신이면 생략)
    → SharedFileDrivePort.upload() ✅ (GoogleDriveAdapter가 multipart/related 본문을 직접 구성해 업로드)
```

### 8. POST /api/shared-files/google-files — Google 파일 생성

```text
SharedFileController(🚧)
  → CreateGoogleWorkspaceFileUseCase(✅) / CreateGoogleWorkspaceFileService(✅)
    → 이름 검증 실패 시 SharedFileInvalidNameException ✅
    → SharedFileRootRepository.find() ✅ (READY 확인)
    → GetGoogleAccessTokenUseCase.getAccessToken() ✅
    → SharedFileRootGuard.requireDescendant() ✅ (parentId, 루트 자신이면 생략)
    → SharedFileDrivePort.createWorkspaceFile(GoogleWorkspaceFileType) ✅
```

### 9. PATCH /api/shared-files/items/{itemId} — 이름 변경·이동

```text
SharedFileController(🚧)
  → RenameSharedFileItemUseCase(✅) / RenameSharedFileItemService(✅)
    → SharedFileRootRepository.find() ✅ (READY 확인)
    → GetGoogleAccessTokenUseCase.getAccessToken() ✅
    → SharedFileRootGuard.requireDescendant() ✅ (대상)
    → SharedFileDrivePort.getItem() ✅ (일반 업로드 파일이면 확장자 동일성 검사, 다르면 SharedFileInvalidNameException ✅)
    → SharedFileDrivePort.rename() ✅

  → MoveSharedFileItemUseCase(✅) / MoveSharedFileItemService(✅)
    → SharedFileRootGuard.requireDescendant() ✅ (대상, 목적지가 루트 자신이 아니면 목적지도 검증)
    → SharedFileDrivePort.getItem() ✅ (현재 parentId 확인 — PATCH 요청엔 새 parentId만 오므로 직접 조회)
    → SharedFileDrivePort.move() ✅
```

### 10. DELETE /api/shared-files/items/{itemId} — 휴지통 삭제

```text
SharedFileController(🚧)
  → TrashSharedFileItemUseCase(✅) / TrashSharedFileItemService(✅)
    → SharedFileRootRepository.find() ✅ (READY 확인)
    → GetGoogleAccessTokenUseCase.getAccessToken() ✅
    → SharedFileRootGuard.requireDescendant() ✅
    → SharedFileDrivePort.trash() ✅
```

### 11. GET /api/shared-files/items/{itemId}/download — 원본·변환 다운로드 (신설)

> 설계서(`2026-08-10-sharedfile-design.md`)의 10개 API 목록에는 다운로드 전용 엔드포인트가 없었다. `FUNCTIONAL_SPEC.md`와 Task5 계획서는 다운로드를 별도 기능으로 다루고 있어, 상세조회(4번) 응답과 분리된 11번째 엔드포인트를 신설하기로 결정했다(2026-08-11).

```text
SharedFileController(🚧)
  → DownloadSharedFileUseCase(✅) / DownloadSharedFileService(✅)
    → SharedFileRootRepository.find() ✅ (READY 확인)
    → GetGoogleAccessTokenUseCase.getAccessToken() ✅
    → SharedFileRootGuard.requireDescendant() ✅
    → format(ExportTargetFormat) 없음 → SharedFileDrivePort.downloadOriginal() ✅
    → format 있음 → SharedFileDrivePort.getItem() ✅ → DriveItem.workspaceType() ✅으로 원본 유형 확인
      → GoogleWorkspaceExportFormat.valueOf(유형_format) 매핑 → SharedFileDrivePort.export() ✅
      (일반 파일에 format 요청, 또는 유형과 안 맞는 조합이면 SharedFileInvalidExportFormatException ✅)
```

## 다음 갱신 시점

- ~~Task4(3·4·5·6·7·8) 구현 완료 시 해당 UseCase/Service 실제 클래스명과 🚧→✅ 갱신~~ 완료(2026-08-11)
- ~~Task5(9·10·11) 구현 완료 시 갱신~~ 완료(2026-08-11)
- Task6(Controller) 구현 완료 시 모든 `SharedFileController(🚧)`를 실제 메서드명으로 교체, 2번(루트 재생성) UseCase 설계 결정
