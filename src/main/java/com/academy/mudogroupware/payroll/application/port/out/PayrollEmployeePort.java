package com.academy.mudogroupware.payroll.application.port.out;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface PayrollEmployeePort {
  Optional<EmployeeView> findById(Long userId);
  Map<Long, EmployeeView> findByIds(Set<Long> userIds);
  Optional<EmployeeView> findActiveById(Long userId);
  List<EmployeeView> findAllActive(String keyword);

  record EmployeeView(Long id, String name, String email, LocalDate joinedAt) {}
}
