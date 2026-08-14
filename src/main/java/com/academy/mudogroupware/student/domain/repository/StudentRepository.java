package com.academy.mudogroupware.student.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.student.domain.model.Student;

public interface StudentRepository {

    Student save(Student student);

    Optional<Student> findById(Long id);

    List<Student> findAllById(List<Long> ids);

    PageResult<Student> findAll(String keyword, int page, int size);

    // 소프트 삭제된 학생은 findById/findAll 조회에서 제외된다(deleted_at is null 조건).
    void markDeleted(Long id, LocalDateTime deletedAt);

    // 소프트 삭제되지 않은(deleted_at is null) 학생 수만 반환한다 — 플랜 한도 계산이 이 계약에 의존한다.
    long countAll();
}
