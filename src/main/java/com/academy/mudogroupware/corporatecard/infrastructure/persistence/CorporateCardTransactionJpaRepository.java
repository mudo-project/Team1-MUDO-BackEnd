package com.academy.mudogroupware.corporatecard.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CorporateCardTransactionJpaRepository extends JpaRepository<CorporateCardTransactionJpaEntity, Long> {
    List<CorporateCardTransactionJpaEntity> findAllByCard_AcademyIdOrderByApprovedAtDesc(Long academyId);
    Optional<CorporateCardTransactionJpaEntity> findByIdAndCard_AcademyId(Long id, Long academyId);
}
