package com.academy.mudogroupware.google.infrastructure.external.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
record GoogleOAuthErrorBody(
        String error,
        @JsonProperty("error_description") String errorDescription) {
}
