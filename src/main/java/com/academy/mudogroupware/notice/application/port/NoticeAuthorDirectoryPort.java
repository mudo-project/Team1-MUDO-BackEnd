package com.academy.mudogroupware.notice.application.port;

import java.util.List;
import java.util.Map;

public interface NoticeAuthorDirectoryPort {

    AuthorInfo getAuthor(Long userId);

    Map<Long, AuthorInfo> getAuthors(List<Long> userIds);

    long countActiveUsers(Long academyId);
}
