package com.academy.mudogroupware.google.application.port;

public class GoogleOAuthCallException extends RuntimeException {

    public GoogleOAuthCallException(String message, Throwable cause) {
        super(message, cause);
    }

    public GoogleOAuthCallException(String message) {
        super(message);
    }
}
