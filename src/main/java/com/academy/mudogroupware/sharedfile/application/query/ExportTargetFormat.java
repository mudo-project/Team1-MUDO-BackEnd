package com.academy.mudogroupware.sharedfile.application.query;

// 다운로드 요청의 변환 대상 형식. 원본 유형(Docs/Sheets/Slides)과 조합해 GoogleWorkspaceExportFormat을 결정한다.
public enum ExportTargetFormat {
    PDF,
    DOCX,
    XLSX,
    PPTX
}
