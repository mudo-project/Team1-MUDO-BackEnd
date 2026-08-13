package com.academy.mudogroupware.users.application.usecase;

import java.util.List;

import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.global.domain.auth.AdminScope;

public interface GetMyPermissionsUseCase {
    List<String> getMyPermissions(Long roleId, AccountType accountType, AdminScope adminScope);
}
