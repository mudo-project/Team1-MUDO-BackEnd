package com.academy.mudogroupware.file.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.file.application.command.GeneratePresignedUploadUrlCommand;
import com.academy.mudogroupware.file.application.result.PresignedUploadUrlResult;
import com.academy.mudogroupware.file.domain.repository.FileStoragePort;

class GeneratePresignedUploadUrlServiceTest {

    private final FileStoragePort fileStoragePort = mock(FileStoragePort.class);
    private final GeneratePresignedUploadUrlService service =
            new GeneratePresignedUploadUrlService(fileStoragePort);

    @Test
    void generatesObjectKeyScopedByAcademyAndSanitizesFileName() {
        when(fileStoragePort.generatePresignedUploadUrl(anyString(), eq("application/pdf")))
                .thenReturn("https://s3.example.com/presigned-put");

        PresignedUploadUrlResult result = service.generate(
                new GeneratePresignedUploadUrlCommand(10L, "../secret/휴가원.pdf", "application/pdf"));

        assertThat(result.objectKey()).startsWith("uploads/10/");
        assertThat(result.objectKey()).endsWith("휴가원.pdf");
        // fileName의 경로 구분자는 전부 치환되어, uploads/{academyId}/ 뒤로 추가 "/"가 생기지 않는다
        // (uploads/10/ 두 개 + 접미사에 남은 슬래시 없음 = 총 2개)
        long slashCount = result.objectKey().chars().filter(c -> c == '/').count();
        assertThat(slashCount).isEqualTo(2);
        assertThat(result.uploadUrl()).isEqualTo("https://s3.example.com/presigned-put");
    }
}
