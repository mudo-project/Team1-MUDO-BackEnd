package com.academy.mudogroupware.file.application.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.file.application.usecase.GetFileDownloadUrlUseCase;
import com.academy.mudogroupware.file.domain.exception.FileErrorCode;
import com.academy.mudogroupware.file.domain.exception.FileException;
import com.academy.mudogroupware.file.domain.repository.FileStoragePort;
import com.academy.mudogroupware.file.infrastructure.persistence.FileMetadataEntity;
import com.academy.mudogroupware.file.infrastructure.persistence.FileMetadataJpaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetFileDownloadUrlService implements GetFileDownloadUrlUseCase {

    private final FileMetadataJpaRepository fileMetadataJpaRepository;
    private final FileStoragePort fileStoragePort;

    @Override
    public String getDownloadUrl(Long fileId, Long academyId) {
        FileMetadataEntity metadata = fileMetadataJpaRepository.findByIdAndAcademyId(fileId, academyId)
                .orElseThrow(() -> new FileException(FileErrorCode.FILE_NOT_FOUND));
        return fileStoragePort.generatePresignedDownloadUrl(metadata.getObjectKey());
    }

    @Override
    public Map<Long, String> getDownloadUrls(List<Long> fileIds, Long academyId) {
        if (fileIds == null || fileIds.isEmpty()) {
            return Map.of();
        }
        return fileMetadataJpaRepository.findAllByIdInAndAcademyId(fileIds, academyId).stream()
                .collect(Collectors.toMap(FileMetadataEntity::getId,
                        metadata -> fileStoragePort.generatePresignedDownloadUrl(metadata.getObjectKey())));
    }
}
