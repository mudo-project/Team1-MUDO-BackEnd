package com.academy.mudogroupware.users.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.users.application.port.MemberTodayAttendanceStatus;
import com.academy.mudogroupware.users.application.port.TodayAttendanceStatusPort;
import com.academy.mudogroupware.users.application.result.MemberListItem;
import com.academy.mudogroupware.users.application.result.MemberPage;
import com.academy.mudogroupware.users.domain.model.Role;
import com.academy.mudogroupware.users.domain.model.User;
import com.academy.mudogroupware.users.domain.model.UserStatus;
import com.academy.mudogroupware.users.domain.repository.RoleRepository;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

class ListMembersServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final TodayAttendanceStatusPort todayAttendanceStatusPort = mock(TodayAttendanceStatusPort.class);
    private final ListMembersService service =
            new ListMembersService(userRepository, roleRepository, todayAttendanceStatusPort);

    private User user(long id, String name, Long roleId, UserStatus status) {
        LocalDateTime now = LocalDateTime.now();
        return User.restore(id, "user" + id, "hash", name, "010-0000-0000", name + "@example.com", roleId,
                status, false, AccountType.MEMBER, null, now, now, now);
    }

    @Test
    void listsAllMembersSortedByRoleNameThenNameWhenNoFilters() {
        when(userRepository.findAll()).thenReturn(List.of(
                user(1L, "김강사", 8L, UserStatus.ACTIVE),
                user(2L, "이조교", 9L, UserStatus.RESIGNED),
                user(3L, "박강사", 8L, UserStatus.ACTIVE)));
        when(roleRepository.findAll()).thenReturn(List.of(
                Role.restore(8L, "강사", null, LocalDateTime.now(), Set.of()),
                Role.restore(9L, "조교", null, LocalDateTime.now(), Set.of())));
        when(todayAttendanceStatusPort.findTodayStatusByUserIds(any())).thenReturn(List.of());

        MemberPage result = service.list(null, null, 0, 20);

        assertThat(result.content()).extracting(MemberListItem::name)
                .containsExactly("김강사", "박강사", "이조교");
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    void filtersByNameKeyword() {
        when(userRepository.findAll()).thenReturn(List.of(
                user(1L, "김강사", 8L, UserStatus.ACTIVE),
                user(2L, "이조교", 9L, UserStatus.ACTIVE)));
        when(roleRepository.findAll()).thenReturn(List.of(
                Role.restore(8L, "강사", null, LocalDateTime.now(), Set.of()),
                Role.restore(9L, "조교", null, LocalDateTime.now(), Set.of())));
        when(todayAttendanceStatusPort.findTodayStatusByUserIds(any())).thenReturn(List.of());

        MemberPage result = service.list("김강사", null, 0, 20);

        assertThat(result.content()).extracting(MemberListItem::name).containsExactly("김강사");
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void filtersByRoleNameKeyword() {
        when(userRepository.findAll()).thenReturn(List.of(
                user(1L, "김강사", 8L, UserStatus.ACTIVE),
                user(2L, "이조교", 9L, UserStatus.ACTIVE)));
        when(roleRepository.findAll()).thenReturn(List.of(
                Role.restore(8L, "강사", null, LocalDateTime.now(), Set.of()),
                Role.restore(9L, "조교", null, LocalDateTime.now(), Set.of())));
        when(todayAttendanceStatusPort.findTodayStatusByUserIds(any())).thenReturn(List.of());

        MemberPage result = service.list("조교", null, 0, 20);

        assertThat(result.content()).extracting(MemberListItem::name).containsExactly("이조교");
    }

    @Test
    void filtersByRoleId() {
        when(userRepository.findAll()).thenReturn(List.of(
                user(1L, "김강사", 8L, UserStatus.ACTIVE),
                user(2L, "이조교", 9L, UserStatus.ACTIVE)));
        when(roleRepository.findAll()).thenReturn(List.of(
                Role.restore(8L, "강사", null, LocalDateTime.now(), Set.of()),
                Role.restore(9L, "조교", null, LocalDateTime.now(), Set.of())));
        when(todayAttendanceStatusPort.findTodayStatusByUserIds(any())).thenReturn(List.of());

        MemberPage result = service.list(null, 9L, 0, 20);

        assertThat(result.content()).extracting(MemberListItem::name).containsExactly("이조교");
    }

    @Test
    void paginatesResultsAndReportsHasNextCorrectly() {
        when(userRepository.findAll()).thenReturn(List.of(
                user(1L, "김강사", 8L, UserStatus.ACTIVE),
                user(2L, "박강사", 8L, UserStatus.ACTIVE),
                user(3L, "이조교", 9L, UserStatus.ACTIVE)));
        when(roleRepository.findAll()).thenReturn(List.of(
                Role.restore(8L, "강사", null, LocalDateTime.now(), Set.of()),
                Role.restore(9L, "조교", null, LocalDateTime.now(), Set.of())));
        when(todayAttendanceStatusPort.findTodayStatusByUserIds(any())).thenReturn(List.of());

        MemberPage firstPage = service.list(null, null, 0, 2);
        MemberPage secondPage = service.list(null, null, 1, 2);

        assertThat(firstPage.content()).extracting(MemberListItem::name).containsExactly("김강사", "박강사");
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(firstPage.totalElements()).isEqualTo(3);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(secondPage.content()).extracting(MemberListItem::name).containsExactly("이조교");
        assertThat(secondPage.hasNext()).isFalse();
        assertThat(secondPage.totalElements()).isEqualTo(3);
        assertThat(secondPage.totalPages()).isEqualTo(2);
    }

    @Test
    void computesTotalPagesAsCeilingOfTotalElementsOverSize() {
        when(userRepository.findAll()).thenReturn(List.of(
                user(1L, "김강사", 8L, UserStatus.ACTIVE),
                user(2L, "박강사", 8L, UserStatus.ACTIVE),
                user(3L, "이조교", 9L, UserStatus.ACTIVE),
                user(4L, "최조교", 9L, UserStatus.ACTIVE),
                user(5L, "정조교", 9L, UserStatus.ACTIVE)));
        when(roleRepository.findAll()).thenReturn(List.of(
                Role.restore(8L, "강사", null, LocalDateTime.now(), Set.of()),
                Role.restore(9L, "조교", null, LocalDateTime.now(), Set.of())));
        when(todayAttendanceStatusPort.findTodayStatusByUserIds(any())).thenReturn(List.of());

        MemberPage result = service.list(null, null, 0, 2);

        assertThat(result.totalElements()).isEqualTo(5);
        assertThat(result.totalPages()).isEqualTo(3);
    }

    @Test
    void returnsEmptyListWhenNoKeywordMatch() {
        when(userRepository.findAll()).thenReturn(List.of(user(1L, "김강사", 8L, UserStatus.ACTIVE)));
        when(roleRepository.findAll()).thenReturn(List.of(
                Role.restore(8L, "강사", null, LocalDateTime.now(), Set.of())));
        when(todayAttendanceStatusPort.findTodayStatusByUserIds(any())).thenReturn(List.of());

        MemberPage result = service.list("존재하지않는이름", null, 0, 20);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(0);
        assertThat(result.totalPages()).isEqualTo(0);
    }

    @Test
    void handlesUserWithNullRoleIdGracefully() {
        when(userRepository.findAll()).thenReturn(List.of(user(1L, "김원장", null, UserStatus.ACTIVE)));
        when(roleRepository.findAll()).thenReturn(List.of());
        when(todayAttendanceStatusPort.findTodayStatusByUserIds(any())).thenReturn(List.of());

        MemberPage result = service.list(null, null, 0, 20);

        assertThat(result.content()).extracting(MemberListItem::name, MemberListItem::roleName)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("김원장", null));
    }

    @Test
    void fillsAttendanceStatusForActiveMembersOnlyAndNullForOthers() {
        when(userRepository.findAll()).thenReturn(List.of(
                user(1L, "김강사", 8L, UserStatus.ACTIVE),
                user(2L, "이조교", 9L, UserStatus.RESIGNED)));
        when(roleRepository.findAll()).thenReturn(List.of(
                Role.restore(8L, "강사", null, LocalDateTime.now(), Set.of()),
                Role.restore(9L, "조교", null, LocalDateTime.now(), Set.of())));
        when(todayAttendanceStatusPort.findTodayStatusByUserIds(any())).thenReturn(List.of(
                new MemberTodayAttendanceStatus(1L, "PRESENT"),
                new MemberTodayAttendanceStatus(2L, "SHOULD_BE_IGNORED")));

        MemberPage result = service.list(null, null, 0, 20);

        assertThat(result.content()).extracting(MemberListItem::userId, MemberListItem::attendanceStatus)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(1L, "PRESENT"),
                        org.assertj.core.groups.Tuple.tuple(2L, null));
    }

    @Test
    void doesNotOverflowWhenPageIsVeryLarge() {
        when(userRepository.findAll()).thenReturn(List.of(
                user(1L, "김강사", 8L, UserStatus.ACTIVE)));
        when(roleRepository.findAll()).thenReturn(List.of(
                Role.restore(8L, "강사", null, LocalDateTime.now(), Set.of())));
        when(todayAttendanceStatusPort.findTodayStatusByUserIds(any())).thenReturn(List.of());

        MemberPage result = service.list(null, null, Integer.MAX_VALUE, 100);

        assertThat(result.content()).isEmpty();
        assertThat(result.hasNext()).isFalse();
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    void sortsMembersWithSameRoleAndNameDeterministicallyByUserId() {
        when(userRepository.findAll()).thenReturn(List.of(
                user(2L, "김강사", 8L, UserStatus.ACTIVE),
                user(1L, "김강사", 8L, UserStatus.ACTIVE)));
        when(roleRepository.findAll()).thenReturn(List.of(
                Role.restore(8L, "강사", null, LocalDateTime.now(), Set.of())));
        when(todayAttendanceStatusPort.findTodayStatusByUserIds(any())).thenReturn(List.of());

        MemberPage result = service.list(null, null, 0, 20);

        assertThat(result.content()).extracting(MemberListItem::userId).containsExactly(1L, 2L);
    }

    @Test
    void queriesAttendanceStatusOnlyForUsersInTheCurrentPage() {
        when(userRepository.findAll()).thenReturn(List.of(
                user(1L, "김강사", 8L, UserStatus.ACTIVE),
                user(2L, "박강사", 8L, UserStatus.ACTIVE),
                user(3L, "이조교", 9L, UserStatus.ACTIVE)));
        when(roleRepository.findAll()).thenReturn(List.of(
                Role.restore(8L, "강사", null, LocalDateTime.now(), Set.of()),
                Role.restore(9L, "조교", null, LocalDateTime.now(), Set.of())));
        when(todayAttendanceStatusPort.findTodayStatusByUserIds(any())).thenReturn(List.of());

        service.list(null, null, 0, 2);

        verify(todayAttendanceStatusPort).findTodayStatusByUserIds(List.of(1L, 2L));
    }
}
