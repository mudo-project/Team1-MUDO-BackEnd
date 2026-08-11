package com.academy.mudogroupware.file.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FileMetadataJpaRepository extends JpaRepository<FileMetadataEntity, Long> {
}
