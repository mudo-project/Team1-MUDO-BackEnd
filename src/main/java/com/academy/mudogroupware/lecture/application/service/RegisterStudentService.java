package com.academy.mudogroupware.lecture.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.lecture.application.command.RegisterStudentCommand;
import com.academy.mudogroupware.lecture.application.usecase.RegisterStudentUseCase;
import com.academy.mudogroupware.lecture.domain.model.Student;
import com.academy.mudogroupware.lecture.domain.repository.StudentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RegisterStudentService implements RegisterStudentUseCase {

    private final StudentRepository studentRepository;
    private final Clock clock;

    @Override
    public Long registerStudent(RegisterStudentCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        Student student = Student.create(command.academyId(), command.name(), command.grade(), command.school(),
                command.phone(), command.parentPhone(), command.note(), now);
        return studentRepository.save(student).getId();
    }
}
