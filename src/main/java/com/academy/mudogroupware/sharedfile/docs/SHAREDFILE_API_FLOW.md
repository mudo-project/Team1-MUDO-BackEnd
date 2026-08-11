# SHAREDFILE_API_FLOW.md

## 목적

공유파일 10개 API 각각의 `Controller → UseCase → Service → Domain → Port/Adapter` 호출 흐름을 정리한다. `AGENTS.md`가 정한 표준 호출 흐름 설명 방식을 그대로 따른다.

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
  → GetSharedFileRootUseCase(🚧) / GetSharedFileRootService(🚧)
    → SharedFileRootRepository.find() ✅
```

### 2. POST /api/shared-files/root/recreation — 시스템 루트 재생성

```text
SharedFileController(🚧)
  → RecreateSharedFileRootUseCase(🚧) / RecreateSharedFileRootService(🚧)
    → SharedFileRootRepository.find() ✅ (FAILED 상태 확인)
    → GetGoogleAccessTokenUseCase.getAccessToken() ✅(google 도메인)
    → SharedFileDrivePort.createRootFolder() ✅
    → SharedFileRootRepository.save() ✅
```

### 3. GET /api/shared-files/items — 현재 폴더 목록

```text
SharedFileController(🚧)
  → ListSharedFileItemsUseCase(🚧) / ListSharedFileItemsService(🚧)
    → SharedFileRootRepository.find() ✅ (READY 아니면 SharedFileRootUnavailableException ✅)
    → parentId 없으면 루트 ID를 기본값으로 사용
    → GetGoogleAccessTokenUseCase.getAccessToken() ✅
    → SharedFileDrivePort.listChildren() ✅
```

### 4. GET /api/shared-files/items/{itemId} — 파일·폴더 상세 조회

```text
SharedFileController(🚧)
  → GetSharedFileItemUseCase(🚧) / GetSharedFileItemService(🚧)
    → GetGoogleAccessTokenUseCase.getAccessToken() ✅
    → SharedFileRootGuard.requireDescendant() ✅
    → SharedFileDrivePort.getItem() ✅
```

### 5. GET /api/shared-files/items/search — 시스템 루트 전체 검색

```text
SharedFileController(🚧)
  → SearchSharedFileItemsUseCase(🚧) / SearchSharedFileItemsService(🚧)
    → GetGoogleAccessTokenUseCase.getAccessToken() ✅
    → SharedFileDrivePort.searchByName() ✅
    → 후보마다 SharedFileRootGuard.requireDescendant() ✅ 적용해 루트 밖 결과 제외
```

### 6. POST /api/shared-files/folders — 하위 폴더 생성

```text
SharedFileController(🚧)
  → CreateSharedFolderUseCase(🚧) / CreateSharedFolderService(🚧)
    → 이름 검증 실패 시 SharedFileInvalidNameException ✅
    → GetGoogleAccessTokenUseCase.getAccessToken() ✅
    → SharedFileRootGuard.requireDescendant() ✅ (parentId)
    → SharedFileDrivePort.createFolder() ✅
```

### 7. POST /api/shared-files/items/upload — 로컬 파일 업로드

```text
SharedFileController(🚧)
  → UploadSharedFileUseCase(🚧) / UploadSharedFileService(🚧)
    → 100MB 초과 시 SharedFileUploadTooLargeException ✅
    → GetGoogleAccessTokenUseCase.getAccessToken() ✅
    → SharedFileRootGuard.requireDescendant() ✅ (parentId)
    → SharedFileDrivePort.upload() 🚧 (Adapter의 multipart 실호출 자체가 미구현 — 이 UseCase 작업 시 TDD로 구현)
```

### 8. POST /api/shared-files/google-files — Google 파일 생성

```text
SharedFileController(🚧)
  → CreateGoogleWorkspaceFileUseCase(🚧) / CreateGoogleWorkspaceFileService(🚧)
    → GetGoogleAccessTokenUseCase.getAccessToken() ✅
    → SharedFileRootGuard.requireDescendant() ✅ (parentId)
    → SharedFileDrivePort.createWorkspaceFile(GoogleWorkspaceFileType) ✅
```

### 9. PATCH /api/shared-files/items/{itemId} — 이름 변경·이동

```text
SharedFileController(🚧)
  → RenameSharedFileItemUseCase(🚧) / MoveSharedFileItemUseCase(🚧)
    → SharedFileRootGuard.requireDescendant() ✅ (대상, 이동 시 목적지 parentId도 함께 검증)
    → SharedFileDrivePort.rename() ✅ / SharedFileDrivePort.move() ✅
```

### 10. DELETE /api/shared-files/items/{itemId} — 휴지통 삭제

```text
SharedFileController(🚧)
  → TrashSharedFileItemUseCase(🚧) / TrashSharedFileItemService(🚧)
    → SharedFileRootGuard.requireDescendant() ✅
    → SharedFileDrivePort.trash() ✅
```

### 11. GET /api/shared-files/items/{itemId}/download — 원본·변환 다운로드 (신설)

> 설계서(`2026-08-10-sharedfile-design.md`)의 10개 API 목록에는 다운로드 전용 엔드포인트가 없었다. `FUNCTIONAL_SPEC.md`와 Task5 계획서는 다운로드를 별도 기능으로 다루고 있어, 상세조회(4번) 응답과 분리된 11번째 엔드포인트를 신설하기로 결정했다(2026-08-11).

```text
SharedFileController(🚧)
  → DownloadSharedFileUseCase(🚧) / DownloadSharedFileService(🚧)
    → GetGoogleAccessTokenUseCase.getAccessToken() ✅
    → SharedFileRootGuard.requireDescendant() ✅
    → format 쿼리 파라미터 없음 → SharedFileDrivePort.downloadOriginal() ✅
    → format 쿼리 파라미터 있음 → GoogleWorkspaceExportFormat 매핑 후 SharedFileDrivePort.export() ✅
      (매핑 실패 시 SharedFileInvalidExportFormatException ✅)
```

## 다음 갱신 시점

- Task4(3·4·5·6·7·8) 구현 완료 시 해당 UseCase/Service 실제 클래스명과 🚧→✅ 갱신
- Task5(9·10, 그리고 다운로드는 API 목록에 없지만 4번 상세조회 응답의 다운로드 URL/변환 다운로드 흐름으로 별도 절 추가 검토) 구현 완료 시 갱신
- Task6(Controller) 구현 완료 시 모든 `SharedFileController(🚧)`를 실제 메서드명으로 교체
