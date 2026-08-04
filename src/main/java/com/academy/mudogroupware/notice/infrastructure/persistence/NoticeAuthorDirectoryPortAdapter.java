package com.academy.mudogroupware.notice.infrastructure.persistence;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.global.domain.common.exception.NotFoundException;
import com.academy.mudogroupware.notice.application.port.AuthorInfo;
import com.academy.mudogroupware.notice.application.port.NoticeAuthorDirectoryPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NoticeAuthorDirectoryPortAdapter implements NoticeAuthorDirectoryPort {

    private static final String ACTIVE_STATUS = "ACTIVE";

    private final UserInfoJpaRepository userInfoJpaRepository;

    @Override
    public AuthorInfo getAuthor(Long userId) {
        UserInfoEntity entity = userInfoJpaRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        return toAuthorInfo(entity);
    }

    @Override
    public Map<Long, AuthorInfo> getAuthors(List<Long> userIds) {
        return userInfoJpaRepository.findAllById(userIds).stream()
                .map(this::toAuthorInfo)
                .collect(Collectors.toMap(AuthorInfo::userId, Function.identity()));
    }

    @Override
    public long countActiveUsers(Long academyId) {
        return userInfoJpaRepository.countByAcademyIdAndStatus(academyId, ACTIVE_STATUS);
    }

    private AuthorInfo toAuthorInfo(UserInfoEntity entity) {
        return new AuthorInfo(entity.getId(), entity.getName(), entity.getRole(), entity.getAcademyId());
    }
}
