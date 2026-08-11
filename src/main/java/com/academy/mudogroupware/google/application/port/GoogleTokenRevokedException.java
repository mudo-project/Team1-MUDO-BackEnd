package com.academy.mudogroupware.google.application.port;

public class GoogleTokenRevokedException extends GoogleOAuthCallException {

    public GoogleTokenRevokedException(String message, Throwable cause) {
        super(message, cause);
    }
}
