package com.academy.mudogroupware.users.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.users.application.result.MemberListItem;
import com.academy.mudogroupware.users.domain.model.Role;
import com.academy.mudogroupware.users.domain.model.User;
import com.academy.mudogroupware.users.domain.model.UserStatus;
import com.academy.mudogroupware.users.domain.repository.RoleRepository;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

class ListMembersServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final ListMembersService service = new ListMembersService(userRepository, roleRepository);

    private User user(long id, String name, Long roleId, UserStatus status) {
        LocalDateTime now = LocalDateTime.now();
        return User.restore(id, 1L, "user" + id, "hash", name, "010-0000-0000", name + "@example.com", roleId,
                status, false, AccountType.MEMBER, null, now, now, now);
    }

    @Test
    void listsAllMembersWithRoleNameWhenNoKeyword() {
        when(userRepository.findAllByAcademyId(1L)).thenReturn(List.of(
                user(1L, "김강사", 8L, UserStatus.ACTIVE),
                user(2L, "이조교", 9L, UserStatus.RESIGNED)));
        when(roleRepository.findAllByAcademyId(1L)).thenReturn(List.of(
                Role.restore(8L, 1L, "강사", null, LocalDateTime.now(), Set.of()),
                Role.restore(9L, 1L, "조교", null, LocalDateTime.now(), Set.of())));

        List<MemberListItem> result = service.list(1L, null);

        assertThat(result).extracting(MemberListItem::name, MemberListItem::roleName, MemberListItem::status)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("김강사", "강사", UserStatus.ACTIVE),
                        org.assertj.core.groups.Tuple.tuple("이조교", "조교", UserStatus.RESIGNED));
    }

    @Test
    void filtersByNameKeyword() {
        when(userRepository.findAllByAcademyId(1L)).thenReturn(List.of(
                user(1L, "김강사", 8L, UserStatus.ACTIVE),
                user(2L, "이조교", 9L, UserStatus.ACTIVE)));
        when(roleRepository.findAllByAcademyId(1L)).thenReturn(List.of(
                Role.restore(8L, 1L, "강사", null, LocalDateTime.now(), Set.of()),
                Role.restore(9L, 1L, "조교", null, LocalDateTime.now(), Set.of())));

        List<MemberListItem> result = service.list(1L, "김강사");

        assertThat(result).extracting(MemberListItem::name).containsExactly("김강사");
    }

    @Test
    void filtersByRoleNameKeyword() {
        when(userRepository.findAllByAcademyId(1L)).thenReturn(List.of(
                user(1L, "김강사", 8L, UserStatus.ACTIVE),
                user(2L, "이조교", 9L, UserStatus.ACTIVE)));
        when(roleRepository.findAllByAcademyId(1L)).thenReturn(List.of(
                Role.restore(8L, 1L, "강사", null, LocalDateTime.now(), Set.of()),
                Role.restore(9L, 1L, "조교", null, LocalDateTime.now(), Set.of())));

        List<MemberListItem> result = service.list(1L, "조교");

        assertThat(result).extracting(MemberListItem::name).containsExactly("이조교");
    }

    @Test
    void returnsEmptyListWhenNoKeywordMatch() {
        when(userRepository.findAllByAcademyId(1L)).thenReturn(List.of(user(1L, "김강사", 8L, UserStatus.ACTIVE)));
        when(roleRepository.findAllByAcademyId(1L)).thenReturn(List.of(
                Role.restore(8L, 1L, "강사", null, LocalDateTime.now(), Set.of())));

        List<MemberListItem> result = service.list(1L, "존재하지않는이름");

        assertThat(result).isEmpty();
    }

    @Test
    void handlesUserWithNullRoleIdGracefully() {
        when(userRepository.findAllByAcademyId(1L)).thenReturn(List.of(user(1L, "김원장", null, UserStatus.ACTIVE)));
        when(roleRepository.findAllByAcademyId(1L)).thenReturn(List.of());

        List<MemberListItem> result = service.list(1L, null);

        assertThat(result).extracting(MemberListItem::name, MemberListItem::roleName)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("김원장", null));
    }
}
