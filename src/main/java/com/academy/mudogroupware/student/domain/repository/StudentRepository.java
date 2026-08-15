package com.academy.mudogroupware.student.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.student.domain.model.Student;
import com.academy.mudogroupware.student.domain.model.StudentSortDirection;

public interface StudentRepository {

    Student save(Student student);

    Optional<Student> findById(Long id);

    List<Student> findAllById(List<Long> ids);

    PageResult<Student> findAll(String keyword, int page, int size);

    default PageResult<Student> findAll(String keyword, int page, int size, StudentSortDirection direction) {
        return findAll(keyword, page, size);
    }

    // Soft-deleted students are excluded from findById/findAll queries.
    void markDeleted(Long id, LocalDateTime deletedAt);

    // Returns only non-deleted students for dashboard counts.
    long countAll();
}
