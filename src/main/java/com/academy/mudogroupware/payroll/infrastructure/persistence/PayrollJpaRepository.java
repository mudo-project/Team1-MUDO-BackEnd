package com.academy.mudogroupware.payroll.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PayrollJpaRepository extends JpaRepository<PayrollJpaEntity, Long> {
  boolean existsByUserIdAndYearMonthAndRevisionNo(Long userId, LocalDate yearMonth, int revisionNo);

  @EntityGraph(attributePaths = "items")
  @Query("select p from PayrollJpaEntity p where p.id = :id")
  Optional<PayrollJpaEntity> findAggregateById(@Param("id") Long id);

  @EntityGraph(attributePaths = "items")
  Optional<PayrollJpaEntity> findFirstByUserIdAndYearMonthOrderByRevisionNoDesc(
      Long userId, LocalDate yearMonth);

  @EntityGraph(attributePaths = "items")
  List<PayrollJpaEntity> findAllByUserIdAndYearMonthOrderByRevisionNoDesc(
      Long userId, LocalDate yearMonth);

  @Query("select p from PayrollJpaEntity p where p.yearMonth = :yearMonth and p.revisionNo = "
      + "(select max(p2.revisionNo) from PayrollJpaEntity p2 where p2.userId = p.userId "
      + "and p2.yearMonth = p.yearMonth)")
  List<PayrollJpaEntity> findLatestByMonth(@Param("yearMonth") LocalDate yearMonth);
}
