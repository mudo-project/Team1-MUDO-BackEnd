package com.academy.mudogroupware.file.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.file.application.command.RegisterFileCommand;
import com.academy.mudogroupware.file.infrastructure.persistence.FileMetadataEntity;
import com.academy.mudogroupware.file.infrastructure.persistence.FileMetadataJpaRepository;

class RegisterFileServiceTest {

    private final FileMetadataJpaRepository fileMetadataJpaRepository = mock(FileMetadataJpaRepository.class);
    private final RegisterFileService service = new RegisterFileService(fileMetadataJpaRepository);

    @Test
    void registersFileMetadataAndReturnsGeneratedId() {
        when(fileMetadataJpaRepository.save(any(FileMetadataEntity.class)))
                .thenReturn(FileMetadataEntity.restore(5L, "uploads/10/abc-file.pdf", "application/pdf"));

        Long fileId = service.register(new RegisterFileCommand("uploads/10/abc-file.pdf", "application/pdf"));

        assertThat(fileId).isEqualTo(5L);
    }
}
