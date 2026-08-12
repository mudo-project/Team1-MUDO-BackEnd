package com.academy.mudogroupware.sharedfile.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileRootUnavailableException;
import com.academy.mudogroupware.sharedfile.domain.model.SharedFileRoot;
import com.academy.mudogroupware.sharedfile.domain.repository.SharedFileRootRepository;

class RenameSharedFileItemServiceTest {

    private final SharedFileRootRepository rootRepository = mock(SharedFileRootRepository.class);
    private final SharedFileRootGuard rootGuard = mock(SharedFileRootGuard.class);
    private final SharedFileDrivePort drivePort = mock(SharedFileDrivePort.class);
    private final GetGoogleAccessTokenUseCase getGoogleAccessTokenUseCase = mock(GetGoogleAccessTokenUseCase.class);

    private final RenameSharedFileItemService service =
            new RenameSharedFileItemService(rootRepository, rootGuard, drivePort, getGoogleAccessTokenUseCase);

    @Test
    void throwsWhenNewNameIsBlank() {
        assertThatThrownBy(() -> service.rename("item-id", " "))
                .isInstanceOf(SharedFileInvalidNameException.class);
    }

    @Test
    void throwsWhenRootIsNotReady() {
        when(rootRepository.find()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rename("item-id", "새 이름.pdf"))
                .isInstanceOf(SharedFileRootUnavailableException.class);
    }

    @Test
    void throwsWhenRegularFileExtensionDiffers() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        when(drivePort.getItem("access-token", "item-id")).thenReturn(Optional.of(regularFile("원본.pdf")));

        assertThatThrownBy(() -> service.rename("item-id", "새이름.docx"))
                .isInstanceOf(SharedFileInvalidNameException.class);

        verify(drivePort, never()).rename(any(), any(), any());
    }

    @Test
    void renamesRegularFileWhenExtensionMatches() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        when(drivePort.getItem("access-token", "item-id")).thenReturn(Optional.of(regularFile("원본.pdf")));
        when(drivePort.rename("access-token", "item-id", "새이름.pdf")).thenReturn(regularFile("새이름.pdf"));

        SharedFileItemView view = service.rename("item-id", "새이름.pdf");

        verify(rootGuard).requireDescendant("access-token", "root-id", "item-id");
        assertThat(view.name()).isEqualTo("새이름.pdf");
    }

    @Test
    void renamesFolderRegardlessOfExtensionLikeSuffix() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        when(drivePort.getItem("access-token", "folder-id")).thenReturn(Optional.of(
                new DriveItem("folder-id", "기존 폴더", "application/vnd.google-apps.folder",
                        List.of("root-id"), null, false, null, false)));
        when(drivePort.rename("access-token", "folder-id", "새 폴더.v2")).thenReturn(
                new DriveItem("folder-id", "새 폴더.v2", "application/vnd.google-apps.folder",
                        List.of("root-id"), null, false, null, false));

        SharedFileItemView view = service.rename("folder-id", "새 폴더.v2");

        assertThat(view.name()).isEqualTo("새 폴더.v2");
    }

    private DriveItem regularFile(String name) {
        return new DriveItem("item-id", name, "application/pdf", List.of("root-id"), null, true, null, false);
    }
}
