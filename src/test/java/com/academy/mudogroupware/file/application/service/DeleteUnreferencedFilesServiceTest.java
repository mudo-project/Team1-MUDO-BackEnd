package com.academy.mudogroupware.file.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.academy.mudogroupware.file.application.port.FileReferenceChecker;
import com.academy.mudogroupware.file.domain.repository.FileStoragePort;
import com.academy.mudogroupware.file.infrastructure.persistence.FileMetadataEntity;
import com.academy.mudogroupware.file.infrastructure.persistence.FileMetadataJpaRepository;

class DeleteUnreferencedFilesServiceTest {

    private final FileMetadataJpaRepository fileMetadataJpaRepository =
            Mockito.mock(FileMetadataJpaRepository.class);
    private final FileStoragePort fileStoragePort = Mockito.mock(FileStoragePort.class);
    private final FileReferenceChecker fileReferenceChecker = Mockito.mock(FileReferenceChecker.class);
    private final DeleteUnreferencedFilesService service = new DeleteUnreferencedFilesService(
            fileMetadataJpaRepository, fileStoragePort, fileReferenceChecker);

    @Test
    void deletesOnlyFilesThatAreNoLongerReferenced() {
        FileMetadataEntity unreferenced = FileMetadataEntity.restore(
                1L, "tenants/academy-a/files/a.pdf", "application/pdf");
        FileMetadataEntity referenced = FileMetadataEntity.restore(
                2L, "tenants/academy-a/files/b.pdf", "application/pdf");
        when(fileMetadataJpaRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(List.of(unreferenced, referenced));
        when(fileReferenceChecker.isReferenced(1L)).thenReturn(false);
        when(fileReferenceChecker.isReferenced(2L)).thenReturn(true);

        int deletedCount = service.deleteUnreferencedFiles(List.of(1L, 2L));

        assertThat(deletedCount).isEqualTo(1);
        verify(fileStoragePort).delete("tenants/academy-a/files/a.pdf");
        verify(fileMetadataJpaRepository).delete(unreferenced);
        verify(fileStoragePort, never()).delete("tenants/academy-a/files/b.pdf");
        verify(fileMetadataJpaRepository, never()).delete(referenced);
    }
}
