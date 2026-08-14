package com.academy.mudogroupware.file.infrastructure.notice;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.academy.mudogroupware.file.application.service.DeleteUnreferencedFilesService;
import com.academy.mudogroupware.notice.domain.event.NoticeAttachmentFilesCleanupRequestedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NoticeAttachmentFileCleanupListener {

    private final DeleteUnreferencedFilesService deleteUnreferencedFilesService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(NoticeAttachmentFilesCleanupRequestedEvent event) {
        deleteUnreferencedFilesService.deleteUnreferencedFiles(event.fileIds());
    }
}
