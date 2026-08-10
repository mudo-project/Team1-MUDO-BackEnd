package com.academy.mudogroupware.notice.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.notice.application.usecase.DeleteNoticeUseCase;
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
public class DeleteNoticeService implements DeleteNoticeUseCase {

    private final NoticeRepository noticeRepository;

    @Override
    public void deleteNotice(Long noticeId, Long requesterId) {
        log.info("event=notice_delete_시작 noticeId={}, requesterId={}", noticeId, requesterId);
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new NoticeException(NoticeErrorCode.NOTICE_NOT_FOUND));

        if (!notice.isAuthor(requesterId)) {
            throw new NoticeException(NoticeErrorCode.NOT_AUTHOR_DELETE);
        }

        noticeRepository.deleteById(noticeId);
        log.info("event=notice_delete_완료 noticeId={}, requesterId={}", noticeId, requesterId);
    }
}
