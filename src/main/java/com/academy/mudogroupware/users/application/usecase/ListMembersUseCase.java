package com.academy.mudogroupware.users.application.usecase;

import java.util.List;

import com.academy.mudogroupware.users.application.result.MemberListItem;

public interface ListMembersUseCase {

    List<MemberListItem> list(Long academyId, String keyword);
}
