package com.academy.mudogroupware.file.infrastructure.approval;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.approval.application.port.AttachmentContentPort;
import com.academy.mudogroupware.approval.application.port.AttachmentContentUnavailableException;
import com.academy.mudogroupware.file.domain.repository.FileStoragePort;
import com.academy.mudogroupware.file.infrastructure.persistence.FileMetadataEntity;
import com.academy.mudogroupware.file.infrastructure.persistence.FileMetadataJpaRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ApprovalAttachmentContentAdapter implements AttachmentContentPort {

    private final FileMetadataJpaRepository fileMetadataJpaRepository;
    private final FileStoragePort fileStoragePort;

    /**
     * Consumer: approval
     * Purpose: Load stored attachment text for Gemini summary without approval knowing file storage details.
     */
    @Override
    public String loadContent(Long fileId) {
        FileMetadataEntity metadata = fileMetadataJpaRepository.findById(fileId)
                .orElseThrow(() -> new AttachmentContentUnavailableException(fileId));
        if (!isTextContent(metadata.getContentType())) {
            throw new AttachmentContentUnavailableException(fileId);
        }
        try {
            return new String(fileStoragePort.download(metadata.getObjectKey()), UTF_8);
        } catch (RuntimeException e) {
            throw new AttachmentContentUnavailableException(fileId);
        }
    }

    private boolean isTextContent(String contentType) {
        if (contentType == null) {
            return false;
        }
        String normalized = contentType.toLowerCase();
        return normalized.startsWith("text/")
                || normalized.equals("application/json")
                || normalized.equals("application/xml")
                || normalized.equals("application/csv")
                || normalized.equals("application/x-www-form-urlencoded");
    }
}
