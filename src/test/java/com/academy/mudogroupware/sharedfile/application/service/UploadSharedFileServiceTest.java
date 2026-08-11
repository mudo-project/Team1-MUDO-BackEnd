package com.academy.mudogroupware.sharedfile.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

class UploadSharedFileServiceTest {

    private static final long ONE_HUNDRED_MB = 100L * 1024 * 1024;
    private static final byte[] CONTENT = "content".getBytes();

    private final SharedFileRootRepository rootRepository = mock(SharedFileRootRepository.class);
    private final SharedFileRootGuard rootGuard = mock(SharedFileRootGuard.class);
    private final SharedFileDrivePort drivePort = mock(SharedFileDrivePort.class);
    private final GetGoogleAccessTokenUseCase getGoogleAccessTokenUseCase = mock(GetGoogleAccessTokenUseCase.class);

    private final UploadSharedFileService service =
            new UploadSharedFileService(rootRepository, rootGuard, drivePort, getGoogleAccessTokenUseCase);

    @Test
    void throwsWhenFileExceeds100Mb() {
        assertThatThrownBy(() -> service.upload(
                "parent-id", "big.zip", "application/zip", ONE_HUNDRED_MB + 1, CONTENT))
                .isInstanceOf(SharedFileUploadTooLargeException.class);

        verify100MbNeverCalledDrive();
    }

    @Test
    void allowsExactly100Mb() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        when(drivePort.upload("access-token", "root-id", "exact.zip", "application/zip", CONTENT))
                .thenReturn(driveItem("uploaded-id"));

        SharedFileItemView view = service.upload("root-id", "exact.zip", "application/zip", ONE_HUNDRED_MB, CONTENT);

        assertThat(view.id()).isEqualTo("uploaded-id");
    }

    @Test
    void throwsWhenFilenameIsBlank() {
        assertThatThrownBy(() -> service.upload("parent-id", " ", "application/zip", 1L, CONTENT))
                .isInstanceOf(SharedFileInvalidNameException.class);
    }

    @Test
    void throwsWhenRootIsNotReady() {
        when(rootRepository.find()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upload("parent-id", "a.txt", "text/plain", 1L, CONTENT))
                .isInstanceOf(SharedFileRootUnavailableException.class);
    }

    @Test
    void throwsWhenParentIsOutsideRoot() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        doThrow(new SharedFileOutOfRootException("outside-id"))
                .when(rootGuard).requireDescendant("access-token", "root-id", "outside-id");

        assertThatThrownBy(() -> service.upload("outside-id", "a.txt", "text/plain", 1L, CONTENT))
                .isInstanceOf(SharedFileOutOfRootException.class);
    }

    private void verify100MbNeverCalledDrive() {
        org.mockito.Mockito.verify(drivePort, never()).upload(any(), any(), any(), any(), any());
    }

    private DriveItem driveItem(String id) {
        return new DriveItem(id, "name", "application/octet-stream", List.of("root-id"), null, true, null, false);
    }
}
