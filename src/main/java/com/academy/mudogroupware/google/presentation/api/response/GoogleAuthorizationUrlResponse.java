package com.academy.mudogroupware.google.presentation.api.response;

public record GoogleAuthorizationUrlResponse(String authorizationUrl) {

    public static GoogleAuthorizationUrlResponse from(String authorizationUrl) {
        return new GoogleAuthorizationUrlResponse(authorizationUrl);
    }
}
