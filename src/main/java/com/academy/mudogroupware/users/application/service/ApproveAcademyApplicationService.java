package com.academy.mudogroupware.users.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

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
import com.academy.mudogroupware.users.domain.model.Academy;
import com.academy.mudogroupware.users.domain.model.AcademyApplication;
import com.academy.mudogroupware.users.domain.repository.AcademyApplicationRepository;
import com.academy.mudogroupware.users.domain.repository.AcademyRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ApproveAcademyApplicationService implements ApproveAcademyApplicationUseCase {

    private final AcademyApplicationRepository academyApplicationRepository;
    private final AcademyRepository academyRepository;
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

            LocalDateTime now = LocalDateTime.now(clock);
            academyApplicationRepository.markApproved(command.applicationId(), command.reviewerId(), now);

            Academy academy = academyRepository.save(Academy.create(
                    application.getAcademyName(), application.getBusinessNo(), application.getId(), now));

            IssuedAccount issuedAccount = accountIssuer.issue(academy.getId(), application.getRequestedLoginId(),
                    application.getRepresentativeName(), application.getRepresentativePhone(),
                    application.getRepresentativeEmail(), null, AccountType.ADMIN, AdminScope.ACADEMY, now);

            academyRepository.assignUser(academy.getId(), issuedAccount.user().getId(), now);

            eventPublisher.publishEvent(new AcademyApplicationApprovedEvent(
                    academy.getId(), issuedAccount.user().getId(), application.getId()));

            log.info("event=academy_application_approve_완료 applicationId={}, academyId={}, userId={}",
                    command.applicationId(), academy.getId(), issuedAccount.user().getId());
            return new ApproveAcademyApplicationResult(academy.getId(), issuedAccount.user().getId(),
                    issuedAccount.temporaryPassword());
        } catch (RuntimeException e) {
            log.warn("event=academy_application_approve_실패 applicationId={}, reviewerId={}, reason={}",
                    command.applicationId(), command.reviewerId(), e.getMessage(), e);
            throw e;
        }
    }
}
