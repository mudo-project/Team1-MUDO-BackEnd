package com.academy.mudogroupware.notice.application.retention;

import java.util.List;

public record NoticeRetentionTarget(Long noticeId, List<Long> fileIds) {

    public NoticeRetentionTarget {
        fileIds = fileIds == null ? List.of() : List.copyOf(fileIds);
    }
}
