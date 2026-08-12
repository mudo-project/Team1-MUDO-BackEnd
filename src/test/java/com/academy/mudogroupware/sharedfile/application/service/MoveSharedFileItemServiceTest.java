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
        doThrow(new SharedFileOutOfRootException("outside-parent-id"))
                .when(rootGuard).requireDescendant("access-token", "root-id", "outside-parent-id");

        assertThatThrownBy(() -> service.move("item-id", "outside-parent-id"))
                .isInstanceOf(SharedFileOutOfRootException.class);

        verify(drivePort, never()).move(any(), any(), any(), any());
    }

    @Test
    void throwsWhenNewParentIsTheItemItself() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");

        assertThatThrownBy(() -> service.move("item-id", "item-id"))
                .isInstanceOf(SharedFileOutOfRootException.class);

        verify(drivePort, never()).getItem(any(), any());
        verify(drivePort, never()).move(any(), any(), any(), any());
    }

    @Test
    void throwsWhenDestinationIsARegularFileNotAFolder() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        when(drivePort.getItem("access-token", "file-id")).thenReturn(Optional.of(
                regularFile("file-id", "root-id")));

        assertThatThrownBy(() -> service.move("item-id", "file-id"))
                .isInstanceOf(SharedFileOutOfRootException.class);

        verify(drivePort, never()).move(any(), any(), any(), any());
    }

    @Test
    void throwsWhenDestinationIsADescendantOfTheItemBeingMoved() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        // child-id의 부모가 곧 item-id다 — item-id를 그 자신의 하위(child-id)로 옮기면 순환이 생긴다.
        when(drivePort.getItem("access-token", "child-id")).thenReturn(Optional.of(
                folder("child-id", "item-id")));

        assertThatThrownBy(() -> service.move("item-id", "child-id"))
                .isInstanceOf(SharedFileOutOfRootException.class);

        verify(drivePort, never()).move(any(), any(), any(), any());
    }

    @Test
    void movesItemToDestinationFolderUnderRoot() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        when(drivePort.getItem("access-token", "new-parent-id")).thenReturn(Optional.of(
                folder("new-parent-id", "root-id")));
        when(drivePort.getItem("access-token", "item-id")).thenReturn(Optional.of(
                regularFile("item-id", "old-parent-id")));
        when(drivePort.move("access-token", "item-id", "old-parent-id", "new-parent-id"))
                .thenReturn(regularFile("item-id", "new-parent-id"));

        SharedFileItemView view = service.move("item-id", "new-parent-id");

        assertThat(view.id()).isEqualTo("item-id");
    }

    @Test
    void movesItemToRootItselfWithoutGuardingOrCheckingDestination() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        when(drivePort.getItem("access-token", "item-id")).thenReturn(Optional.of(
                regularFile("item-id", "old-parent-id")));
        when(drivePort.move("access-token", "item-id", "old-parent-id", "root-id"))
                .thenReturn(regularFile("item-id", "root-id"));

        service.move("item-id", "root-id");

        verify(rootGuard, never()).requireDescendant("access-token", "root-id", "root-id");
        verify(drivePort, never()).getItem("access-token", "root-id");
    }

    private DriveItem regularFile(String id, String parentId) {
        return new DriveItem(id, "name", "application/pdf", List.of(parentId), null, true, null, false);
    }

    private DriveItem folder(String id, String parentId) {
        return new DriveItem(id, "name", "application/vnd.google-apps.folder", List.of(parentId), null, false, null, false);
    }
}
