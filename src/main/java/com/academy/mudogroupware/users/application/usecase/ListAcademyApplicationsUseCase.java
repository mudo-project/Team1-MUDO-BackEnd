package com.academy.mudogroupware.users.application.usecase;

import java.util.List;

import com.academy.mudogroupware.users.domain.model.AcademyApplication;

public interface ListAcademyApplicationsUseCase {

    List<AcademyApplication> listApplications();
}
