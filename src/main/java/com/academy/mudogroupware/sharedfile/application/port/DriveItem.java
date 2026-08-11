package com.academy.mudogroupware.sharedfile.application.port;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// Drive의 파일·폴더 메타데이터를 감싼 값 객체. parentIds는 SharedFileRootGuard가 루트까지 경로를
// 추적하는 데 쓰이고, downloadable은 Drive가 미리보기를 지원하지 않는 형식을 프론트에 안내할 때 쓰인다.
public record DriveItem(
        String id,
        String name,
        String mimeType,
        List<String> parentIds,
        String viewUrl,
        boolean downloadable,
        LocalDateTime modifiedAt,
        boolean trashed
) {
    private static final String FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";

    // parentIds를 불변 스냅샷으로 저장한다. Jackson이 만드는 List는 가변이라, 방어적 복사가
    // 없으면 나중에 외부에서 원본이 변경될 때 SharedFileRootGuard의 경로 판정이 흔들릴 수 있다.
    public DriveItem {
        parentIds = List.copyOf(parentIds);
    }

    public boolean isFolder() {
        return FOLDER_MIME_TYPE.equals(mimeType);
    }

    // Docs/Sheets/Slides면 해당 유형을, 그 외(폴더·일반 업로드 파일)면 빈 값을 돌려준다. 이름 변경 규칙과
    // 다운로드 변환 형식 결정이 Google MIME type 문자열을 직접 비교하지 않고 이 메서드 하나로 판단하게 한다.
    public Optional<GoogleWorkspaceFileType> workspaceType() {
        if (mimeType == null) {
            return Optional.empty();
        }
        return switch (mimeType) {
            case "application/vnd.google-apps.document" -> Optional.of(GoogleWorkspaceFileType.DOCS);
            case "application/vnd.google-apps.spreadsheet" -> Optional.of(GoogleWorkspaceFileType.SHEETS);
            case "application/vnd.google-apps.presentation" -> Optional.of(GoogleWorkspaceFileType.SLIDES);
            default -> Optional.empty();
        };
    }

    // 폴더도 Google Workspace 파일도 아닌, 사용자가 직접 업로드한 원본 파일.
    public boolean isRegularFile() {
        return !isFolder() && workspaceType().isEmpty();
    }
}
