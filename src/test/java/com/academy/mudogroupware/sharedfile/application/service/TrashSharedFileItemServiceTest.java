package com.academy.mudogroupware.sharedfile.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.google.application.usecase.GetGoogleAccessTokenUseCase;
import com.academy.mudogroupware.sharedfile.application.port.SharedFileDrivePort;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileOutOfRootException;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileRootUnavailableException;
import com.academy.mudogroupware.sharedfile.domain.model.SharedFileRoot;
import com.academy.mudogroupware.sharedfile.domain.repository.SharedFileRootRepository;

class TrashSharedFileItemServiceTest {

    private final SharedFileRootRepository rootRepository = mock(SharedFileRootRepository.class);
    private final SharedFileRootGuard rootGuard = mock(SharedFileRootGuard.class);
    private final SharedFileDrivePort drivePort = mock(SharedFileDrivePort.class);
    private final GetGoogleAccessTokenUseCase getGoogleAccessTokenUseCase = mock(GetGoogleAccessTokenUseCase.class);

    private final TrashSharedFileItemService service =
            new TrashSharedFileItemService(rootRepository, rootGuard, drivePort, getGoogleAccessTokenUseCase);

    @Test
    void throwsWhenRootIsNotReady() {
        when(rootRepository.find()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.trash("item-id"))
                .isInstanceOf(SharedFileRootUnavailableException.class);

        verify(drivePort, never()).trash(any(), any());
    }

    @Test
    void throwsWhenItemIsOutsideRoot() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id", "academy@mudo.co.kr")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        doThrow(new SharedFileOutOfRootException("outside-id"))
                .when(rootGuard).requireDescendant("access-token", "root-id", "outside-id");

        assertThatThrownBy(() -> service.trash("outside-id"))
                .isInstanceOf(SharedFileOutOfRootException.class);

        verify(drivePort, never()).trash(any(), any());
    }

    @Test
    void trashesValidatedItem() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id", "academy@mudo.co.kr")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");

        service.trash("item-id");

        verify(rootGuard).requireDescendant("access-token", "root-id", "item-id");
        verify(drivePort, times(1)).trash("access-token", "item-id");
    }
}
