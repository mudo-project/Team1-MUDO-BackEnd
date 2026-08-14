package com.academy.mudogroupware.notice.application.retention;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.global.scheduler.RetentionJobResult;
import com.academy.mudogroupware.notice.application.port.NoticeRetentionPort;
import com.academy.mudogroupware.notice.domain.event.NoticeAttachmentFilesCleanupRequestedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class NoticeRetentionService {

    public static final String JOB_NAME = "notice_retention";

    private final NoticeRetentionProperties properties;
    private final NoticeRetentionPort noticeRetentionPort;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public RetentionJobResult hardDeleteExpiredNotices(LocalDateTime now) {
        List<NoticeRetentionTarget> targets =
                noticeRetentionPort.findExpiredNoticeTargets(now, properties.batchSize());

        if (targets.isEmpty()) {
            log.info("event=notice_retention_empty now={} recoveryDays={} batchSize={}",
                    now, properties.recoveryDays(), properties.batchSize());
            return RetentionJobResult.empty(JOB_NAME);
        }

        List<Long> noticeIds = targets.stream()
                .map(NoticeRetentionTarget::noticeId)
                .toList();
        List<Long> fileIds = targets.stream()
                .flatMap(target -> target.fileIds().stream())
                .distinct()
                .toList();

        int deletedReadCount = noticeRetentionPort.deleteReadRecordsByNoticeIds(noticeIds);
        int deletedAttachmentCount = noticeRetentionPort.deleteAttachmentsByNoticeIds(noticeIds);
        int deletedNoticeCount = noticeRetentionPort.hardDeleteNoticesByIds(noticeIds, now);

        if (!fileIds.isEmpty()) {
            eventPublisher.publishEvent(new NoticeAttachmentFilesCleanupRequestedEvent(fileIds));
        }

        log.info("event=notice_retention_completed candidateCount={} deletedChildCount={} deletedParentCount={}",
                targets.size(), deletedReadCount + deletedAttachmentCount, deletedNoticeCount);
        return new RetentionJobResult(JOB_NAME, targets.size(),
                deletedReadCount + deletedAttachmentCount, deletedNoticeCount);
    }
}
