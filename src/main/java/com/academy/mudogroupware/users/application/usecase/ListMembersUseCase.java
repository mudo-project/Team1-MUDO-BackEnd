package com.academy.mudogroupware.users.application.usecase;

import com.academy.mudogroupware.users.application.result.MemberPage;

public interface ListMembersUseCase {

    MemberPage list(String keyword, Long roleId, int page, int size);
}
