package com.academy.mudogroupware.corporatecard.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface CorporateCardTransactionJpaRepository extends JpaRepository<CorporateCardTransactionJpaEntity, Long> {
    Page<CorporateCardTransactionJpaEntity> findAllBy(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from CorporateCardTransactionJpaEntity t join fetch t.card where t.id = :id")
    Optional<CorporateCardTransactionJpaEntity> findForUpdate(@Param("id") Long id);
}
