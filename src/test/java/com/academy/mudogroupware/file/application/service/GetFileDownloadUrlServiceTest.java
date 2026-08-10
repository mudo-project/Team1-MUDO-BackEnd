package com.academy.mudogroupware.file.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.file.domain.exception.FileException;
import com.academy.mudogroupware.file.domain.repository.FileStoragePort;
import com.academy.mudogroupware.file.infrastructure.persistence.FileMetadataEntity;
import com.academy.mudogroupware.file.infrastructure.persistence.FileMetadataJpaRepository;

class GetFileDownloadUrlServiceTest {

    private final FileMetadataJpaRepository fileMetadataJpaRepository = mock(FileMetadataJpaRepository.class);
    private final FileStoragePort fileStoragePort = mock(FileStoragePort.class);
    private final GetFileDownloadUrlService service =
            new GetFileDownloadUrlService(fileMetadataJpaRepository, fileStoragePort);

    @Test
    void returnsPresignedDownloadUrlForRegisteredFile() {
        FileMetadataEntity metadata = FileMetadataEntity.restore(5L, "uploads/10/abc-file.pdf", "application/pdf");
        when(fileMetadataJpaRepository.findById(5L)).thenReturn(Optional.of(metadata));
        when(fileStoragePort.generatePresignedDownloadUrl("uploads/10/abc-file.pdf"))
                .thenReturn("https://s3.example.com/presigned-get");

        String downloadUrl = service.getDownloadUrl(5L);

        assertThat(downloadUrl).isEqualTo("https://s3.example.com/presigned-get");
    }

    @Test
    void throwsWhenFileDoesNotExist() {
        when(fileMetadataJpaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDownloadUrl(999L))
                .isInstanceOf(FileException.class);
    }
}
