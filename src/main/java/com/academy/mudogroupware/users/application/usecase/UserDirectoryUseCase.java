package com.academy.mudogroupware.users.application.usecase;

import java.util.Set;

public interface UserDirectoryUseCase {

    Set<Long> findActiveUserIds(Long academyId, Set<Long> userIds);
}
