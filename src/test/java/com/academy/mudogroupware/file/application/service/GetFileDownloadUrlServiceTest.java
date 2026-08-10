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

    private static final Long ACADEMY_ID = 10L;

    private final FileMetadataJpaRepository fileMetadataJpaRepository = mock(FileMetadataJpaRepository.class);
    private final FileStoragePort fileStoragePort = mock(FileStoragePort.class);
    private final GetFileDownloadUrlService service =
            new GetFileDownloadUrlService(fileMetadataJpaRepository, fileStoragePort);

    @Test
    void returnsPresignedDownloadUrlForRegisteredFile() {
        FileMetadataEntity metadata = FileMetadataEntity.restore(5L, ACADEMY_ID, "uploads/10/abc-file.pdf",
                "application/pdf");
        when(fileMetadataJpaRepository.findByIdAndAcademyId(5L, ACADEMY_ID)).thenReturn(Optional.of(metadata));
        when(fileStoragePort.generatePresignedDownloadUrl("uploads/10/abc-file.pdf"))
                .thenReturn("https://s3.example.com/presigned-get");

        String downloadUrl = service.getDownloadUrl(5L, ACADEMY_ID);

        assertThat(downloadUrl).isEqualTo("https://s3.example.com/presigned-get");
    }

    @Test
    void throwsWhenFileDoesNotExist() {
        when(fileMetadataJpaRepository.findByIdAndAcademyId(999L, ACADEMY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDownloadUrl(999L, ACADEMY_ID))
                .isInstanceOf(FileException.class);
    }

    @Test
    void throwsWhenFileBelongsToAnotherAcademy() {
        // 다른 학원(20L) 소속 파일이라 findByIdAndAcademyId(5L, 10L) 자체가 빈 결과를 반환해야 한다.
        when(fileMetadataJpaRepository.findByIdAndAcademyId(5L, ACADEMY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDownloadUrl(5L, ACADEMY_ID))
                .isInstanceOf(FileException.class);
    }

    @Test
    void batchResolvesDownloadUrlsForExistingFilesAndSkipsMissingOnes() {
        FileMetadataEntity fileA = FileMetadataEntity.restore(1L, ACADEMY_ID, "uploads/10/a.png", "image/png");
        FileMetadataEntity fileB = FileMetadataEntity.restore(2L, ACADEMY_ID, "uploads/10/b.png", "image/png");
        when(fileMetadataJpaRepository.findAllByIdInAndAcademyId(List.of(1L, 2L, 999L), ACADEMY_ID))
                .thenReturn(List.of(fileA, fileB));
        when(fileStoragePort.generatePresignedDownloadUrl("uploads/10/a.png")).thenReturn("url-a");
        when(fileStoragePort.generatePresignedDownloadUrl("uploads/10/b.png")).thenReturn("url-b");

        Map<Long, String> downloadUrls = service.getDownloadUrls(List.of(1L, 2L, 999L), ACADEMY_ID);

        assertThat(downloadUrls).containsExactlyInAnyOrderEntriesOf(Map.of(1L, "url-a", 2L, "url-b"));
    }

    @Test
    void batchReturnsEmptyMapForEmptyInput() {
        assertThat(service.getDownloadUrls(List.of(), ACADEMY_ID)).isEmpty();
    }
}
