package com.academy.mudogroupware.lecture.domain.repository;

import java.util.List;
import java.util.Optional;

import com.academy.mudogroupware.lecture.domain.model.Subject;

public interface SubjectRepository {

    Optional<Subject> findByName(String name);

    List<Subject> findAllById(List<Long> ids);

    Subject save(Subject subject);
}
