package com.academy.mudogroupware.google.application.port;

import java.util.Set;

/**
 * 현재 설정에서 요구되는 구글 OAuth scope 집합을 제공한다. Application 계층은 이 Port에만
 * 의존하고, Infrastructure의 OAuth 설정 구현 세부사항(클라이언트 시크릿 등)을 직접 참조하지 않는다.
 */
public interface RequiredGoogleScopePort {

    Set<String> requiredScopes();
}
