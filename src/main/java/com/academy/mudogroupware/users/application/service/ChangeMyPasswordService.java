package com.academy.mudogroupware.users.application.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.users.application.usecase.ChangeMyPasswordUseCase;
import com.academy.mudogroupware.users.domain.exception.UserErrorCode;
import com.academy.mudogroupware.users.domain.exception.UserException;
import com.academy.mudogroupware.users.domain.model.User;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ChangeMyPasswordService implements ChangeMyPasswordUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .filter(u -> passwordEncoder.matches(currentPassword, u.getPassword()))
                .orElseThrow(() -> new UserException(UserErrorCode.CURRENT_PASSWORD_MISMATCH));
        userRepository.changePassword(user.getId(), passwordEncoder.encode(newPassword));
    }
}
