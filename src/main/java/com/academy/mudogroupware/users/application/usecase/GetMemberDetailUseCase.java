package com.academy.mudogroupware.users.application.usecase;

import com.academy.mudogroupware.users.application.result.UserDetailResult;

public interface GetMemberDetailUseCase {
    UserDetailResult getMemberDetail(Long academyId, Long userId);
}
