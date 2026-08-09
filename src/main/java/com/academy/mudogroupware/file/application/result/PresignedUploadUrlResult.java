package com.academy.mudogroupware.file.application.result;

public record PresignedUploadUrlResult(
        String objectKey,
        String uploadUrl
) {
}
