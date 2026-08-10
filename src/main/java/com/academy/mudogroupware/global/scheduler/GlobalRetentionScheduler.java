package com.academy.mudogroupware.global.scheduler;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class GlobalRetentionScheduler {

    private final List<RetentionJob> retentionJobs;
    private final Clock clock;

    @Scheduled(
            cron = "${app.scheduler.retention.cron:0 0 3 * * *}",
            zone = "${app.scheduler.retention.zone:Asia/Seoul}"
    )
    public void runRetentionJobs() {
        LocalDateTime now = LocalDateTime.now(clock);
        if (retentionJobs.isEmpty()) {
            log.debug("event=retention_jobs_empty");
            return;
        }
        for (RetentionJob job : retentionJobs) {
            runJob(job, now);
        }
    }

    private void runJob(RetentionJob job, LocalDateTime now) {
        try {
            RetentionJobResult result = job.run(now);
            log.info(
                    "event=retention_job_succeeded jobName={} candidateCount={} deletedChildCount={} deletedParentCount={}",
                    result.jobName(), result.candidateCount(),
                    result.deletedChildCount(), result.deletedParentCount());
        } catch (Exception exception) {
            // 한 도메인 실패가 다른 도메인 작업을 중단하지 않도록 격리하는 역할
            log.error("event=retention_job_failed jobName={}", job.name(), exception);
        }
    }
}
