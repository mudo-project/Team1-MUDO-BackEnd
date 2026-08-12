package com.academy.mudogroupware.users.application.usecase;

import com.academy.mudogroupware.users.application.result.MemberPage;
import com.academy.mudogroupware.users.domain.model.UserStatus;

public interface ListMembersUseCase {

    MemberPage list(String keyword, Long roleId, UserStatus status, int page, int size);
}
