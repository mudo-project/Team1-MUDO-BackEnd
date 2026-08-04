package com.academy.mudogroupware.notice.application.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeQueryService implements NoticeQueryUseCase {

    private final NoticeRepository noticeRepository;
    private final NoticeReadRepository noticeReadRepository;
    private final NoticeAuthorDirectoryPort noticeAuthorDirectoryPort;

    @Override
    public PageResult<NoticeSummaryView> getNotices(Long requesterId, String keyword, int page, int size) {
        AuthorInfo requester = noticeAuthorDirectoryPort.getAuthor(requesterId);

        return noticeRepository.findAll(requester.academyId(), keyword, page, size)
                .map(notice -> toSummaryView(notice, requesterId));
    }

    @Override
    @Transactional
    public NoticeDetailView getNoticeDetail(Long noticeId, Long requesterId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new NoticeException(NoticeErrorCode.NOTICE_NOT_FOUND));

        AuthorInfo requester = noticeAuthorDirectoryPort.getAuthor(requesterId);
        if (!notice.getAcademyId().equals(requester.academyId())) {
            throw new NoticeException(NoticeErrorCode.NOTICE_ACCESS_DENIED);
        }

        notice.recordView();
        noticeReadRepository.markRead(noticeId, requesterId);
        noticeRepository.save(notice);

        AuthorInfo author = noticeAuthorDirectoryPort.getAuthor(notice.getAuthorUserId());
        List<NoticeAttachmentView> attachments = notice.getAttachments().stream()
                .map(this::toAttachmentView)
                .toList();
        long readerCount = noticeReadRepository.countReaders(noticeId);
        long totalRecipientCount = noticeAuthorDirectoryPort.countActiveUsers(notice.getAcademyId());

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
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new NoticeException(NoticeErrorCode.NOTICE_NOT_FOUND));

        AuthorInfo requester = noticeAuthorDirectoryPort.getAuthor(requesterId);
        if (!notice.getAcademyId().equals(requester.academyId())) {
            throw new NoticeException(NoticeErrorCode.NOTICE_ACCESS_DENIED);
        }

        Map<Long, LocalDateTime> readTimestamps = noticeReadRepository.findReadTimestamps(noticeId);
        Map<Long, AuthorInfo> readers = noticeAuthorDirectoryPort.getAuthors(List.copyOf(readTimestamps.keySet()));

        return readTimestamps.entrySet().stream()
                .map(entry -> toReaderView(entry.getKey(), entry.getValue(), readers))
                .sorted((a, b) -> b.readAt().compareTo(a.readAt()))
                .toList();
    }

    private NoticeReaderView toReaderView(Long userId, LocalDateTime readAt, Map<Long, AuthorInfo> readers) {
        AuthorInfo reader = readers.get(userId);
        String name = reader != null ? reader.name() : null;
        String role = reader != null ? reader.role() : null;
        return new NoticeReaderView(userId, name, role, readAt);
    }

    private NoticeSummaryView toSummaryView(Notice notice, Long requesterId) {
        AuthorInfo author = noticeAuthorDirectoryPort.getAuthor(notice.getAuthorUserId());
        boolean read = noticeReadRepository.hasRead(notice.getId(), requesterId);

        return new NoticeSummaryView(
                notice.getId(),
                notice.getTitle(),
                author.name(),
                author.role(),
                notice.isPinned(),
                read,
                !notice.getAttachments().isEmpty(),
                notice.getCreatedAt()
        );
    }

    private NoticeAttachmentView toAttachmentView(NoticeAttachment attachment) {
        return new NoticeAttachmentView(attachment.getId(), attachment.getFileUrl(), attachment.getFileName(),
                attachment.getFileType());
    }
}
