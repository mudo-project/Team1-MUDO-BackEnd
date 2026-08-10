package com.academy.mudogroupware.notice.infrastructure.persistence;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.global.domain.auth.RolePermissionLookupPort;
import com.academy.mudogroupware.notice.application.port.AuthorInfo;
import com.academy.mudogroupware.notice.application.port.NoticeAuthorDirectoryPort;
import com.academy.mudogroupware.notice.domain.exception.NoticeErrorCode;
import com.academy.mudogroupware.notice.domain.exception.NoticeException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NoticeAuthorDirectoryPortAdapter implements NoticeAuthorDirectoryPort {

    private static final String ACTIVE_STATUS = "ACTIVE";

    private final UserInfoJpaRepository userInfoJpaRepository;
    private final RolePermissionLookupPort rolePermissionLookupPort;

    @Override
    public AuthorInfo getAuthor(Long userId) {
        UserInfoEntity entity = userInfoJpaRepository.findById(userId)
                .orElseThrow(() -> new NoticeException(NoticeErrorCode.AUTHOR_NOT_FOUND));
        return toAuthorInfo(entity, rolePermissionLookupPort.lookup(entity.getRoleId()).roleName());
    }

    @Override
    public Map<Long, AuthorInfo> getAuthors(List<Long> userIds) {
        List<UserInfoEntity> entities = userInfoJpaRepository.findAllById(userIds);

        Map<Long, String> roleNamesByRoleId = entities.stream()
                .map(UserInfoEntity::getRoleId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toMap(Function.identity(),
                        roleId -> rolePermissionLookupPort.lookup(roleId).roleName()));

        return entities.stream()
                .map(entity -> toAuthorInfo(entity, roleNamesByRoleId.get(entity.getRoleId())))
                .collect(Collectors.toMap(AuthorInfo::userId, Function.identity()));
    }

    @Override
    public long countActiveUsers(Long academyId) {
        return userInfoJpaRepository.countByAcademyIdAndStatus(academyId, ACTIVE_STATUS);
    }

    private AuthorInfo toAuthorInfo(UserInfoEntity entity, String roleName) {
        return new AuthorInfo(entity.getId(), entity.getName(), roleName, entity.getAcademyId());
    }
}
