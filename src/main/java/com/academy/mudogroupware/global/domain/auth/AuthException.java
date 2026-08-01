package com.academy.mudogroupware.global.domain.auth;
import com.academy.mudogroupware.global.domain.common.exception.BusinessException;
public class AuthException extends BusinessException { public AuthException(AuthErrorCode c){super(c);} public AuthException(AuthErrorCode c,Throwable t){super(c,c.getMessage(),t);} }
