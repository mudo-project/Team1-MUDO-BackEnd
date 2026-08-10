package com.academy.mudogroupware.users.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.users.application.usecase.GetAcademyApplicationUseCase;
import com.academy.mudogroupware.users.domain.exception.AcademyApplicationNotFoundException;
import com.academy.mudogroupware.users.domain.model.AcademyApplication;
import com.academy.mudogroupware.users.domain.repository.AcademyApplicationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetAcademyApplicationService implements GetAcademyApplicationUseCase {

    private final AcademyApplicationRepository academyApplicationRepository;

    @Override
    public AcademyApplication getApplication(Long applicationId) {
        log.info("event=academy_application_get_시작 applicationId={}", applicationId);
        try {
            AcademyApplication application = academyApplicationRepository.findById(applicationId)
                    .orElseThrow(AcademyApplicationNotFoundException::new);
            log.info("event=academy_application_get_완료 applicationId={}", applicationId);
            return application;
        } catch (RuntimeException e) {
            log.warn("event=academy_application_get_실패 applicationId={}, reason={}", applicationId,
                    e.getMessage(), e);
            throw e;
        }
    }
}
