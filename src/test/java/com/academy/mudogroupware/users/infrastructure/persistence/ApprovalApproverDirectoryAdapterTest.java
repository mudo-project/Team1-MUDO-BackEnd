package com.academy.mudogroupware.users.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.approval.application.port.ApproverInfo;
import com.academy.mudogroupware.approval.domain.exception.ApprovalErrorCode;
import com.academy.mudogroupware.approval.domain.exception.ApprovalException;
import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.users.domain.model.User;
import com.academy.mudogroupware.users.domain.model.UserStatus;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

class ApprovalApproverDirectoryAdapterTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final ApprovalApproverDirectoryAdapter adapter = new ApprovalApproverDirectoryAdapter(userRepository);

    @Test
    void returnsApproverInfoByUserId() {
        when(userRepository.findById(10L)).thenReturn(java.util.Optional.of(user(10L, "Approver Lee")));

        ApproverInfo result = adapter.getApprover(10L);

        assertThat(result).isEqualTo(new ApproverInfo(10L, "Approver Lee"));
    }

    @Test
    void throwsApprovalExceptionWhenApproverNotFound() {
        when(userRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> adapter.getApprover(999L))
                .isInstanceOf(ApprovalException.class)
                .extracting(e -> ((ApprovalException) e).getErrorCode())
                .isEqualTo(ApprovalErrorCode.APPROVER_NOT_FOUND);
    }

    @Test
    void returnsApproverMapForRequestedIds() {
        when(userRepository.findAllById(Set.of(10L, 20L))).thenReturn(List.of(
                user(10L, "Approver Lee"),
                user(20L, "Approver Park")));

        Map<Long, ApproverInfo> result = adapter.getApprovers(List.of(10L, 20L));

        assertThat(result).containsEntry(10L, new ApproverInfo(10L, "Approver Lee"));
        assertThat(result).containsEntry(20L, new ApproverInfo(20L, "Approver Park"));
    }

    private User user(Long id, String name) {
        return User.restore(id, "user" + id, "pw", name, "010", "user" + id + "@example.com",
                1L, UserStatus.ACTIVE, false, AccountType.MEMBER, null, LocalDateTime.now(), LocalDateTime.now(),
                LocalDateTime.now());
    }
}
