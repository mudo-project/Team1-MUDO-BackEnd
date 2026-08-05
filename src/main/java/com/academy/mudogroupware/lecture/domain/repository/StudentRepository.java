package com.academy.mudogroupware.lecture.domain.repository;

import java.util.List;
import java.util.Optional;

import com.academy.mudogroupware.lecture.domain.model.Student;

public interface StudentRepository {

    Student save(Student student);

    Optional<Student> findById(Long id);

    List<Student> findAllById(List<Long> ids);
}
