package com.academy.mudogroupware.student.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.student.application.port.StudentRetentionPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StudentRetentionAdapter implements StudentRetentionPort {

    private final StudentJpaRepository studentJpaRepository;
    private final EnrollmentJpaRepository enrollmentJpaRepository;

    @Override
    public List<Long> findHardDeleteCandidateIds(LocalDateTime threshold, int batchSize) {
        return studentJpaRepository.findHardDeleteCandidateIds(threshold, batchSize);
    }

    @Override
    public int deleteEnrollmentsByStudentIds(List<Long> studentIds) {
        if (studentIds.isEmpty()) {
            return 0;
        }
        return enrollmentJpaRepository.deleteAllByStudentIds(studentIds);
    }

    @Override
    public int hardDeleteStudentsByIds(List<Long> studentIds, LocalDateTime threshold) {
        if (studentIds.isEmpty()) {
            return 0;
        }
        return studentJpaRepository.hardDeleteByIds(studentIds, threshold);
    }
}
