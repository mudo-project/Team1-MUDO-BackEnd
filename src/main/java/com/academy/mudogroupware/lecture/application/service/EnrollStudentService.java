package com.academy.mudogroupware.lecture.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.lecture.application.command.EnrollStudentCommand;
import com.academy.mudogroupware.lecture.application.usecase.EnrollStudentUseCase;
import com.academy.mudogroupware.lecture.domain.exception.LectureNotFoundException;
import com.academy.mudogroupware.lecture.domain.exception.StudentNotFoundException;
import com.academy.mudogroupware.lecture.domain.model.Enrollment;
import com.academy.mudogroupware.lecture.domain.repository.EnrollmentRepository;
import com.academy.mudogroupware.lecture.domain.repository.LectureRepository;
import com.academy.mudogroupware.lecture.domain.repository.StudentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class EnrollStudentService implements EnrollStudentUseCase {

    private final LectureRepository lectureRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final Clock clock;

    @Override
    public void enrollStudent(EnrollStudentCommand command) {
        lectureRepository.findById(command.lectureId()).orElseThrow(LectureNotFoundException::new);
        studentRepository.findById(command.studentId()).orElseThrow(StudentNotFoundException::new);

        LocalDateTime now = LocalDateTime.now(clock);
        Enrollment enrollment = Enrollment.create(command.studentId(), command.lectureId(), now);
        enrollmentRepository.save(enrollment);
    }
}
