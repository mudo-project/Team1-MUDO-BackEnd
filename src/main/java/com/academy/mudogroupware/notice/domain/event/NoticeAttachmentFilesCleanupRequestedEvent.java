package com.academy.mudogroupware.notice.domain.event;

import java.util.List;

public record NoticeAttachmentFilesCleanupRequestedEvent(List<Long> fileIds) {

    public NoticeAttachmentFilesCleanupRequestedEvent {
        fileIds = fileIds == null ? List.of() : List.copyOf(fileIds);
    }
}
