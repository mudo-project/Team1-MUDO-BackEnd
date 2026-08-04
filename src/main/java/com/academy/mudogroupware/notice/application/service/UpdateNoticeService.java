package com.academy.mudogroupware.notice.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.notice.application.command.UpdateNoticeCommand;
import com.academy.mudogroupware.notice.application.usecase.UpdateNoticeUseCase;
import com.academy.mudogroupware.notice.domain.exception.NoticeErrorCode;
import com.academy.mudogroupware.notice.domain.exception.NoticeException;
import com.academy.mudogroupware.notice.domain.model.Notice;
import com.academy.mudogroupware.notice.domain.repository.NoticeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateNoticeService implements UpdateNoticeUseCase {

    private final NoticeRepository noticeRepository;

    @Override
    public void updateNotice(UpdateNoticeCommand command) {
        Notice notice = noticeRepository.findById(command.noticeId())
                .orElseThrow(() -> new NoticeException(NoticeErrorCode.NOTICE_NOT_FOUND));

        if (!notice.isAuthor(command.requesterId())) {
            throw new NoticeException(NoticeErrorCode.NOT_AUTHOR_UPDATE);
        }

        notice.update(command.title(), command.content());
        noticeRepository.save(notice);
    }
}
