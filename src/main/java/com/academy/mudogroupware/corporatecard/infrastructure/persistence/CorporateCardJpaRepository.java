package com.academy.mudogroupware.corporatecard.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CorporateCardJpaRepository extends JpaRepository<CorporateCardJpaEntity, Long> {
}
