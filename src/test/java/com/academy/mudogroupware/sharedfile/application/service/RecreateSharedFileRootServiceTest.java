package com.academy.mudogroupware.sharedfile.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.google.application.usecase.GetGoogleAccessTokenUseCase;
import com.academy.mudogroupware.sharedfile.application.port.DriveItem;
import com.academy.mudogroupware.sharedfile.application.port.SharedFileDrivePort;
import com.academy.mudogroupware.sharedfile.application.query.SharedFileRootView;
import com.academy.mudogroupware.sharedfile.domain.model.SharedFileRoot;
import com.academy.mudogroupware.sharedfile.domain.repository.SharedFileRootRepository;

class RecreateSharedFileRootServiceTest {

    private final SharedFileRootRepository sharedFileRootRepository = mock(SharedFileRootRepository.class);
    private final SharedFileDrivePort sharedFileDrivePort = mock(SharedFileDrivePort.class);
    private final GetGoogleAccessTokenUseCase getGoogleAccessTokenUseCase =
            mock(GetGoogleAccessTokenUseCase.class);
    private final RecreateSharedFileRootService service = new RecreateSharedFileRootService(
            sharedFileRootRepository, sharedFileDrivePort, getGoogleAccessTokenUseCase);

    @Test
    void rejectsRecreationWhenRootIsAlreadyReady() {
        when(sharedFileRootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("existing-folder-id")));

        assertThatThrownBy(service::recreate).isInstanceOf(IllegalStateException.class);

        verify(getGoogleAccessTokenUseCase, never()).getAccessToken();
        verify(sharedFileDrivePort, never()).createRootFolder(anyString(), anyString());
    }

    @Test
    void recreatesRootWhenNoRootRowExists() {
        when(sharedFileRootRepository.find()).thenReturn(Optional.empty());
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        when(sharedFileDrivePort.createRootFolder(anyString(), anyString()))
                .thenReturn(newFolder("new-folder-id"));

        SharedFileRootView view = service.recreate();

        assertThat(view.ready()).isTrue();
        verify(sharedFileRootRepository).save(any(SharedFileRoot.class));
    }

    @Test
    void recreatesRootWhenExistingRootIsFailed() {
        when(sharedFileRootRepository.find()).thenReturn(Optional.of(SharedFileRoot.failed()));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        when(sharedFileDrivePort.createRootFolder(anyString(), anyString()))
                .thenReturn(newFolder("recreated-folder-id"));

        SharedFileRootView view = service.recreate();

        assertThat(view.ready()).isTrue();
        verify(sharedFileRootRepository).save(any(SharedFileRoot.class));
    }

    private DriveItem newFolder(String id) {
        return new DriveItem(id, "이음 그룹웨어 - 공유파일", "application/vnd.google-apps.folder",
                List.of(), null, false, LocalDateTime.now(), false);
    }
}
