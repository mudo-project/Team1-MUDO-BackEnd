package com.academy.mudogroupware.file.infrastructure.approval;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FileMetadataJpaRepository extends JpaRepository<FileMetadataEntity, Long> {
}
