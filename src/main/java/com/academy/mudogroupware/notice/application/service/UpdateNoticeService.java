package com.academy.mudogroupware.notice.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.notice.application.command.UpdateNoticeCommand;
import com.academy.mudogroupware.notice.application.usecase.UpdateNoticeUseCase;
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
public class UpdateNoticeService implements UpdateNoticeUseCase {

    private final NoticeRepository noticeRepository;
    private final Clock clock;

    @Override
    public void updateNotice(UpdateNoticeCommand command) {
        log.info("event=notice_update_시작 noticeId={}, requesterId={}", command.noticeId(), command.requesterId());
        try {
            Notice notice = noticeRepository.findById(command.noticeId())
                    .orElseThrow(() -> new NoticeException(NoticeErrorCode.NOTICE_NOT_FOUND));

            if (!notice.isAuthor(command.requesterId())) {
                throw new NoticeException(NoticeErrorCode.NOT_AUTHOR_UPDATE);
            }

            notice.update(command.title(), command.content(), LocalDateTime.now(clock));
            noticeRepository.save(notice);
            log.info("event=notice_update_완료 noticeId={}, requesterId={}", command.noticeId(),
                    command.requesterId());
        } catch (RuntimeException e) {
            log.warn("event=notice_update_실패 noticeId={}, requesterId={}, reason={}", command.noticeId(),
                    command.requesterId(), e.getMessage());
            throw e;
        }
    }
}
