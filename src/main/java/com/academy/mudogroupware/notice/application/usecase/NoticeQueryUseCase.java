package com.academy.mudogroupware.notice.application.usecase;

import java.util.List;

import com.academy.mudogroupware.notice.application.query.NoticeDetailView;
import com.academy.mudogroupware.notice.application.query.NoticeReaderView;
import com.academy.mudogroupware.notice.application.query.NoticeSummaryView;

public interface NoticeQueryUseCase {

    List<NoticeSummaryView> getNotices(Long requesterId, String keyword);

    NoticeDetailView getNoticeDetail(Long noticeId, Long requesterId);

    List<NoticeReaderView> getReaders(Long noticeId, Long requesterId);
}
