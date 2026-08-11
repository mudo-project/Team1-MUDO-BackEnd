package com.academy.mudogroupware.sharedfile.application.port;

// Google Workspace 파일을 직접 다운로드할 때 선택 가능한 6가지 변환 형식.
// 이 6개 외의 요청은 SharedFileInvalidExportFormatException으로 거부한다(Task5).
public enum GoogleWorkspaceExportFormat {

    DOCS_PDF("application/pdf", "pdf"),
    DOCS_DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"),
    SHEETS_PDF("application/pdf", "pdf"),
    SHEETS_XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
    SLIDES_PDF("application/pdf", "pdf"),
    SLIDES_PPTX("application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx");

    private final String exportMimeType;
    private final String extension;

    GoogleWorkspaceExportFormat(String exportMimeType, String extension) {
        this.exportMimeType = exportMimeType;
        this.extension = extension;
    }

    public String getExportMimeType() {
        return exportMimeType;
    }

    public String getExtension() {
        return extension;
    }
}
