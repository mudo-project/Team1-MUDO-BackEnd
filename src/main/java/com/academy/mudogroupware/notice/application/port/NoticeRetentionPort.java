package com.academy.mudogroupware.notice.application.port;

import java.time.LocalDateTime;
import java.util.List;

import com.academy.mudogroupware.notice.application.retention.NoticeRetentionTarget;

public interface NoticeRetentionPort {

    List<NoticeRetentionTarget> findExpiredNoticeTargets(LocalDateTime now, int batchSize);

    int deleteReadRecordsByNoticeIds(List<Long> noticeIds);

    int deleteAttachmentsByNoticeIds(List<Long> noticeIds);

    int hardDeleteNoticesByIds(List<Long> noticeIds, LocalDateTime now);
}
