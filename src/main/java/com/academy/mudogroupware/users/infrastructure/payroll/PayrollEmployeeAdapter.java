package com.academy.mudogroupware.users.infrastructure.payroll;

import com.academy.mudogroupware.payroll.application.port.out.PayrollEmployeePort;
import com.academy.mudogroupware.users.domain.model.UserStatus;
import com.academy.mudogroupware.users.infrastructure.persistence.UserEntity;
import com.academy.mudogroupware.users.infrastructure.persistence.UserJpaRepository;
import java.util.Optional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PayrollEmployeeAdapter implements PayrollEmployeePort {
  private final UserJpaRepository repository;

  /** Consumer: payroll. Purpose: 월 급여 생성, 조회 및 급여명세서 이메일 수신 주소 조회. */
  @Override
  public Optional<EmployeeView> findById(Long userId) {
    return repository.findById(userId).map(this::toView);
  }

  /** Consumer: payroll. Purpose: 활성 직원의 급여 생성 및 급여 설정 변경 검증. */
  @Override
  public Optional<EmployeeView> findActiveById(Long userId) {
    return repository.findById(userId).filter(user -> user.getStatus() == UserStatus.ACTIVE)
        .map(this::toView);
  }

  /** Consumer: payroll. Purpose: 월 급여 목록의 활성 직원 페이지 조회. */
  @Override
  public List<EmployeeView> findAllActive(String keyword) {
    List<UserEntity> employees = keyword == null || keyword.isBlank()
        ? repository.findAllByStatus(UserStatus.ACTIVE)
        : repository.findAllByStatusAndNameContainingIgnoreCase(UserStatus.ACTIVE, keyword.trim());
    return employees.stream().map(this::toView).toList();
  }

  private EmployeeView toView(UserEntity user) {
    return new EmployeeView(user.getId(), user.getName(), user.getEmail(),
        user.getJoinedAt() == null ? null : user.getJoinedAt().toLocalDate());
  }
}
