package com.academy.mudogroupware.global.domain.common.exception;
import org.springframework.http.HttpStatus;
public interface ErrorCode { String name(); HttpStatus getHttpStatus(); String getCode(); String getMessage(); }
