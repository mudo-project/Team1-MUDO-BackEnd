package com.academy.mudogroupware.student.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.student.application.command.EndEnrollmentCommand;
import com.academy.mudogroupware.student.application.usecase.EndEnrollmentUseCase;
import com.academy.mudogroupware.student.domain.exception.StudentErrorCode;
import com.academy.mudogroupware.student.domain.exception.StudentException;
import com.academy.mudogroupware.student.domain.model.Enrollment;
import com.academy.mudogroupware.student.domain.repository.EnrollmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class EndEnrollmentService implements EndEnrollmentUseCase {

    private final EnrollmentRepository enrollmentRepository;
    private final Clock clock;

    @Override
    public void end(EndEnrollmentCommand command) {
        Enrollment enrollment = enrollmentRepository.findById(command.studentId(), command.enrollmentId())
                .orElseThrow(() -> new StudentException(StudentErrorCode.ENROLLMENT_NOT_FOUND));
        enrollment.end(LocalDateTime.now(clock));
        enrollmentRepository.save(enrollment);
    }
}
