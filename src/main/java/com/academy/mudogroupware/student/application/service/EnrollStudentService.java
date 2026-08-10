package com.academy.mudogroupware.student.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.student.application.command.EnrollStudentCommand;
import com.academy.mudogroupware.student.application.usecase.EnrollStudentUseCase;
import com.academy.mudogroupware.student.domain.exception.StudentErrorCode;
import com.academy.mudogroupware.student.domain.exception.StudentException;
import com.academy.mudogroupware.student.domain.model.Enrollment;
import com.academy.mudogroupware.student.domain.repository.EnrollmentRepository;
import com.academy.mudogroupware.student.domain.repository.StudentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class EnrollStudentService implements EnrollStudentUseCase {

    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final Clock clock;

    @Override
    public Long enroll(EnrollStudentCommand command) {
        studentRepository.findById(command.studentId())
                .orElseThrow(() -> new StudentException(StudentErrorCode.STUDENT_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now(clock);
        return enrollmentRepository
                .findByStudentIdAndLectureId(command.studentId(), command.lectureId())
                .map(existing -> enrollExisting(existing, now))
                .orElseGet(() -> enrollmentRepository.save(
                        Enrollment.create(command.studentId(), command.lectureId(), now)))
                .getId();
    }

    private Enrollment enrollExisting(Enrollment enrollment, LocalDateTime now) {
        if (enrollment.isActive()) {
            throw new StudentException(StudentErrorCode.DUPLICATE_ACTIVE_ENROLLMENT);
        }
        enrollment.reactivate(now);
        return enrollmentRepository.save(enrollment);
    }
}
