package com.academy.mudogroupware.users.application.usecase;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.users.application.result.MemberListItem;

public interface ListMembersUseCase {

    PageResult<MemberListItem> list(Long academyId, String keyword, Long roleId, int page, int size);
}
