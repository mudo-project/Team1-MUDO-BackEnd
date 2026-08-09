package com.academy.mudogroupware.file.presentation.api.response;

public record FileDownloadUrlResponse(
        String downloadUrl
) {

    public static FileDownloadUrlResponse from(String downloadUrl) {
        return new FileDownloadUrlResponse(downloadUrl);
    }
}
