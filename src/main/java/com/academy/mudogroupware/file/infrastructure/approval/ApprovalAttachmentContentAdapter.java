package com.academy.mudogroupware.file.infrastructure.approval;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Set;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import com.academy.mudogroupware.approval.application.port.AttachmentContent;
import com.academy.mudogroupware.approval.application.port.AttachmentContentPort;
import com.academy.mudogroupware.approval.application.port.AttachmentContentUnavailableException;
import com.academy.mudogroupware.file.domain.repository.FileStoragePort;
import com.academy.mudogroupware.file.infrastructure.persistence.FileMetadataEntity;
import com.academy.mudogroupware.file.infrastructure.persistence.FileMetadataJpaRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ApprovalAttachmentContentAdapter implements AttachmentContentPort {

    // Gemini generateContent inline 요청은 대략 20MB 제한이 있다. base64 인코딩(약 4/3배 증가)과
    // 프롬프트 여유분을 감안해 원본 파일 기준으로 더 보수적인 상한을 둔다.
    private static final long MAX_BINARY_BYTES = 15L * 1024 * 1024;

    private static final String DOCX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final Set<String> INLINE_BINARY_CONTENT_TYPES = Set.of(
            "application/pdf", "image/jpeg", "image/png", "image/webp", "image/heic", "image/heif");

    private final FileMetadataJpaRepository fileMetadataJpaRepository;
    private final FileStoragePort fileStoragePort;

    /**
     * Consumer: approval
     * Purpose: Load stored attachment content for Gemini summary without approval knowing file storage details.
     * 텍스트 계열은 그대로 디코딩하고, docx는 텍스트를 추출하고, PDF/이미지는 Gemini가 직접 읽을 수 있도록
     * 바이너리 그대로 넘긴다.
     */
    @Override
    public AttachmentContent loadContent(Long fileId) {
        FileMetadataEntity metadata = fileMetadataJpaRepository.findById(fileId)
                .orElseThrow(() -> new AttachmentContentUnavailableException(fileId));
        String contentType = metadata.getContentType();

        if (isTextContent(contentType)) {
            return AttachmentContent.text(downloadAsText(fileId, metadata.getObjectKey()));
        }
        if (isDocxContent(contentType)) {
            return AttachmentContent.text(extractDocxText(fileId, metadata.getObjectKey()));
        }
        if (isInlineBinaryContent(contentType)) {
            byte[] data = download(fileId, metadata.getObjectKey());
            if (data.length > MAX_BINARY_BYTES) {
                throw new AttachmentContentUnavailableException(fileId);
            }
            return AttachmentContent.binary(data, contentType);
        }
        throw new AttachmentContentUnavailableException(fileId);
    }

    private String downloadAsText(Long fileId, String objectKey) {
        return new String(download(fileId, objectKey), UTF_8);
    }

    private String extractDocxText(Long fileId, String objectKey) {
        byte[] data = download(fileId, objectKey);
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(data));
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            String text = extractor.getText();
            if (text == null || text.isBlank()) {
                throw new AttachmentContentUnavailableException(fileId);
            }
            return text;
        } catch (IOException | RuntimeException e) {
            if (e instanceof AttachmentContentUnavailableException unavailable) {
                throw unavailable;
            }
            throw new AttachmentContentUnavailableException(fileId);
        }
    }

    private byte[] download(Long fileId, String objectKey) {
        try {
            return fileStoragePort.download(objectKey);
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

    private boolean isDocxContent(String contentType) {
        return DOCX_CONTENT_TYPE.equalsIgnoreCase(contentType);
    }

    private boolean isInlineBinaryContent(String contentType) {
        return contentType != null && INLINE_BINARY_CONTENT_TYPES.contains(contentType.toLowerCase());
    }
}
