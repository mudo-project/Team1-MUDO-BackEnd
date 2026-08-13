package com.academy.mudogroupware.users.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.global.domain.auth.AdminScope;
import com.academy.mudogroupware.global.domain.auth.EffectivePermissionResolver;
import com.academy.mudogroupware.users.application.usecase.GetMyPermissionsUseCase;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetMyPermissionsService implements GetMyPermissionsUseCase {

    private final EffectivePermissionResolver effectivePermissionResolver;

    @Override
    public List<String> getMyPermissions(Long roleId, AccountType accountType, AdminScope adminScope) {
        return effectivePermissionResolver.resolve(roleId, accountType, adminScope);
    }
}
