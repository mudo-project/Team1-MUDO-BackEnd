package com.academy.mudogroupware.notice.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.notice.application.port.NoticeRetentionPort;
import com.academy.mudogroupware.notice.application.retention.NoticeAttachmentFileReference;
import com.academy.mudogroupware.notice.application.retention.NoticeRetentionTarget;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NoticeRetentionAdapter implements NoticeRetentionPort {

    private final NoticeJpaRepository noticeJpaRepository;
    private final NoticeAttachmentJpaRepository noticeAttachmentJpaRepository;
    private final NoticeReadJpaRepository noticeReadJpaRepository;

    @Override
    public List<NoticeRetentionTarget> findExpiredNoticeTargets(LocalDateTime now, int batchSize) {
        List<Long> noticeIds = noticeJpaRepository.findHardDeleteCandidateIds(now, batchSize);
        if (noticeIds.isEmpty()) {
            return List.of();
        }

        Map<Long, List<Long>> fileIdsByNoticeId = noticeAttachmentJpaRepository
                .findFileReferencesByNoticeIds(noticeIds)
                .stream()
                .collect(Collectors.groupingBy(
                        NoticeAttachmentFileReference::noticeId,
                        Collectors.mapping(NoticeAttachmentFileReference::fileId, Collectors.toList())));

        return noticeIds.stream()
                .map(noticeId -> new NoticeRetentionTarget(
                        noticeId, fileIdsByNoticeId.getOrDefault(noticeId, List.of())))
                .toList();
    }

    @Override
    public int deleteReadRecordsByNoticeIds(List<Long> noticeIds) {
        if (noticeIds.isEmpty()) {
            return 0;
        }
        return noticeReadJpaRepository.deleteAllByNoticeIds(noticeIds);
    }

    @Override
    public int deleteAttachmentsByNoticeIds(List<Long> noticeIds) {
        if (noticeIds.isEmpty()) {
            return 0;
        }
        return noticeAttachmentJpaRepository.deleteAllByNoticeIds(noticeIds);
    }

    @Override
    public int hardDeleteNoticesByIds(List<Long> noticeIds, LocalDateTime now) {
        if (noticeIds.isEmpty()) {
            return 0;
        }
        return noticeJpaRepository.hardDeleteExpiredByIds(noticeIds, now);
    }
}
