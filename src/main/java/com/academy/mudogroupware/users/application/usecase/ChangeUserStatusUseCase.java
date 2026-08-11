package com.academy.mudogroupware.users.application.usecase;

import com.academy.mudogroupware.users.domain.model.UserStatus;

public interface ChangeUserStatusUseCase {
    void changeStatus(Long academyId, Long userId, UserStatus status);
}
