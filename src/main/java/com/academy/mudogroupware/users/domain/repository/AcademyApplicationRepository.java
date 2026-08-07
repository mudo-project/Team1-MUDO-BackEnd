package com.academy.mudogroupware.users.domain.repository;

import java.util.List;
import java.util.Optional;

import com.academy.mudogroupware.users.domain.model.AcademyApplication;

public interface AcademyApplicationRepository {

    List<AcademyApplication> findAll();

    Optional<AcademyApplication> findById(Long id);
}
