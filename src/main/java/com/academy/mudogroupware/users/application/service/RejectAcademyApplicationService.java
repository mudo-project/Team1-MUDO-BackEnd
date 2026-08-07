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

@Service
@RequiredArgsConstructor
@Transactional
public class RejectAcademyApplicationService implements RejectAcademyApplicationUseCase {

    private final AcademyApplicationRepository academyApplicationRepository;
    private final Clock clock;

    @Override
    public void reject(RejectAcademyApplicationCommand command) {
        AcademyApplication application = academyApplicationRepository.findById(command.applicationId())
                .orElseThrow(AcademyApplicationNotFoundException::new);
        application.ensurePending();

        academyApplicationRepository.markRejected(command.applicationId(), command.reviewerId(),
                LocalDateTime.now(clock), command.reason());
    }
}
