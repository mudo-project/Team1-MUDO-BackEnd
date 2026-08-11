package com.academy.mudogroupware.sharedfile.infrastructure.external.google;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// files.get/files.list/files.create/files.update 응답을 매핑하는 DTO. GoogleDriveAdapter.FILE_FIELDS로
// 요청한 필드만 채워지며, GoogleDriveAdapter.toDriveItem()이 도메인 값 객체로 변환한다.
@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleDriveFileResponse(
        String id,
        String name,
        String mimeType,
        List<String> parents,
        String webViewLink,
        String modifiedTime,
        Boolean trashed,
        Capabilities capabilities
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Capabilities(Boolean canDownload) {
    }
}
