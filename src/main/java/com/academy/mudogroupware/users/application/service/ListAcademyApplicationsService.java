package com.academy.mudogroupware.users.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.users.application.usecase.ListAcademyApplicationsUseCase;
import com.academy.mudogroupware.users.domain.model.AcademyApplication;
import com.academy.mudogroupware.users.domain.repository.AcademyApplicationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListAcademyApplicationsService implements ListAcademyApplicationsUseCase {

    private final AcademyApplicationRepository academyApplicationRepository;

    @Override
    public List<AcademyApplication> listApplications() {
        log.info("event=academy_application_list_시작");
        List<AcademyApplication> applications = academyApplicationRepository.findAll();
        log.info("event=academy_application_list_완료 count={}", applications.size());
        return applications;
    }
}
