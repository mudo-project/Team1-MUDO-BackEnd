package com.academy.mudogroupware.notice.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.academy.mudogroupware.global.domain.auth.RolePermissionInfo;
import com.academy.mudogroupware.global.domain.auth.RolePermissionLookupPort;
import com.academy.mudogroupware.notice.application.port.AuthorInfo;

class NoticeAuthorDirectoryPortAdapterTest {

    private final UserInfoJpaRepository userInfoJpaRepository = mock(UserInfoJpaRepository.class);
    private final RolePermissionLookupPort rolePermissionLookupPort = mock(RolePermissionLookupPort.class);
    private final NoticeAuthorDirectoryPortAdapter adapter =
            new NoticeAuthorDirectoryPortAdapter(userInfoJpaRepository, rolePermissionLookupPort);

    @Test
    void getAuthorResolvesRoleNameViaRolePermissionLookupPort() {
        UserInfoEntity entity = entity(7L, "김지수", 5L, 1L);
        when(userInfoJpaRepository.findById(7L)).thenReturn(Optional.of(entity));
        when(rolePermissionLookupPort.lookup(5L)).thenReturn(new RolePermissionInfo("대표", Set.of()));

        AuthorInfo author = adapter.getAuthor(7L);

        assertThat(author.role()).isEqualTo("대표");
        assertThat(author.academyId()).isEqualTo(1L);
    }

    @Test
    void getAuthorsLooksUpEachDistinctRoleIdOnlyOnce() {
        UserInfoEntity a = entity(1L, "이민준", 10L, 1L);
        UserInfoEntity b = entity(2L, "김지수", 10L, 1L);
        UserInfoEntity c = entity(3L, "박서준", 20L, 1L);
        when(userInfoJpaRepository.findAllById(List.of(1L, 2L, 3L))).thenReturn(List.of(a, b, c));
        when(rolePermissionLookupPort.lookup(10L)).thenReturn(new RolePermissionInfo("강사", Set.of()));
        when(rolePermissionLookupPort.lookup(20L)).thenReturn(new RolePermissionInfo("직원", Set.of()));

        Map<Long, AuthorInfo> authors = adapter.getAuthors(List.of(1L, 2L, 3L));

        assertThat(authors.get(1L).role()).isEqualTo("강사");
        assertThat(authors.get(2L).role()).isEqualTo("강사");
        assertThat(authors.get(3L).role()).isEqualTo("직원");
        verify(rolePermissionLookupPort, times(1)).lookup(10L);
        verify(rolePermissionLookupPort, times(1)).lookup(20L);
    }

    private UserInfoEntity entity(Long id, String name, Long roleId, Long academyId) {
        UserInfoEntity entity = new UserInfoEntity();
        ReflectionTestUtils.setField(entity, "id", id);
        ReflectionTestUtils.setField(entity, "name", name);
        ReflectionTestUtils.setField(entity, "roleId", roleId);
        ReflectionTestUtils.setField(entity, "academyId", academyId);
        ReflectionTestUtils.setField(entity, "status", "ACTIVE");
        return entity;
    }
}
