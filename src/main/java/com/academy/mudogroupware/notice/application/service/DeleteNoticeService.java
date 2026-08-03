package com.academy.mudogroupware.notice.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.global.domain.common.exception.ForbiddenException;
import com.academy.mudogroupware.global.domain.common.exception.NotFoundException;
import com.academy.mudogroupware.notice.application.usecase.DeleteNoticeUseCase;
import com.academy.mudogroupware.notice.domain.model.Notice;
import com.academy.mudogroupware.notice.domain.repository.NoticeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DeleteNoticeService implements DeleteNoticeUseCase {

    private final NoticeRepository noticeRepository;

    // TODO: 삭제는 "작성자 본인 + 권한을 가진 사람들"도 가능해야 함 - users.role 값 체계 확정되면 추가
    @Override
    public void deleteNotice(Long noticeId, Long requesterId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new NotFoundException("공지사항을 찾을 수 없습니다."));

        if (!notice.isAuthor(requesterId)) {
            throw new ForbiddenException("작성자 본인만 공지사항을 삭제할 수 있습니다.");
        }

        noticeRepository.deleteById(noticeId);
    }
}
