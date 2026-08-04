package com.academy.mudogroupware.users.application.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.auth.application.result.TokenPair;
import com.academy.mudogroupware.auth.application.usecase.TokenIssuerUseCase;
import com.academy.mudogroupware.users.application.command.LoginCommand;
import com.academy.mudogroupware.users.application.usecase.LoginUseCase;
import com.academy.mudogroupware.users.domain.exception.UserErrorCode;
import com.academy.mudogroupware.users.domain.exception.UserException;
import com.academy.mudogroupware.users.domain.model.User;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class LoginService implements LoginUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenIssuerUseCase tokenIssuerUseCase;

    @Override
    public TokenPair login(LoginCommand command) {
        User user = userRepository.findByUsername(command.username())
                .orElseThrow(() -> new UserException(UserErrorCode.LOGIN_FAILED));

        if (!passwordEncoder.matches(command.password(), user.getPassword())) {
            throw new UserException(UserErrorCode.LOGIN_FAILED);
        }

        user.ensureLoginAllowed();

        return tokenIssuerUseCase.issue(user.getId(), user.getUsername(), user.getRole());
    }
}
