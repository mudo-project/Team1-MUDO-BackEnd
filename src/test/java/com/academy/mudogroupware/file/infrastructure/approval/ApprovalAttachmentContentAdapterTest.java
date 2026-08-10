package com.academy.mudogroupware.file.infrastructure.approval;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.approval.application.port.AttachmentContentUnavailableException;
import com.academy.mudogroupware.file.domain.repository.FileStoragePort;
import com.academy.mudogroupware.file.infrastructure.persistence.FileMetadataEntity;
import com.academy.mudogroupware.file.infrastructure.persistence.FileMetadataJpaRepository;

class ApprovalAttachmentContentAdapterTest {

    private final FileMetadataJpaRepository fileMetadataJpaRepository = mock(FileMetadataJpaRepository.class);
    private final FileStoragePort fileStoragePort = mock(FileStoragePort.class);
    private final ApprovalAttachmentContentAdapter adapter =
            new ApprovalAttachmentContentAdapter(fileMetadataJpaRepository, fileStoragePort);

    @Test
    void loadsUtf8TextContentByFileId() {
        FileMetadataEntity metadata = FileMetadataEntity.restore(10L, 1L, "approval/10.txt", "text/plain");
        when(fileMetadataJpaRepository.findById(10L)).thenReturn(Optional.of(metadata));
        when(fileStoragePort.download("approval/10.txt")).thenReturn("real attachment text".getBytes(UTF_8));

        String result = adapter.loadContent(10L);

        assertThat(result).isEqualTo("real attachment text");
    }

    @Test
    void throwsWhenMetadataDoesNotExist() {
        when(fileMetadataJpaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.loadContent(999L))
                .isInstanceOf(AttachmentContentUnavailableException.class);
    }

    @Test
    void throwsWhenContentTypeIsNotText() {
        FileMetadataEntity metadata = FileMetadataEntity.restore(10L, 1L, "approval/10.pdf", "application/pdf");
        when(fileMetadataJpaRepository.findById(10L)).thenReturn(Optional.of(metadata));

        assertThatThrownBy(() -> adapter.loadContent(10L))
                .isInstanceOf(AttachmentContentUnavailableException.class);
    }
}
