package com.academy.mudogroupware.users.infrastructure.payroll;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.global.domain.auth.AdminScope;
import com.academy.mudogroupware.payroll.application.port.out.PayrollEmployeePort;
import com.academy.mudogroupware.users.domain.model.UserStatus;
import com.academy.mudogroupware.users.infrastructure.persistence.UserEntity;
import com.academy.mudogroupware.users.infrastructure.persistence.UserJpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PayrollEmployeeAdapterTest {
  private final UserJpaRepository repository = mock(UserJpaRepository.class);
  private final PayrollEmployeeAdapter adapter = new PayrollEmployeeAdapter(repository);

  @Test
  void 직원들을_아이디로_한번에_조회한다() {
    UserEntity first = employee(10L, "첫째", "first@example.com", UserStatus.ACTIVE);
    UserEntity resigned = employee(11L, "둘째", "second@example.com", UserStatus.RESIGNED);
    when(repository.findAllById(Set.of(10L, 11L))).thenReturn(List.of(first, resigned));

    var result = adapter.findByIds(Set.of(10L, 11L));

    assertThat(result).containsOnlyKeys(10L, 11L);
    assertThat(result.get(11L).name()).isEqualTo("둘째");
    verify(repository).findAllById(Set.of(10L, 11L));
  }

  @Test
  void 아이디가_없으면_저장소를_조회하지_않는다() {
    assertThat(adapter.findByIds(Set.of())).isEmpty();

    verifyNoInteractions(repository);
  }

  @Test
  void 활성_직원_목록에서_플랫폼_슈퍼관리자만_제외한다() {
    UserEntity member = employee(10L, "직원", "member@example.com", UserStatus.ACTIVE,
        AccountType.MEMBER, null);
    UserEntity academyAdmin = employee(11L, "원장", "owner@example.com", UserStatus.ACTIVE,
        AccountType.ADMIN, AdminScope.ACADEMY);
    UserEntity platformAdmin = employee(12L, "슈퍼관리자", "admin@example.com", UserStatus.ACTIVE,
        AccountType.ADMIN, AdminScope.PLATFORM);
    when(repository.findAllByStatus(UserStatus.ACTIVE))
        .thenReturn(List.of(member, academyAdmin, platformAdmin));

    var result = adapter.findAllActive(null);

    assertThat(result).extracting(PayrollEmployeePort.EmployeeView::id)
        .containsExactly(10L, 11L);
  }

  private UserEntity employee(Long id, String name, String email, UserStatus status) {
    return employee(id, name, email, status, AccountType.MEMBER, null);
  }

  private UserEntity employee(Long id, String name, String email, UserStatus status,
      AccountType accountType, AdminScope adminScope) {
    LocalDateTime now = LocalDateTime.now();
    return UserEntity.builder()
        .id(id)
        .username("user" + id)
        .password("password")
        .name(name)
        .email(email)
        .status(status)
        .accountType(accountType)
        .adminScope(adminScope)
        .joinedAt(now)
        .createdAt(now)
        .updatedAt(now)
        .build();
  }
}
