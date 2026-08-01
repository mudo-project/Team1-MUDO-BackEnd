package com.academy.mudogroupware.file.infrastructure.s3;
import com.academy.mudogroupware.global.domain.common.exception.*;
public class S3StorageException extends InfrastructureException { public S3StorageException(Throwable c){super(CommonErrorCode.INTERNAL_SERVER_ERROR,"파일 저장소 처리에 실패했습니다.",c);} }
