package com.academy.mudogroupware.dataimport.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DataImportJobJpaRepository extends JpaRepository<DataImportJobEntity, Long> {
}
