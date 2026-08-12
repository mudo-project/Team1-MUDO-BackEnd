package com.academy.mudogroupware.users.application.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.users.application.command.PasswordSetupCommand;
import com.academy.mudogroupware.users.application.usecase.PasswordSetupUseCase;
import com.academy.mudogroupware.users.domain.exception.PasswordSetupFailedException;
import com.academy.mudogroupware.users.domain.model.User;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PasswordSetupService implements PasswordSetupUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void setup(PasswordSetupCommand command) {
        User user = userRepository.findById(command.userId())
                .filter(User::isMustChangePw)
                .orElseThrow(PasswordSetupFailedException::new);

        boolean updated = userRepository.completePasswordSetup(user.getId(),
                passwordEncoder.encode(command.newPassword()), command.phone(), command.email());
        if (!updated) {
            throw new PasswordSetupFailedException();
        }
    }
}
