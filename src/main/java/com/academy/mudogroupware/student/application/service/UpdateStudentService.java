package com.academy.mudogroupware.student.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.student.application.command.UpdateStudentCommand;
import com.academy.mudogroupware.student.application.usecase.UpdateStudentUseCase;
import com.academy.mudogroupware.student.domain.exception.StudentErrorCode;
import com.academy.mudogroupware.student.domain.exception.StudentException;
import com.academy.mudogroupware.student.domain.model.Student;
import com.academy.mudogroupware.student.domain.repository.StudentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateStudentService implements UpdateStudentUseCase {

    private final StudentRepository studentRepository;
    private final Clock clock;

    @Override
    public void updateStudent(UpdateStudentCommand command) {
        Student student = studentRepository.findById(command.studentId())
                .orElseThrow(() -> new StudentException(StudentErrorCode.STUDENT_NOT_FOUND));
        student.update(command.name(), command.grade(), command.school(), command.phone(), command.parentPhone(),
                command.note(), LocalDateTime.now(clock));
        studentRepository.save(student);
    }
}
