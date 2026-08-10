package com.academy.mudogroupware.users.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.global.domain.auth.AdminScope;
import com.academy.mudogroupware.users.application.command.ApproveAcademyApplicationCommand;
import com.academy.mudogroupware.users.application.result.ApproveAcademyApplicationResult;
import com.academy.mudogroupware.users.application.service.support.AccountIssuer;
import com.academy.mudogroupware.users.application.service.support.IssuedAccount;
import com.academy.mudogroupware.users.application.usecase.ApproveAcademyApplicationUseCase;
import com.academy.mudogroupware.users.domain.event.AcademyApplicationApprovedEvent;
import com.academy.mudogroupware.users.domain.exception.AcademyApplicationNotFoundException;
import com.academy.mudogroupware.users.domain.exception.UsernameDuplicateException;
import com.academy.mudogroupware.users.domain.model.Academy;
import com.academy.mudogroupware.users.domain.model.AcademyApplication;
import com.academy.mudogroupware.users.domain.model.Permission;
import com.academy.mudogroupware.users.domain.model.Role;
import com.academy.mudogroupware.users.domain.repository.AcademyApplicationRepository;
import com.academy.mudogroupware.users.domain.repository.AcademyRepository;
import com.academy.mudogroupware.users.domain.repository.PermissionRepository;
import com.academy.mudogroupware.users.domain.repository.RoleRepository;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ApproveAcademyApplicationService implements ApproveAcademyApplicationUseCase {

    private static final String DIRECTOR_ROLE_NAME = "원장";
    private static final String DIRECTOR_ROLE_DESCRIPTION = "학원 관리자 기본 역할";

    private final AcademyApplicationRepository academyApplicationRepository;
    private final AcademyRepository academyRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AccountIssuer accountIssuer;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Override
    public ApproveAcademyApplicationResult approve(ApproveAcademyApplicationCommand command) {
        log.info("event=academy_application_approve_시작 applicationId={}, reviewerId={}", command.applicationId(),
                command.reviewerId());
        try {
            AcademyApplication application = academyApplicationRepository.findById(command.applicationId())
                    .orElseThrow(AcademyApplicationNotFoundException::new);
            application.ensurePending();
            if (userRepository.existsByUsername(application.getRequestedLoginId())) {
                throw new UsernameDuplicateException();
            }

            LocalDateTime now = LocalDateTime.now(clock);
            academyApplicationRepository.markApproved(command.applicationId(), command.reviewerId(), now);

            Academy academy = academyRepository.save(Academy.create(
                    application.getAcademyName(), application.getBusinessNo(), application.getId(), now));

            Role directorRole = roleRepository.save(
                    Role.create(academy.getId(), DIRECTOR_ROLE_NAME, DIRECTOR_ROLE_DESCRIPTION, now));
            Set<String> allPermissionCodes = permissionRepository.findAll().stream()
                    .map(Permission::code)
                    .collect(Collectors.toSet());
            roleRepository.updatePermissions(directorRole.getId(), allPermissionCodes);

            IssuedAccount issuedAccount = accountIssuer.issue(academy.getId(), application.getRequestedLoginId(),
                    application.getRepresentativeName(), application.getRepresentativePhone(),
                    application.getRepresentativeEmail(), directorRole.getId(), AccountType.ADMIN,
                    AdminScope.ACADEMY, now);

            academyRepository.assignUser(academy.getId(), issuedAccount.user().getId(), now);

            eventPublisher.publishEvent(new AcademyApplicationApprovedEvent(
                    academy.getId(), issuedAccount.user().getId(), application.getId()));

            log.info("event=academy_application_approve_완료 applicationId={}, academyId={}, userId={}, "
                    + "directorRoleId={}", command.applicationId(), academy.getId(), issuedAccount.user().getId(),
                    directorRole.getId());
            return new ApproveAcademyApplicationResult(academy.getId(), issuedAccount.user().getId(),
                    issuedAccount.passwordSetupLink());
        } catch (RuntimeException e) {
            log.warn("event=academy_application_approve_실패 applicationId={}, reviewerId={}, reason={}",
                    command.applicationId(), command.reviewerId(), e.getMessage(), e);
            throw e;
        }
    }
}
