package com.academy.mudogroupware.users.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.users.application.usecase.GetAcademyApplicationUseCase;
import com.academy.mudogroupware.users.domain.exception.AcademyApplicationNotFoundException;
import com.academy.mudogroupware.users.domain.model.AcademyApplication;
import com.academy.mudogroupware.users.domain.repository.AcademyApplicationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetAcademyApplicationService implements GetAcademyApplicationUseCase {

    private final AcademyApplicationRepository academyApplicationRepository;

    @Override
    public AcademyApplication getApplication(Long applicationId) {
        return academyApplicationRepository.findById(applicationId)
                .orElseThrow(AcademyApplicationNotFoundException::new);
    }
}
