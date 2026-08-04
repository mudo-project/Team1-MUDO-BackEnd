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

@Service
@RequiredArgsConstructor
@Transactional
public class PinNoticeService implements PinNoticeUseCase {

    private final NoticeRepository noticeRepository;
    private final NoticeAuthorDirectoryPort noticeAuthorDirectoryPort;

    @Override
    public void pin(Long noticeId, Long requesterId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new NoticeException(NoticeErrorCode.NOTICE_NOT_FOUND));

        if (!notice.isAuthor(requesterId)) {
            throw new NoticeException(NoticeErrorCode.NOT_AUTHOR_PIN);
        }

        notice.pin();
        noticeRepository.save(notice);
    }

    // TODO: 고정 해제는 "권한을 가진 사람들 모두" 가능해야 함 - users.role 값 체계 확정되면 작성자 제한을 완화
    // 지금은 같은 학원 소속이면 누구나 해제 가능 (최소한의 테넌시 격리만 적용, 저자 제한은 아직 없음)
    @Override
    public void unpin(Long noticeId, Long requesterId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new NoticeException(NoticeErrorCode.NOTICE_NOT_FOUND));

        AuthorInfo requester = noticeAuthorDirectoryPort.getAuthor(requesterId);
        if (!notice.getAcademyId().equals(requester.academyId())) {
            throw new NoticeException(NoticeErrorCode.CROSS_ACADEMY_NOTICE);
        }

        notice.unpin();
        noticeRepository.save(notice);
    }
}
