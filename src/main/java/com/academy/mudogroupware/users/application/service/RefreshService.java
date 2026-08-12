package com.academy.mudogroupware.users.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.auth.application.usecase.RefreshTokenValidatorUseCase;
import com.academy.mudogroupware.auth.application.usecase.TokenIssuerUseCase;
import com.academy.mudogroupware.global.domain.auth.RefreshTokenClaims;
import com.academy.mudogroupware.users.application.command.RefreshCommand;
import com.academy.mudogroupware.users.application.usecase.RefreshUseCase;
import com.academy.mudogroupware.users.domain.exception.UserErrorCode;
import com.academy.mudogroupware.users.domain.exception.UserException;
import com.academy.mudogroupware.users.domain.model.User;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefreshService implements RefreshUseCase {

    private final RefreshTokenValidatorUseCase refreshTokenValidatorUseCase;
    private final UserRepository userRepository;
    private final TokenIssuerUseCase tokenIssuerUseCase;

    @Override
    public String refresh(RefreshCommand command) {
        log.info("event=auth_token_reissue_시작");
        try {
            String refreshToken = command.refreshToken();
            if (refreshToken == null || refreshToken.isBlank()) {
                throw new UserException(UserErrorCode.REFRESH_TOKEN_NOT_FOUND);
            }

            RefreshTokenClaims claims = refreshTokenValidatorUseCase.validateStored(refreshToken);

            User user = userRepository.findById(claims.userId())
                    .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

            user.ensureLoginAllowed();

            String accessToken = tokenIssuerUseCase.issueAccessToken(user.getId(), user.getUsername(),
                    user.getRoleId(), user.getAccountType(), user.getAdminScope());
            log.info("event=auth_token_reissue_완료 userId={}", user.getId());
            return accessToken;
        } catch (RuntimeException e) {
            log.warn("event=auth_token_reissue_실패 reason={}", e.getMessage(), e);
            throw e;
        }
    }
}
