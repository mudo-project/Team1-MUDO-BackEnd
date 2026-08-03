package com.academy.mudogroupware.notice.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.notice.application.command.UpdateNoticeCommand;
import com.academy.mudogroupware.notice.application.usecase.UpdateNoticeUseCase;
import com.academy.mudogroupware.notice.domain.model.Notice;
import com.academy.mudogroupware.notice.domain.repository.NoticeRepository;
import com.academy.mudogroupware.global.domain.common.exception.ForbiddenException;
import com.academy.mudogroupware.global.domain.common.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateNoticeService implements UpdateNoticeUseCase {

    private final NoticeRepository noticeRepository;

    @Override
    public void updateNotice(UpdateNoticeCommand command) {
        Notice notice = noticeRepository.findById(command.noticeId())
                .orElseThrow(() -> new NotFoundException("공지사항을 찾을 수 없습니다."));

        if (!notice.isAuthor(command.requesterId())) {
            throw new ForbiddenException("작성자 본인만 공지사항을 수정할 수 있습니다.");
        }

        notice.update(command.title(), command.content());
        noticeRepository.save(notice);
    }
}
