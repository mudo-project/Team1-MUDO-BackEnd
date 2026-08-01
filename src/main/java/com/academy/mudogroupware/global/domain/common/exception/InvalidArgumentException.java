package com.academy.mudogroupware.global.domain.common.exception;
public class InvalidArgumentException extends BusinessException { public InvalidArgumentException(){super(CommonErrorCode.INVALID_ARGUMENT);} public InvalidArgumentException(String m){super(CommonErrorCode.INVALID_ARGUMENT,m);} }
