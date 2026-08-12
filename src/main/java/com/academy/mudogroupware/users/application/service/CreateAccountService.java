package com.academy.mudogroupware.users.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.users.application.command.CreateAccountCommand;
import com.academy.mudogroupware.users.application.result.CreateAccountResult;
import com.academy.mudogroupware.users.application.service.support.AccountIssuer;
import com.academy.mudogroupware.users.application.service.support.IssuedAccount;
import com.academy.mudogroupware.users.application.usecase.CreateAccountUseCase;
import com.academy.mudogroupware.users.domain.exception.RoleNotFoundException;
import com.academy.mudogroupware.users.domain.exception.UsernameDuplicateException;
import com.academy.mudogroupware.users.domain.model.Role;
import com.academy.mudogroupware.users.domain.repository.RoleRepository;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CreateAccountService implements CreateAccountUseCase {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AccountIssuer accountIssuer;
    private final Clock clock;

    @Override
    public CreateAccountResult createAccount(CreateAccountCommand command) {
        log.info("event=account_create_시작 roleId={}", command.roleId());
        try {
            if (userRepository.existsByUsername(command.username())) {
                throw new UsernameDuplicateException();
            }
            Role role = roleRepository.findById(command.roleId())
                    .orElseThrow(RoleNotFoundException::new);

            LocalDateTime now = LocalDateTime.now(clock);
            IssuedAccount issuedAccount = accountIssuer.issue(command.username(),
                    command.name(), role.getId(), AccountType.MEMBER, null, now);

            log.info("event=account_create_완료 userId={}", issuedAccount.user().getId());
            return new CreateAccountResult(issuedAccount.user().getId(), issuedAccount.user().getUsername(),
                    issuedAccount.temporaryPassword());
        } catch (RuntimeException e) {
            log.warn("event=account_create_실패 roleId={}, reason={}", command.roleId(), e.getMessage(), e);
            throw e;
        }
    }
}
