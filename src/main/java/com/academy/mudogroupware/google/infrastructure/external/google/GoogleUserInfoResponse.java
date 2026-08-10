package com.academy.mudogroupware.google.infrastructure.external.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record GoogleUserInfoResponse(String email) {
}
