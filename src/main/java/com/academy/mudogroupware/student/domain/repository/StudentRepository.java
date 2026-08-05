package com.academy.mudogroupware.student.domain.repository;

import java.util.List;
import java.util.Optional;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.student.domain.model.Student;

public interface StudentRepository {

    Student save(Student student);

    Optional<Student> findById(Long id);

    List<Student> findAllById(List<Long> ids);

    PageResult<Student> findAll(Long academyId, String keyword, int page, int size);
}
