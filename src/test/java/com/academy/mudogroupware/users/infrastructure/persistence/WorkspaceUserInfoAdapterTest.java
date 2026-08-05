package com.academy.mudogroupware.users.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.users.domain.model.User;
import com.academy.mudogroupware.users.domain.model.UserStatus;
import com.academy.mudogroupware.users.domain.repository.UserRepository;
import com.academy.mudogroupware.workspace.application.query.WorkspaceMemberInfo;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkspaceUserInfoAdapterTest {

  @Mock private UserRepository userRepository;

  @Test
  void mapsUsersToMinimalWorkspaceMemberInfo() {
    WorkspaceUserInfoAdapter adapter = new WorkspaceUserInfoAdapter(userRepository);
    User user =
        User.restore(
            10L, 1L, "u10", "pw", "김지수", "010", "a@a.com", 1L, UserStatus.ACTIVE, false, false,
            LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
    when(userRepository.findAllById(Set.of(10L))).thenReturn(List.of(user));

    List<WorkspaceMemberInfo> result = adapter.findUserInfo(Set.of(10L));

    assertThat(result).containsExactly(new WorkspaceMemberInfo(10L, "김지수"));
  }

  @Test
  void returnsEmptyListWithoutCallingRepositoryForEmptyIds() {
    WorkspaceUserInfoAdapter adapter = new WorkspaceUserInfoAdapter(userRepository);

    assertThat(adapter.findUserInfo(Set.of())).isEmpty();
  }
}
