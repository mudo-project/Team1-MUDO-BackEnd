package com.academy.mudogroupware.sharedfile.application.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.academy.mudogroupware.google.application.event.GoogleAccountConnectedEvent;
import com.academy.mudogroupware.google.application.usecase.GetGoogleAccessTokenUseCase;
import com.academy.mudogroupware.sharedfile.application.port.DriveItem;
import com.academy.mudogroupware.sharedfile.application.port.SharedFileDrivePort;
import com.academy.mudogroupware.sharedfile.domain.model.SharedFileRoot;
import com.academy.mudogroupware.sharedfile.domain.repository.SharedFileRootRepository;

class SharedFileRootInitializerTest {

    private final SharedFileRootRepository repository = mock(SharedFileRootRepository.class);
    private final SharedFileDrivePort drivePort = mock(SharedFileDrivePort.class);
    private final GetGoogleAccessTokenUseCase getGoogleAccessTokenUseCase = mock(GetGoogleAccessTokenUseCase.class);

    private final SharedFileRootInitializer initializer =
            new SharedFileRootInitializer(repository, drivePort, getGoogleAccessTokenUseCase);

    @Test
    void createsReadyRootOnFirstConnection() {
        when(repository.find()).thenReturn(Optional.empty());
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        when(drivePort.createRootFolder("access-token", "이음 그룹웨어 - 공유파일"))
                .thenReturn(driveItem("folder-id"));

        initializer.handle(new GoogleAccountConnectedEvent(false));

        ArgumentCaptor<SharedFileRoot> captor = ArgumentCaptor.forClass(SharedFileRoot.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().isReady()).isTrue();
        assertThat(captor.getValue().getGoogleRootFolderId()).isEqualTo("folder-id");
    }

    @Test
    void keepsExistingReadyRootOnSameAccountReconnection() {
        when(repository.find()).thenReturn(Optional.of(SharedFileRoot.ready("existing-folder-id")));

        initializer.handle(new GoogleAccountConnectedEvent(false));

        verify(repository, never()).save(any());
        verify(drivePort, never()).createRootFolder(any(), any());
    }

    @Test
    void recreatesRootOnAccountReplacement() {
        when(repository.find()).thenReturn(Optional.of(SharedFileRoot.ready("old-folder-id")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        when(drivePort.createRootFolder("access-token", "이음 그룹웨어 - 공유파일"))
                .thenReturn(driveItem("new-folder-id"));

        initializer.handle(new GoogleAccountConnectedEvent(true));

        ArgumentCaptor<SharedFileRoot> captor = ArgumentCaptor.forClass(SharedFileRoot.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().isReady()).isTrue();
        assertThat(captor.getValue().getGoogleRootFolderId()).isEqualTo("new-folder-id");
    }

    @Test
    void retriesCreationWhenExistingRootIsFailed() {
        when(repository.find()).thenReturn(Optional.of(SharedFileRoot.failed()));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        when(drivePort.createRootFolder("access-token", "이음 그룹웨어 - 공유파일"))
                .thenReturn(driveItem("recovered-folder-id"));

        initializer.handle(new GoogleAccountConnectedEvent(false));

        ArgumentCaptor<SharedFileRoot> captor = ArgumentCaptor.forClass(SharedFileRoot.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().isReady()).isTrue();
    }

    @Test
    void savesFailedRootWhenDriveCreationFails() {
        when(repository.find()).thenReturn(Optional.empty());
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        when(drivePort.createRootFolder(any(), any())).thenThrow(new RuntimeException("drive down"));

        initializer.handle(new GoogleAccountConnectedEvent(false));

        ArgumentCaptor<SharedFileRoot> captor = ArgumentCaptor.forClass(SharedFileRoot.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().isReady()).isFalse();
    }

    private DriveItem driveItem(String id) {
        return new DriveItem(id, "이음 그룹웨어 - 공유파일", "application/vnd.google-apps.folder",
                java.util.List.of(), null, false, null, false);
    }
}
