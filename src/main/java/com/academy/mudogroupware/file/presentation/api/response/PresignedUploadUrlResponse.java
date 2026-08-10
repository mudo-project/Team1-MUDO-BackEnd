package com.academy.mudogroupware.file.presentation.api.response;

import com.academy.mudogroupware.file.application.result.PresignedUploadUrlResult;

public record PresignedUploadUrlResponse(
        String objectKey,
        String uploadUrl
) {

    public static PresignedUploadUrlResponse from(PresignedUploadUrlResult result) {
        return new PresignedUploadUrlResponse(result.objectKey(), result.uploadUrl());
    }
}
