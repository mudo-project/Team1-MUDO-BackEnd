package com.academy.mudogroupware.users.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.academy.mudogroupware.auth.application.result.TokenPair;
import com.academy.mudogroupware.auth.application.usecase.TokenIssuerUseCase;
import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.users.application.command.LoginCommand;
import com.academy.mudogroupware.users.application.result.LoginResult;
import com.academy.mudogroupware.users.domain.exception.UserErrorCode;
import com.academy.mudogroupware.users.domain.exception.UserException;
import com.academy.mudogroupware.users.domain.model.User;
import com.academy.mudogroupware.users.domain.model.UserStatus;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

class LoginServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final TokenIssuerUseCase tokenIssuerUseCase = mock(TokenIssuerUseCase.class);
    private final LoginService service = new LoginService(userRepository, passwordEncoder, tokenIssuerUseCase);

    private User user(boolean mustChangePw, UserStatus status) {
        LocalDateTime now = LocalDateTime.now();
        return User.restore(1L, "teacher01", "hashed", "김강사", "010-0000-0000",
                "teacher01@example.com", 5L, status, mustChangePw, AccountType.MEMBER, null, now, now, now);
    }

    @Test
    void loginReturnsMustChangePwTrueWhenPasswordNotYetSet() {
        when(userRepository.findByUsername("teacher01")).thenReturn(java.util.Optional.of(user(true, UserStatus.ACTIVE)));
        when(passwordEncoder.matches("temp-pass", "hashed")).thenReturn(true);
        when(tokenIssuerUseCase.issue(eq(1L), eq("teacher01"), eq(5L), eq(AccountType.MEMBER), any(), eq(true)))
                .thenReturn(new TokenPair("access-token", "refresh-token"));

        LoginResult result = service.login(new LoginCommand("teacher01", "temp-pass"));

        assertThat(result.mustChangePw()).isTrue();
        assertThat(result.tokenPair().accessToken()).isEqualTo("access-token");
        assertThat(result.tokenPair().refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void loginReturnsMustChangePwFalseWhenPasswordAlreadySet() {
        when(userRepository.findByUsername("teacher01")).thenReturn(java.util.Optional.of(user(false, UserStatus.ACTIVE)));
        when(passwordEncoder.matches("real-pass", "hashed")).thenReturn(true);
        when(tokenIssuerUseCase.issue(eq(1L), eq("teacher01"), eq(5L), eq(AccountType.MEMBER), any(), eq(false)))
                .thenReturn(new TokenPair("access-token", "refresh-token"));

        LoginResult result = service.login(new LoginCommand("teacher01", "real-pass"));

        assertThat(result.mustChangePw()).isFalse();
        assertThat(result.tokenPair().accessToken()).isEqualTo("access-token");
        assertThat(result.tokenPair().refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void loginThrowsWhenUsernameNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginCommand("unknown", "any-pass")))
                .isInstanceOf(UserException.class)
                .extracting(e -> ((UserException) e).getErrorCode())
                .isEqualTo(UserErrorCode.LOGIN_FAILED);
    }

    @Test
    void loginThrowsWhenPasswordDoesNotMatch() {
        when(userRepository.findByUsername("teacher01")).thenReturn(java.util.Optional.of(user(false, UserStatus.ACTIVE)));
        when(passwordEncoder.matches("wrong-pass", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginCommand("teacher01", "wrong-pass")))
                .isInstanceOf(UserException.class)
                .extracting(e -> ((UserException) e).getErrorCode())
                .isEqualTo(UserErrorCode.LOGIN_FAILED);
    }

    @Test
    void loginThrowsWhenAccountIsNotActive() {
        when(userRepository.findByUsername("teacher01"))
                .thenReturn(java.util.Optional.of(user(false, UserStatus.RESIGNED)));
        when(passwordEncoder.matches("real-pass", "hashed")).thenReturn(true);

        assertThatThrownBy(() -> service.login(new LoginCommand("teacher01", "real-pass")))
                .isInstanceOf(UserException.class)
                .extracting(e -> ((UserException) e).getErrorCode())
                .isEqualTo(UserErrorCode.LOGIN_RESTRICTED);
    }
}
