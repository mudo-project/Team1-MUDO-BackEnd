package com.academy.mudogroupware.users.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.users.application.command.SubmitAcademyApplicationCommand;
import com.academy.mudogroupware.users.application.usecase.SubmitAcademyApplicationUseCase;
import com.academy.mudogroupware.users.domain.exception.UsernameDuplicateException;
import com.academy.mudogroupware.users.domain.model.AcademyApplication;
import com.academy.mudogroupware.users.domain.repository.AcademyApplicationRepository;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SubmitAcademyApplicationService implements SubmitAcademyApplicationUseCase {

    private final AcademyApplicationRepository academyApplicationRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Override
    public Long submit(SubmitAcademyApplicationCommand command) {
        log.info("event=academy_application_submit_시작 academyName={}, plan={}", command.academyName(),
                command.plan());
        try {
            if (userRepository.existsByUsername(command.requestedLoginId())
                    || academyApplicationRepository.existsActiveRequestedLoginId(command.requestedLoginId())) {
                throw new UsernameDuplicateException();
            }

            AcademyApplication application = AcademyApplication.submit(
                    command.requestedLoginId(), command.academyName(), command.representativeName(),
                    command.representativeEmail(), command.representativePhone(), command.plan(),
                    LocalDateTime.now(clock));

            AcademyApplication saved = academyApplicationRepository.save(application);

            log.info("event=academy_application_submit_완료 applicationId={}", saved.getId());
            return saved.getId();
        } catch (RuntimeException e) {
            log.warn("event=academy_application_submit_실패 academyName={}, reason={}", command.academyName(),
                    e.getMessage(), e);
            throw e;
        }
    }
}
