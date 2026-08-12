package com.academy.mudogroupware.payroll.application.port.out;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

public interface PayrollEmployeePort {
  Optional<EmployeeView> findById(Long userId);
  Optional<EmployeeView> findActiveById(Long userId);
  List<EmployeeView> findAllActive(String keyword);

  record EmployeeView(Long id, String name, LocalDate joinedAt) {}
}
