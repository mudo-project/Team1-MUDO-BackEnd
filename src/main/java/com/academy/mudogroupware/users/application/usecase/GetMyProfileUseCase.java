package com.academy.mudogroupware.users.application.usecase;

import com.academy.mudogroupware.users.application.result.UserDetailResult;

public interface GetMyProfileUseCase {
    UserDetailResult getMyProfile(Long userId);
}
