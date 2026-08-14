package com.academy.mudogroupware.file.application.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.file.application.port.FileMetadataCleanupPort;
import com.academy.mudogroupware.file.application.port.FileMetadataCleanupTarget;
import com.academy.mudogroupware.file.application.port.FileReferenceChecker;
import com.academy.mudogroupware.file.domain.repository.FileStoragePort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeleteUnreferencedFilesService {

    private final FileMetadataCleanupPort fileMetadataCleanupPort;
    private final FileStoragePort fileStoragePort;
    private final FileReferenceChecker fileReferenceChecker;

    @Transactional
    public int deleteUnreferencedFiles(List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return 0;
        }

        List<FileMetadataCleanupTarget> targets = fileMetadataCleanupPort.findAllByIds(fileIds);
        Set<Long> referencedFileIds = fileReferenceChecker.findReferencedFileIds(fileIds);
        int deletedCount = 0;
        for (FileMetadataCleanupTarget metadata : targets) {
            if (referencedFileIds.contains(metadata.fileId())) {
                continue;
            }
            try {
                fileStoragePort.delete(metadata.objectKey());
                fileMetadataCleanupPort.deleteById(metadata.fileId());
                deletedCount++;
            } catch (RuntimeException exception) {
                log.warn("event=file_cleanup_failed fileId={}, reason={}",
                        metadata.fileId(), exception.getMessage(), exception);
            }
        }
        return deletedCount;
    }
}
