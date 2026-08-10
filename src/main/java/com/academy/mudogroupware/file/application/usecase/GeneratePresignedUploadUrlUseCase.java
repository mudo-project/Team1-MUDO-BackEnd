package com.academy.mudogroupware.file.application.usecase;

import com.academy.mudogroupware.file.application.command.GeneratePresignedUploadUrlCommand;
import com.academy.mudogroupware.file.application.result.PresignedUploadUrlResult;

public interface GeneratePresignedUploadUrlUseCase {

    PresignedUploadUrlResult generate(GeneratePresignedUploadUrlCommand command);
}
