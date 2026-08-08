package com.academy.mudogroupware.corporatecard.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface CorporateCardTransactionJpaRepository extends JpaRepository<CorporateCardTransactionJpaEntity, Long> {
    Slice<CorporateCardTransactionJpaEntity> findAllByCard_AcademyId(Long academyId, Pageable pageable);
    Optional<CorporateCardTransactionJpaEntity> findByIdAndCard_AcademyId(Long id, Long academyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from CorporateCardTransactionJpaEntity t join fetch t.card c where t.id = :id and c.academyId = :academyId")
    Optional<CorporateCardTransactionJpaEntity> findForUpdate(@Param("id") Long id, @Param("academyId") Long academyId);
}
