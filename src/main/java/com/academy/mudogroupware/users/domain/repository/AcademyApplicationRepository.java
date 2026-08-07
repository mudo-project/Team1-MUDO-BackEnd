package com.academy.mudogroupware.users.domain.repository;

import java.util.List;

import com.academy.mudogroupware.users.domain.model.AcademyApplication;

public interface AcademyApplicationRepository {

    List<AcademyApplication> findAll();
}
