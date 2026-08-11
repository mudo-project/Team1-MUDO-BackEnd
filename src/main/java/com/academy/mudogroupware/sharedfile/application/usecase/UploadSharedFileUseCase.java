package com.academy.mudogroupware.sharedfile.application.usecase;

import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemView;

public interface UploadSharedFileUseCase {

    SharedFileItemView upload(String parentId, String filename, String contentType, long size, byte[] content);
}
