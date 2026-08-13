package com.academy.mudogroupware.corporatecard.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.academy.mudogroupware.corporatecard.domain.model.ExpenseCategory;

import jakarta.persistence.LockModeType;

public interface CardExpenseJpaRepository extends JpaRepository<CardExpenseJpaEntity, Long> {
    Optional<CardExpenseJpaEntity> findByTransaction_Id(Long transactionId);
    java.util.List<CardExpenseJpaEntity> findAllByTransaction_IdIn(java.util.List<Long> transactionIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from CardExpenseJpaEntity e join fetch e.transaction t join fetch t.card where t.id = :transactionId")
    Optional<CardExpenseJpaEntity> findForUpdate(@Param("transactionId") Long transactionId);

    @Query("""
            select ce.expenseCategory as category, sum(t.amount) as amount
            from CardExpenseJpaEntity ce
            join ce.transaction t
            where t.approvedAt >= :from and t.approvedAt < :to
            group by ce.expenseCategory
            """)
    List<CategoryAmountProjection> sumAmountByCategoryAndApprovedAtBetween(
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    interface CategoryAmountProjection {
        ExpenseCategory getCategory();

        Long getAmount();
    }
}
