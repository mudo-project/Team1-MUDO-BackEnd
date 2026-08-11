package com.academy.mudogroupware.file.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
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
        FileMetadataEntity metadata = FileMetadataEntity.restore(5L, "uploads/abc-file.pdf", "application/pdf");
        when(fileMetadataJpaRepository.findById(5L)).thenReturn(Optional.of(metadata));
        when(fileStoragePort.generatePresignedDownloadUrl("uploads/abc-file.pdf"))
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

    @Test
    void batchResolvesDownloadUrlsForExistingFilesAndSkipsMissingOnes() {
        FileMetadataEntity fileA = FileMetadataEntity.restore(1L, "uploads/a.png", "image/png");
        FileMetadataEntity fileB = FileMetadataEntity.restore(2L, "uploads/b.png", "image/png");
        when(fileMetadataJpaRepository.findAllById(List.of(1L, 2L, 999L)))
                .thenReturn(List.of(fileA, fileB));
        when(fileStoragePort.generatePresignedDownloadUrl("uploads/a.png")).thenReturn("url-a");
        when(fileStoragePort.generatePresignedDownloadUrl("uploads/b.png")).thenReturn("url-b");

        Map<Long, String> downloadUrls = service.getDownloadUrls(List.of(1L, 2L, 999L));

        assertThat(downloadUrls).containsExactlyInAnyOrderEntriesOf(Map.of(1L, "url-a", 2L, "url-b"));
    }

    @Test
    void batchReturnsEmptyMapForEmptyInput() {
        assertThat(service.getDownloadUrls(List.of())).isEmpty();
    }
}
