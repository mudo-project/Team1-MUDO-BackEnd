package com.academy.mudogroupware.notice.application.retention;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.global.scheduler.RetentionJob;
import com.academy.mudogroupware.global.scheduler.RetentionJobResult;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NoticeRetentionJob implements RetentionJob {

    private final NoticeRetentionService noticeRetentionService;

    @Override
    public String name() {
        return NoticeRetentionService.JOB_NAME;
    }

    @Override
    public RetentionJobResult run(LocalDateTime now) {
        return noticeRetentionService.hardDeleteExpiredNotices(now);
    }
}
