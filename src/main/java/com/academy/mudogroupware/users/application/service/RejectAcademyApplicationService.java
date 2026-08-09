package com.academy.mudogroupware.users.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.users.application.command.RejectAcademyApplicationCommand;
import com.academy.mudogroupware.users.application.usecase.RejectAcademyApplicationUseCase;
import com.academy.mudogroupware.users.domain.exception.AcademyApplicationNotFoundException;
import com.academy.mudogroupware.users.domain.model.AcademyApplication;
import com.academy.mudogroupware.users.domain.repository.AcademyApplicationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RejectAcademyApplicationService implements RejectAcademyApplicationUseCase {

    private final AcademyApplicationRepository academyApplicationRepository;
    private final Clock clock;

    @Override
    public void reject(RejectAcademyApplicationCommand command) {
        log.info("event=academy_application_reject_시작 applicationId={}, reviewerId={}", command.applicationId(),
                command.reviewerId());
        try {
            AcademyApplication application = academyApplicationRepository.findById(command.applicationId())
                    .orElseThrow(AcademyApplicationNotFoundException::new);
            application.ensurePending();

            academyApplicationRepository.markRejected(command.applicationId(), command.reviewerId(),
                    LocalDateTime.now(clock), command.reason());
            log.info("event=academy_application_reject_완료 applicationId={}", command.applicationId());
        } catch (RuntimeException e) {
            log.warn("event=academy_application_reject_실패 applicationId={}, reviewerId={}, reason={}",
                    command.applicationId(), command.reviewerId(), e.getMessage());
            throw e;
        }
    }
}
