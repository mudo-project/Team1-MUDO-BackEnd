package com.academy.mudogroupware.notice.application.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.notice.application.port.AuthorInfo;
import com.academy.mudogroupware.notice.application.port.NoticeAuthorDirectoryPort;
import com.academy.mudogroupware.notice.application.query.NoticeAttachmentView;
import com.academy.mudogroupware.notice.application.query.NoticeDetailView;
import com.academy.mudogroupware.notice.application.query.NoticeReaderView;
import com.academy.mudogroupware.notice.application.query.NoticeSummaryView;
import com.academy.mudogroupware.notice.application.usecase.NoticeQueryUseCase;
import com.academy.mudogroupware.notice.domain.exception.NoticeErrorCode;
import com.academy.mudogroupware.notice.domain.exception.NoticeException;
import com.academy.mudogroupware.notice.domain.model.Notice;
import com.academy.mudogroupware.notice.domain.model.NoticeAttachment;
import com.academy.mudogroupware.notice.domain.repository.NoticeReadRepository;
import com.academy.mudogroupware.notice.domain.repository.NoticeRepository;
import com.academy.mudogroupware.global.domain.common.page.PageResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeQueryService implements NoticeQueryUseCase {

    private final NoticeRepository noticeRepository;
    private final NoticeReadRepository noticeReadRepository;
    private final NoticeAuthorDirectoryPort noticeAuthorDirectoryPort;

    @Override
    public PageResult<NoticeSummaryView> getNotices(Long requesterId, String keyword, int page, int size) {
        log.info("event=notice_list_시작 requesterId={}, keyword={}, page={}, size={}", requesterId, keyword, page,
                size);
        noticeAuthorDirectoryPort.getAuthor(requesterId);

        PageResult<Notice> result = noticeRepository.findAll(keyword, page, size);
        List<Notice> notices = result.content();
        List<Long> authorIds = notices.stream().map(Notice::getAuthorUserId).distinct().toList();
        List<Long> noticeIds = notices.stream().map(Notice::getId).toList();
        Map<Long, AuthorInfo> authors = noticeAuthorDirectoryPort.getAuthors(authorIds);
        Set<Long> readNoticeIds = noticeReadRepository.findReadNoticeIds(noticeIds, requesterId);

        PageResult<NoticeSummaryView> views = result.map(notice -> toSummaryView(notice, authors, readNoticeIds));
        log.info("event=notice_list_완료 requesterId={}, count={}, hasNext={}", requesterId,
                views.content().size(), views.hasNext());
        return views;
    }

    @Override
    @Transactional
    public NoticeDetailView getNoticeDetail(Long noticeId, Long requesterId) {
        log.info("event=notice_detail_시작 noticeId={}, requesterId={}", noticeId, requesterId);
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new NoticeException(NoticeErrorCode.NOTICE_NOT_FOUND));

        noticeAuthorDirectoryPort.getAuthor(requesterId);

        notice.recordView();
        noticeReadRepository.markRead(noticeId, requesterId);
        noticeRepository.save(notice);

        AuthorInfo author = noticeAuthorDirectoryPort.getAuthor(notice.getAuthorUserId());
        List<NoticeAttachmentView> attachments = notice.getAttachments().stream()
                .map(this::toAttachmentView)
                .toList();
        long readerCount = noticeReadRepository.countReaders(noticeId);
        long totalRecipientCount = noticeAuthorDirectoryPort.countActiveUsers();

        log.info("event=notice_detail_완료 noticeId={}, requesterId={}, viewCount={}", noticeId, requesterId,
                notice.getViewCount());
        return new NoticeDetailView(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getAuthorUserId(),
                author.name(),
                author.role(),
                notice.isPinned(),
                notice.getViewCount(),
                readerCount,
                totalRecipientCount,
                notice.getCreatedAt(),
                notice.getUpdatedAt(),
                attachments
        );
    }

    @Override
    public List<NoticeReaderView> getReaders(Long noticeId, Long requesterId) {
        log.info("event=notice_reader_list_시작 noticeId={}, requesterId={}", noticeId, requesterId);
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new NoticeException(NoticeErrorCode.NOTICE_NOT_FOUND));

        noticeAuthorDirectoryPort.getAuthor(requesterId);

        Map<Long, LocalDateTime> readTimestamps = noticeReadRepository.findReadTimestamps(noticeId);
        Map<Long, AuthorInfo> readers = noticeAuthorDirectoryPort.getAuthors(
                List.copyOf(readTimestamps.keySet()));

        List<NoticeReaderView> views = readTimestamps.entrySet().stream()
                .map(entry -> toReaderView(entry.getKey(), entry.getValue(), readers))
                .sorted((a, b) -> b.readAt().compareTo(a.readAt()))
                .toList();
        log.info("event=notice_reader_list_완료 noticeId={}, requesterId={}, count={}", noticeId, requesterId,
                views.size());
        return views;
    }

    private NoticeReaderView toReaderView(Long userId, LocalDateTime readAt, Map<Long, AuthorInfo> readers) {
        AuthorInfo reader = readers.get(userId);
        String name = reader != null ? reader.name() : null;
        String role = reader != null ? reader.role() : null;
        return new NoticeReaderView(userId, name, role, readAt);
    }

    private NoticeSummaryView toSummaryView(Notice notice, Map<Long, AuthorInfo> authors, Set<Long> readNoticeIds) {
        AuthorInfo author = authors.get(notice.getAuthorUserId());

        return new NoticeSummaryView(
                notice.getId(),
                notice.getTitle(),
                author != null ? author.name() : null,
                author != null ? author.role() : null,
                notice.isPinned(),
                readNoticeIds.contains(notice.getId()),
                !notice.getAttachments().isEmpty(),
                notice.getCreatedAt()
        );
    }

    private NoticeAttachmentView toAttachmentView(NoticeAttachment attachment) {
        return new NoticeAttachmentView(attachment.getId(), attachment.getFileId(), attachment.getFileName());
    }
}
