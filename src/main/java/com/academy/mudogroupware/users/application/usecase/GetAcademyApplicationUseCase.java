package com.academy.mudogroupware.users.application.usecase;

import com.academy.mudogroupware.users.domain.model.AcademyApplication;

public interface GetAcademyApplicationUseCase {

    AcademyApplication getApplication(Long applicationId);
}
