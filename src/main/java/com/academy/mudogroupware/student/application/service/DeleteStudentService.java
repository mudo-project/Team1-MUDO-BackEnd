package com.academy.mudogroupware.student.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.student.application.command.DeleteStudentCommand;
import com.academy.mudogroupware.student.application.usecase.DeleteStudentUseCase;
import com.academy.mudogroupware.student.domain.exception.StudentErrorCode;
import com.academy.mudogroupware.student.domain.exception.StudentException;
import com.academy.mudogroupware.student.domain.model.Student;
import com.academy.mudogroupware.student.domain.repository.StudentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DeleteStudentService implements DeleteStudentUseCase {

    private final StudentRepository studentRepository;
    private final Clock clock;

    @Override
    public void deleteStudent(DeleteStudentCommand command) {
        Student student = studentRepository.findById(command.studentId())
                .orElseThrow(() -> new StudentException(StudentErrorCode.STUDENT_NOT_FOUND));
        studentRepository.markDeleted(student.getId(), LocalDateTime.now(clock));
    }
}
