package com.academy.mudogroupware.notice.application.usecase;

import java.util.List;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.notice.application.query.NoticeDetailView;
import com.academy.mudogroupware.notice.application.query.NoticeReaderView;
import com.academy.mudogroupware.notice.application.query.NoticeSummaryView;

public interface NoticeQueryUseCase {

    PageResult<NoticeSummaryView> getNotices(Long requesterId, String keyword, int page, int size);

    NoticeDetailView getNoticeDetail(Long noticeId, Long requesterId);

    List<NoticeReaderView> getReaders(Long noticeId, Long requesterId);
}
