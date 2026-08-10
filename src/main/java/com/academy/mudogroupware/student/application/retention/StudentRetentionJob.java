package com.academy.mudogroupware.student.application.retention;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.global.scheduler.RetentionJob;
import com.academy.mudogroupware.global.scheduler.RetentionJobResult;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StudentRetentionJob implements RetentionJob {

    private final StudentRetentionService studentRetentionService;

    @Override
    public String name() {
        return StudentRetentionService.JOB_NAME;
    }

    @Override
    public RetentionJobResult run(LocalDateTime now) {
        return studentRetentionService.hardDeleteExpiredStudents(now);
    }
}
