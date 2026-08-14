package com.academy.mudogroupware.file.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.academy.mudogroupware.file.application.port.FileMetadataCleanupPort;
import com.academy.mudogroupware.file.application.port.FileMetadataCleanupTarget;
import com.academy.mudogroupware.file.application.port.FileReferenceChecker;
import com.academy.mudogroupware.file.domain.repository.FileStoragePort;

class DeleteUnreferencedFilesServiceTest {

    private final FileMetadataCleanupPort fileMetadataCleanupPort = Mockito.mock(FileMetadataCleanupPort.class);
    private final FileStoragePort fileStoragePort = Mockito.mock(FileStoragePort.class);
    private final FileReferenceChecker fileReferenceChecker = Mockito.mock(FileReferenceChecker.class);
    private final DeleteUnreferencedFilesService service = new DeleteUnreferencedFilesService(
            fileMetadataCleanupPort, fileStoragePort, fileReferenceChecker);

    @Test
    void deletesOnlyFilesThatAreNoLongerReferenced() {
        FileMetadataCleanupTarget unreferenced = new FileMetadataCleanupTarget(
                1L, "tenants/academy-a/files/a.pdf");
        FileMetadataCleanupTarget referenced = new FileMetadataCleanupTarget(
                2L, "tenants/academy-a/files/b.pdf");
        when(fileMetadataCleanupPort.findAllByIds(List.of(1L, 2L)))
                .thenReturn(List.of(unreferenced, referenced));
        when(fileReferenceChecker.findReferencedFileIds(List.of(1L, 2L))).thenReturn(Set.of(2L));

        int deletedCount = service.deleteUnreferencedFiles(List.of(1L, 2L));

        assertThat(deletedCount).isEqualTo(1);
        verify(fileStoragePort).delete("tenants/academy-a/files/a.pdf");
        verify(fileMetadataCleanupPort).deleteById(1L);
        verify(fileStoragePort, never()).delete("tenants/academy-a/files/b.pdf");
        verify(fileMetadataCleanupPort, never()).deleteById(2L);
        verify(fileReferenceChecker).findReferencedFileIds(List.of(1L, 2L));
    }
}
