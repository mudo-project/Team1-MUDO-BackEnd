package com.academy.mudogroupware.google.infrastructure.external.google;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.academy.mudogroupware.google.application.port.GoogleOAuthCallException;
import com.academy.mudogroupware.google.application.port.GoogleOAuthPort;
import com.academy.mudogroupware.google.application.port.GoogleTokenExchangeResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleOAuthAdapter implements GoogleOAuthPort {

    private static final String AUTHORIZATION_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String REVOKE_ENDPOINT = "https://oauth2.googleapis.com/revoke";
    private static final String USERINFO_ENDPOINT = "https://www.googleapis.com/oauth2/v3/userinfo";

    private final RestClient googleOAuthRestClient;
    private final GoogleOAuthProperties googleOAuthProperties;

    @Override
    public String buildAuthorizationUrl(String state, boolean forceAccountSelection) {
        String prompt = forceAccountSelection ? "select_account consent" : "consent";
        return AUTHORIZATION_ENDPOINT
                + "?client_id=" + encode(googleOAuthProperties.clientId())
                + "&redirect_uri=" + encode(googleOAuthProperties.redirectUri())
                + "&response_type=code"
                + "&access_type=offline"
                + "&include_granted_scopes=true"
                + "&prompt=" + encode(prompt)
                + "&scope=" + encode(googleOAuthProperties.scope())
                + "&state=" + encode(state);
    }

    @Override
    public GoogleTokenExchangeResult exchangeAuthorizationCode(String authorizationCode) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", authorizationCode);
        body.add("client_id", googleOAuthProperties.clientId());
        body.add("client_secret", googleOAuthProperties.clientSecret());
        body.add("redirect_uri", googleOAuthProperties.redirectUri());
        body.add("grant_type", "authorization_code");

        GoogleTokenResponse response = requestToken(body);
        return new GoogleTokenExchangeResult(response.accessToken(), response.refreshToken(), response.scope());
    }

    @Override
    public GoogleTokenExchangeResult refreshAccessToken(String refreshToken) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("refresh_token", refreshToken);
        body.add("client_id", googleOAuthProperties.clientId());
        body.add("client_secret", googleOAuthProperties.clientSecret());
        body.add("grant_type", "refresh_token");

        GoogleTokenResponse response = requestToken(body);
        return new GoogleTokenExchangeResult(response.accessToken(), response.refreshToken(), response.scope());
    }

    @Override
    public String fetchAccountEmail(String accessToken) {
        try {
            GoogleUserInfoResponse response = googleOAuthRestClient.get()
                    .uri(USERINFO_ENDPOINT)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(GoogleUserInfoResponse.class);
            if (response == null || response.email() == null || response.email().isBlank()) {
                throw new GoogleOAuthCallException("구글 사용자 정보 응답에 이메일이 없습니다.");
            }
            return response.email();
        } catch (RestClientException e) {
            throw new GoogleOAuthCallException("구글 사용자 정보 조회에 실패했습니다.", e);
        }
    }

    @Override
    public void revoke(String token) {
        try {
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("token", token);
            googleOAuthRestClient.post()
                    .uri(REVOKE_ENDPOINT)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("event=google_token_revoke_failed message={}", e.getMessage());
        }
    }

    private GoogleTokenResponse requestToken(MultiValueMap<String, String> body) {
        try {
            GoogleTokenResponse response = googleOAuthRestClient.post()
                    .uri(TOKEN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(GoogleTokenResponse.class);
            if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
                throw new GoogleOAuthCallException("구글 토큰 응답에 액세스 토큰이 없습니다.");
            }
            return response;
        } catch (RestClientException e) {
            throw new GoogleOAuthCallException("구글 토큰 발급에 실패했습니다.", e);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
