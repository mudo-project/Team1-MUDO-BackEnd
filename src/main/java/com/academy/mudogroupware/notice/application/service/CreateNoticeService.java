package com.academy.mudogroupware.notice.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.notice.application.command.CreateNoticeCommand;
import com.academy.mudogroupware.notice.application.command.NoticeAttachmentInput;
import com.academy.mudogroupware.notice.application.port.AuthorInfo;
import com.academy.mudogroupware.notice.application.port.NoticeAuthorDirectoryPort;
import com.academy.mudogroupware.notice.application.usecase.CreateNoticeUseCase;
import com.academy.mudogroupware.notice.domain.model.Notice;
import com.academy.mudogroupware.notice.domain.model.NoticeAttachment;
import com.academy.mudogroupware.notice.domain.repository.NoticeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CreateNoticeService implements CreateNoticeUseCase {

    private final NoticeRepository noticeRepository;
    private final NoticeAuthorDirectoryPort noticeAuthorDirectoryPort;
    private final Clock clock;

    @Override
    public Long createNotice(CreateNoticeCommand command) {
        log.info("event=notice_create_시작 authorUserId={}", command.authorUserId());
        AuthorInfo author = noticeAuthorDirectoryPort.getAuthor(command.authorUserId());

        List<NoticeAttachment> attachments = command.attachments() == null
                ? List.of()
                : command.attachments().stream().map(this::toAttachment).toList();

        Notice notice = Notice.create(author.academyId(), command.authorUserId(), command.title(),
                command.content(), command.pinned(), attachments, LocalDateTime.now(clock));

        Long noticeId = noticeRepository.save(notice).getId();
        log.info("event=notice_create_완료 authorUserId={}, noticeId={}", command.authorUserId(), noticeId);
        return noticeId;
    }

    private NoticeAttachment toAttachment(NoticeAttachmentInput input) {
        return NoticeAttachment.create(input.fileId(), input.fileName());
    }
}
