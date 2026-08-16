package com.academy.mudogroupware.student.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.planquota.application.service.CurrentPlanProvider;
import com.academy.mudogroupware.planquota.domain.exception.PlanLimitErrorCode;
import com.academy.mudogroupware.planquota.domain.exception.PlanLimitExceededException;
import com.academy.mudogroupware.student.application.command.CreateStudentCommand;
import com.academy.mudogroupware.student.application.usecase.CreateStudentUseCase;
import com.academy.mudogroupware.student.domain.model.Student;
import com.academy.mudogroupware.student.domain.repository.StudentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateStudentService implements CreateStudentUseCase {

    private final StudentRepository studentRepository;
    private final Clock clock;
    private final CurrentPlanProvider currentPlanProvider;

    @Override
    public Long createStudent(CreateStudentCommand command) {
        long limit = currentPlanProvider.currentLimits().studentLimit();
        long current = studentRepository.countAll();
        if (current >= limit) {
            throw new PlanLimitExceededException(PlanLimitErrorCode.STUDENT_LIMIT_EXCEEDED,
                    currentPlanProvider.currentPlan(), limit, current);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        Student student = Student.create(command.name(), command.grade(), command.school(),
                command.phone(), command.parentPhone(), command.note(), now);
        return studentRepository.save(student).getId();
    }
}
