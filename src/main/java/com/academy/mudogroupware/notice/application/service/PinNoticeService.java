package com.academy.mudogroupware.notice.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.global.domain.common.exception.ForbiddenException;
import com.academy.mudogroupware.global.domain.common.exception.NotFoundException;
import com.academy.mudogroupware.notice.application.usecase.PinNoticeUseCase;
import com.academy.mudogroupware.notice.domain.model.Notice;
import com.academy.mudogroupware.notice.domain.repository.NoticeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PinNoticeService implements PinNoticeUseCase {

    private final NoticeRepository noticeRepository;

    @Override
    public void pin(Long noticeId, Long requesterId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new NotFoundException("공지사항을 찾을 수 없습니다."));

        if (!notice.isAuthor(requesterId)) {
            throw new ForbiddenException("작성자 본인만 공지사항을 고정할 수 있습니다.");
        }

        notice.pin();
        noticeRepository.save(notice);
    }

    // TODO: 고정 해제는 "권한을 가진 사람들 모두" 가능해야 함 - users.role 값 체계 확정되면 작성자 제한을 완화
    @Override
    public void unpin(Long noticeId, Long requesterId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new NotFoundException("공지사항을 찾을 수 없습니다."));

        notice.unpin();
        noticeRepository.save(notice);
    }
}
