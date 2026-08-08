package com.academy.mudogroupware.notice.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.notice.application.port.AuthorInfo;
import com.academy.mudogroupware.notice.application.port.NoticeAuthorDirectoryPort;
import com.academy.mudogroupware.notice.application.usecase.PinNoticeUseCase;
import com.academy.mudogroupware.notice.domain.exception.NoticeErrorCode;
import com.academy.mudogroupware.notice.domain.exception.NoticeException;
import com.academy.mudogroupware.notice.domain.model.Notice;
import com.academy.mudogroupware.notice.domain.repository.NoticeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PinNoticeService implements PinNoticeUseCase {

    private final NoticeRepository noticeRepository;
    private final NoticeAuthorDirectoryPort noticeAuthorDirectoryPort;

    @Override
    public void pin(Long noticeId, Long requesterId) {
        log.info("event=notice_pin_시작 noticeId={}, requesterId={}", noticeId, requesterId);
        try {
            Notice notice = noticeRepository.findById(noticeId)
                    .orElseThrow(() -> new NoticeException(NoticeErrorCode.NOTICE_NOT_FOUND));

            AuthorInfo requester = noticeAuthorDirectoryPort.getAuthor(requesterId);
            if (!notice.getAcademyId().equals(requester.academyId())) {
                throw new NoticeException(NoticeErrorCode.CROSS_ACADEMY_NOTICE);
            }

            notice.pin();
            noticeRepository.save(notice);
            log.info("event=notice_pin_완료 noticeId={}, requesterId={}", noticeId, requesterId);
        } catch (RuntimeException e) {
            log.warn("event=notice_pin_실패 noticeId={}, requesterId={}, reason={}", noticeId, requesterId,
                    e.getMessage());
            throw e;
        }
    }

    @Override
    public void unpin(Long noticeId, Long requesterId) {
        log.info("event=notice_unpin_시작 noticeId={}, requesterId={}", noticeId, requesterId);
        try {
            Notice notice = noticeRepository.findById(noticeId)
                    .orElseThrow(() -> new NoticeException(NoticeErrorCode.NOTICE_NOT_FOUND));

            AuthorInfo requester = noticeAuthorDirectoryPort.getAuthor(requesterId);
            if (!notice.getAcademyId().equals(requester.academyId())) {
                throw new NoticeException(NoticeErrorCode.CROSS_ACADEMY_NOTICE);
            }

            notice.unpin();
            noticeRepository.save(notice);
            log.info("event=notice_unpin_완료 noticeId={}, requesterId={}", noticeId, requesterId);
        } catch (RuntimeException e) {
            log.warn("event=notice_unpin_실패 noticeId={}, requesterId={}, reason={}", noticeId, requesterId,
                    e.getMessage());
            throw e;
        }
    }
}
