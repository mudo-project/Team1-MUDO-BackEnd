package com.academy.mudogroupware.lecture.domain.repository;

import java.util.List;
import java.util.Optional;

import com.academy.mudogroupware.lecture.domain.model.Classroom;

public interface ClassroomRepository {

    Optional<Classroom> findByAcademyIdAndName(Long academyId, String name);

    List<Classroom> findAllById(List<Long> ids);

    Classroom save(Classroom classroom);
}
