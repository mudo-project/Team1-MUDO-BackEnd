package com.academy.mudogroupware.student.application.retention;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.global.scheduler.RetentionJobResult;
import com.academy.mudogroupware.student.application.port.StudentRetentionPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class StudentRetentionService {

    public static final String JOB_NAME = "student_retention";

    private final StudentRetentionProperties properties;
    private final StudentRetentionPort studentRetentionPort;

    @Transactional
    public RetentionJobResult hardDeleteExpiredStudents(LocalDateTime now) {
        LocalDateTime threshold = properties.threshold(now);
        List<Long> candidateIds =
                studentRetentionPort.findHardDeleteCandidateIds(threshold, properties.batchSize());

        if (candidateIds.isEmpty()) {
            log.info("event=student_retention_empty threshold={} periodDays={} batchSize={}",
                    threshold, properties.periodDays(), properties.batchSize());
            return RetentionJobResult.empty(JOB_NAME);
        }

        // 자식(수강 이력)부터 지운 뒤 부모(학생)를 지운다 — FK 제약 순서.
        int deletedChildCount = studentRetentionPort.deleteEnrollmentsByStudentIds(candidateIds);
        int deletedParentCount = studentRetentionPort.hardDeleteStudentsByIds(candidateIds, threshold);

        log.info("event=student_retention_completed candidateCount={} deletedChildCount={} deletedParentCount={}",
                candidateIds.size(), deletedChildCount, deletedParentCount);

        return new RetentionJobResult(JOB_NAME, candidateIds.size(), deletedChildCount, deletedParentCount);
    }
}
