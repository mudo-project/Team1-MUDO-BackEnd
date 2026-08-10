package com.academy.mudogroupware.google.application.usecase;

/**
 * 학원의 구글 연동 계정으로 유효한 액세스 토큰을 발급받는다. 다른 도메인(예: 템플릿 기능)이
 * Drive/Docs/Sheets API를 직접 호출할 때 이 UseCase를 사용한다. 이 UseCase는 API 엔드포인트로
 * 노출되지 않으며, 다른 도메인의 코드가 직접 호출하는 내부 공개 계약이다.
 */
public interface GetGoogleAccessTokenUseCase {

    /**
     * @throws com.academy.mudogroupware.google.domain.exception.GoogleAccountNotConnectedException 연동이 없는 경우
     * @throws com.academy.mudogroupware.google.domain.exception.GoogleAccountConnectionInvalidException
     *         연동이 만료됐거나 필요한 scope가 부족해 재연결이 필요한 경우
     */
    String getAccessToken();
}
