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
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileOutOfRootException;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileRootUnavailableException;
import com.academy.mudogroupware.sharedfile.domain.model.SharedFileRoot;
import com.academy.mudogroupware.sharedfile.domain.repository.SharedFileRootRepository;

class MoveSharedFileItemServiceTest {

    private final SharedFileRootRepository rootRepository = mock(SharedFileRootRepository.class);
    private final SharedFileRootGuard rootGuard = mock(SharedFileRootGuard.class);
    private final SharedFileDrivePort drivePort = mock(SharedFileDrivePort.class);
    private final GetGoogleAccessTokenUseCase getGoogleAccessTokenUseCase = mock(GetGoogleAccessTokenUseCase.class);

    private final MoveSharedFileItemService service =
            new MoveSharedFileItemService(rootRepository, rootGuard, drivePort, getGoogleAccessTokenUseCase);

    @Test
    void throwsWhenRootIsNotReady() {
        when(rootRepository.find()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.move("item-id", "new-parent-id"))
                .isInstanceOf(SharedFileRootUnavailableException.class);
    }

    @Test
    void throwsWhenItemIsOutsideRoot() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        doThrow(new SharedFileOutOfRootException("outside-id"))
                .when(rootGuard).requireDescendant("access-token", "root-id", "outside-id");

        assertThatThrownBy(() -> service.move("outside-id", "root-id"))
                .isInstanceOf(SharedFileOutOfRootException.class);

        verify(drivePort, never()).move(any(), any(), any(), any());
    }

    @Test
    void throwsWhenDestinationIsOutsideRoot() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        when(drivePort.getItem("access-token", "item-id")).thenReturn(Optional.of(item("item-id", "root-id")));
        doThrow(new SharedFileOutOfRootException("outside-parent-id"))
                .when(rootGuard).requireDescendant("access-token", "root-id", "outside-parent-id");

        assertThatThrownBy(() -> service.move("item-id", "outside-parent-id"))
                .isInstanceOf(SharedFileOutOfRootException.class);

        verify(drivePort, never()).move(any(), any(), any(), any());
    }

    @Test
    void movesItemToDestinationFolderUnderRoot() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        when(drivePort.getItem("access-token", "item-id")).thenReturn(Optional.of(item("item-id", "old-parent-id")));
        when(drivePort.move("access-token", "item-id", "old-parent-id", "new-parent-id"))
                .thenReturn(item("item-id", "new-parent-id"));

        SharedFileItemView view = service.move("item-id", "new-parent-id");

        assertThat(view.id()).isEqualTo("item-id");
    }

    @Test
    void movesItemToRootItselfWithoutGuardingDestination() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        when(drivePort.getItem("access-token", "item-id")).thenReturn(Optional.of(item("item-id", "old-parent-id")));
        when(drivePort.move("access-token", "item-id", "old-parent-id", "root-id"))
                .thenReturn(item("item-id", "root-id"));

        service.move("item-id", "root-id");

        verify(rootGuard, never()).requireDescendant("access-token", "root-id", "root-id");
    }

    private DriveItem item(String id, String parentId) {
        return new DriveItem(id, "name", "application/pdf", List.of(parentId), null, true, null, false);
    }
}
