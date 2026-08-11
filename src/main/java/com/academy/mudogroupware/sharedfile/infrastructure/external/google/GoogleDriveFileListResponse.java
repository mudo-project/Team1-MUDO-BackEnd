package com.academy.mudogroupware.sharedfile.infrastructure.external.google;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// files.list 응답 DTO. nextPageToken은 그대로 DrivePage.nextCursor로 전달된다.
@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleDriveFileListResponse(List<GoogleDriveFileResponse> files, String nextPageToken) {
}
