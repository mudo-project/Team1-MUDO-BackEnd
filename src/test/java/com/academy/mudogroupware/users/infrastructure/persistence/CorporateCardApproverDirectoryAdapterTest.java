package com.academy.mudogroupware.users.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.corporatecard.application.port.CorporateCardApproverDirectoryPort.ApproverInfo;
import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.users.domain.model.Role;
import com.academy.mudogroupware.users.domain.model.User;
import com.academy.mudogroupware.users.domain.model.UserStatus;
import com.academy.mudogroupware.users.domain.repository.RoleRepository;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

class CorporateCardApproverDirectoryAdapterTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final CorporateCardApproverDirectoryAdapter adapter =
            new CorporateCardApproverDirectoryAdapter(userRepository, roleRepository);

    @Test
    void returnsApproverNameAndCurrentRoleName() {
        when(userRepository.findAllById(Set.of(10L))).thenReturn(List.of(user(10L, "김원장", 3L)));
        when(roleRepository.findAll()).thenReturn(List.of(
                Role.restore(3L, "원장", null, LocalDateTime.now(), Set.of())));

        var result = adapter.getApprovers(List.of(10L));

        assertThat(result).containsEntry(10L, new ApproverInfo(10L, "김원장", "원장"));
    }

    @Test
    void returnsNullPositionWhenApproverHasNoRole() {
        when(userRepository.findAllById(Set.of(10L))).thenReturn(List.of(user(10L, "김직원", null)));

        var result = adapter.getApprovers(List.of(10L));

        assertThat(result).containsEntry(10L, new ApproverInfo(10L, "김직원", null));
    }

    private User user(Long id, String name, Long roleId) {
        LocalDateTime now = LocalDateTime.now();
        return User.restore(id, "user" + id, "pw", name, "010", "user" + id + "@example.com",
                roleId, UserStatus.ACTIVE, false, AccountType.MEMBER, null, now, now, now);
    }
}
