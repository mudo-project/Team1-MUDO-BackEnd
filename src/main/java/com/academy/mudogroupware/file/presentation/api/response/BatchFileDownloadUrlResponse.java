package com.academy.mudogroupware.file.presentation.api.response;

import java.util.Map;

public record BatchFileDownloadUrlResponse(
        Map<Long, String> downloadUrls
) {

    public static BatchFileDownloadUrlResponse from(Map<Long, String> downloadUrls) {
        return new BatchFileDownloadUrlResponse(downloadUrls);
    }
}
