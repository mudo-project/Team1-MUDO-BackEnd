package com.academy.mudogroupware.file.infrastructure.approval;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.util.Optional;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.approval.application.port.AttachmentContent;
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
        FileMetadataEntity metadata = FileMetadataEntity.restore(10L, "approval/10.txt", "text/plain");
        when(fileMetadataJpaRepository.findById(10L)).thenReturn(Optional.of(metadata));
        when(fileStoragePort.download("approval/10.txt")).thenReturn("real attachment text".getBytes(UTF_8));

        AttachmentContent result = adapter.loadContent(10L);

        assertThat(result.kind()).isEqualTo(AttachmentContent.Kind.TEXT);
        assertThat(result.text()).isEqualTo("real attachment text");
    }

    @Test
    void throwsWhenMetadataDoesNotExist() {
        when(fileMetadataJpaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.loadContent(999L))
                .isInstanceOf(AttachmentContentUnavailableException.class);
    }

    @Test
    void returnsBinaryContentForPdf() {
        byte[] pdfBytes = {0x25, 0x50, 0x44, 0x46};
        FileMetadataEntity metadata = FileMetadataEntity.restore(10L, "approval/10.pdf", "application/pdf");
        when(fileMetadataJpaRepository.findById(10L)).thenReturn(Optional.of(metadata));
        when(fileStoragePort.download("approval/10.pdf")).thenReturn(pdfBytes);

        AttachmentContent result = adapter.loadContent(10L);

        assertThat(result.kind()).isEqualTo(AttachmentContent.Kind.BINARY);
        assertThat(result.mimeType()).isEqualTo("application/pdf");
        assertThat(result.binaryData()).isEqualTo(pdfBytes);
    }

    @Test
    void throwsWhenPdfExceedsSizeLimit() {
        byte[] oversized = new byte[16 * 1024 * 1024];
        FileMetadataEntity metadata = FileMetadataEntity.restore(10L, "approval/10.pdf", "application/pdf");
        when(fileMetadataJpaRepository.findById(10L)).thenReturn(Optional.of(metadata));
        when(fileStoragePort.download("approval/10.pdf")).thenReturn(oversized);

        assertThatThrownBy(() -> adapter.loadContent(10L))
                .isInstanceOf(AttachmentContentUnavailableException.class);
    }

    @Test
    void extractsTextFromDocx() throws Exception {
        byte[] docxBytes = buildDocxWithParagraph("결재 첨부 docx 본문입니다");
        FileMetadataEntity metadata = FileMetadataEntity.restore(10L, "approval/10.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        when(fileMetadataJpaRepository.findById(10L)).thenReturn(Optional.of(metadata));
        when(fileStoragePort.download("approval/10.docx")).thenReturn(docxBytes);

        AttachmentContent result = adapter.loadContent(10L);

        assertThat(result.kind()).isEqualTo(AttachmentContent.Kind.TEXT);
        assertThat(result.text()).contains("결재 첨부 docx 본문입니다");
    }

    @Test
    void throwsWhenContentTypeIsUnsupported() {
        FileMetadataEntity metadata = FileMetadataEntity.restore(10L, "approval/10.hwp",
                "application/x-hwp");
        when(fileMetadataJpaRepository.findById(10L)).thenReturn(Optional.of(metadata));

        assertThatThrownBy(() -> adapter.loadContent(10L))
                .isInstanceOf(AttachmentContentUnavailableException.class);
    }

    private byte[] buildDocxWithParagraph(String text) throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun run = paragraph.createRun();
            run.setText(text);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);
            return out.toByteArray();
        }
    }
}
