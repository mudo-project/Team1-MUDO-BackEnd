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
import com.academy.mudogroupware.sharedfile.domain.model.SharedFileRoot;
import com.academy.mudogroupware.sharedfile.domain.repository.SharedFileRootRepository;
import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;

class CreateSharedFolderServiceTest {

    private final SharedFileRootRepository rootRepository = mock(SharedFileRootRepository.class);
    private final SharedFileRootGuard rootGuard = mock(SharedFileRootGuard.class);
    private final SharedFileDrivePort drivePort = mock(SharedFileDrivePort.class);
    private final GetGoogleAccessTokenUseCase getGoogleAccessTokenUseCase = mock(GetGoogleAccessTokenUseCase.class);

    private final CreateSharedFolderService service =
            new CreateSharedFolderService(rootRepository, rootGuard, drivePort, getGoogleAccessTokenUseCase);

    @Test
    void throwsWhenRootIsNotReady() {
        when(rootRepository.find()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create("parent-id", "새 폴더"))
                .isInstanceOf(SharedFileRootUnavailableException.class);
    }

    @Test
    void throwsWhenNameIsBlank() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id")));

        assertThatThrownBy(() -> service.create("parent-id", "  "))
                .isInstanceOf(SharedFileInvalidNameException.class);

        verify(drivePort, never()).createFolder(any(), any(), any());
    }

    @Test
    void throwsWhenParentIdIsNull() {
        assertThatThrownBy(() -> service.create(null, "새 폴더"))
                .isInstanceOf(BadRequestException.class);

        verify(drivePort, never()).createFolder(any(), any(), any());
    }

    @Test
    void throwsWhenParentIsOutsideRoot() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        doThrow(new SharedFileOutOfRootException("outside-id"))
                .when(rootGuard).requireDescendant("access-token", "root-id", "outside-id");

        assertThatThrownBy(() -> service.create("outside-id", "새 폴더"))
                .isInstanceOf(SharedFileOutOfRootException.class);

        verify(drivePort, never()).createFolder(any(), any(), any());
    }

    @Test
    void createsFolderUnderValidatedParent() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        when(drivePort.createFolder("access-token", "parent-id", "새 폴더")).thenReturn(
                new DriveItem("new-id", "새 폴더", "application/vnd.google-apps.folder",
                        List.of("parent-id"), null, false, null, false));

        SharedFileItemView view = service.create("parent-id", "새 폴더");

        verify(rootGuard).requireDescendant("access-token", "root-id", "parent-id");
        assertThat(view.id()).isEqualTo("new-id");
    }
}
