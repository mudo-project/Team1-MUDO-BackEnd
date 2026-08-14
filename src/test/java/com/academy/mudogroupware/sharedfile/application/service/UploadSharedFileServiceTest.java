package com.academy.mudogroupware.sharedfile.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.google.application.usecase.GetGoogleAccessTokenUseCase;
import com.academy.mudogroupware.sharedfile.application.port.DriveItem;
import com.academy.mudogroupware.sharedfile.application.port.SharedFileDrivePort;
import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemView;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileInvalidNameException;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileOutOfRootException;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileRootUnavailableException;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileUploadTooLargeException;
import com.academy.mudogroupware.sharedfile.domain.model.SharedFileRoot;
import com.academy.mudogroupware.sharedfile.domain.repository.SharedFileRootRepository;
import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;

class UploadSharedFileServiceTest {

    private static final int ONE_HUNDRED_MB = 100 * 1024 * 1024;
    private static final byte[] CONTENT = "content".getBytes();

    private final SharedFileRootRepository rootRepository = mock(SharedFileRootRepository.class);
    private final SharedFileRootGuard rootGuard = mock(SharedFileRootGuard.class);
    private final SharedFileDrivePort drivePort = mock(SharedFileDrivePort.class);
    private final GetGoogleAccessTokenUseCase getGoogleAccessTokenUseCase = mock(GetGoogleAccessTokenUseCase.class);

    private final UploadSharedFileService service =
            new UploadSharedFileService(rootRepository, rootGuard, drivePort, getGoogleAccessTokenUseCase);

    // size 파라미터로 우회할 수 없어야 한다 — 실제 content 길이 기준으로 검사한다.
    @Test
    void throwsWhenActualContentExceeds100Mb() {
        byte[] oversized = new byte[ONE_HUNDRED_MB + 1];

        assertThatThrownBy(() -> service.upload("parent-id", "big.zip", "application/zip", oversized))
                .isInstanceOf(SharedFileUploadTooLargeException.class);

        verify(drivePort, never()).upload(any(), any(), any(), any(), any());
    }

    @Test
    void allowsExactly100Mb() {
        byte[] exact = new byte[ONE_HUNDRED_MB];
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        when(drivePort.upload("access-token", "root-id", "exact.zip", "application/zip", exact))
                .thenReturn(driveItem("uploaded-id"));

        SharedFileItemView view = service.upload("root-id", "exact.zip", "application/zip", exact);

        assertThat(view.id()).isEqualTo("uploaded-id");
    }

    @Test
    void throwsWhenFilenameIsBlank() {
        assertThatThrownBy(() -> service.upload("parent-id", " ", "application/zip", CONTENT))
                .isInstanceOf(SharedFileInvalidNameException.class);
    }

    @Test
    void uploadsUnderRootWhenParentIdIsOmitted() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        when(drivePort.upload("access-token", "root-id", "a.txt", "text/plain", CONTENT))
                .thenReturn(driveItem("uploaded-id"));

        SharedFileItemView view = service.upload(null, "a.txt", "text/plain", CONTENT);

        assertThat(view.id()).isEqualTo("uploaded-id");
        verify(rootGuard, never()).requireDescendant(any(), any(), any());
    }

    @Test
    void throwsWhenParentIdIsBlank() {
        assertThatThrownBy(() -> service.upload(" ", "a.txt", "text/plain", CONTENT))
                .isInstanceOf(BadRequestException.class);

        verify(drivePort, never()).upload(any(), any(), any(), any(), any());
    }

    @Test
    void throwsWhenRootIsNotReady() {
        when(rootRepository.find()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upload("parent-id", "a.txt", "text/plain", CONTENT))
                .isInstanceOf(SharedFileRootUnavailableException.class);
    }

    @Test
    void throwsWhenParentIsOutsideRoot() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        doThrow(new SharedFileOutOfRootException("outside-id"))
                .when(rootGuard).requireDescendant("access-token", "root-id", "outside-id");

        assertThatThrownBy(() -> service.upload("outside-id", "a.txt", "text/plain", CONTENT))
                .isInstanceOf(SharedFileOutOfRootException.class);
    }

    private DriveItem driveItem(String id) {
        return new DriveItem(id, "name", "application/octet-stream", List.of("root-id"), null, true, null, false);
    }
}
