package com.academy.mudogroupware.file.infrastructure.persistence;

import java.util.List;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.file.application.port.FileMetadataCleanupPort;
import com.academy.mudogroupware.file.application.port.FileMetadataCleanupTarget;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JpaFileMetadataCleanupAdapter implements FileMetadataCleanupPort {

    private final FileMetadataJpaRepository fileMetadataJpaRepository;

    @Override
    public List<FileMetadataCleanupTarget> findAllByIds(List<Long> fileIds) {
        return fileMetadataJpaRepository.findAllById(fileIds)
                .stream()
                .map(metadata -> new FileMetadataCleanupTarget(metadata.getId(), metadata.getObjectKey()))
                .toList();
    }

    @Override
    public void deleteById(Long fileId) {
        fileMetadataJpaRepository.deleteById(fileId);
    }
}
