package com.academy.mudogroupware.sharedfile.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

class UpdateSharedFileItemServiceTest {

    private final SharedFileRootRepository rootRepository = mock(SharedFileRootRepository.class);
    private final SharedFileRootGuard rootGuard = mock(SharedFileRootGuard.class);
    private final SharedFileDrivePort drivePort = mock(SharedFileDrivePort.class);
    private final GetGoogleAccessTokenUseCase getGoogleAccessTokenUseCase = mock(GetGoogleAccessTokenUseCase.class);

    private final UpdateSharedFileItemService service =
            new UpdateSharedFileItemService(rootRepository, rootGuard, drivePort, getGoogleAccessTokenUseCase);

    @Test
    void throwsWhenRootIsNotReady() {
        when(rootRepository.find()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update("item-id", "새 이름.pdf", null))
                .isInstanceOf(SharedFileRootUnavailableException.class);
    }

    @Test
    void throwsWhenItemIsOutsideRoot() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        doThrow(new SharedFileOutOfRootException("outside-id"))
                .when(rootGuard).requireDescendant("access-token", "root-id", "outside-id");

        assertThatThrownBy(() -> service.update("outside-id", null, "root-id"))
                .isInstanceOf(SharedFileOutOfRootException.class);

        verify(drivePort, never()).updateItem(any(), any(), any(), any(), any());
    }

    // --- 이름 변경만 (parentId 없음) ---

    @Test
    void throwsWhenRegularFileExtensionDiffers() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        when(drivePort.getItem("access-token", "item-id")).thenReturn(Optional.of(
                regularFile("item-id", "원본.pdf", "root-id")));

        assertThatThrownBy(() -> service.update("item-id", "새이름.docx", null))
                .isInstanceOf(SharedFileInvalidNameException.class);

        verify(drivePort, never()).updateItem(any(), any(), any(), any(), any());
    }

    @Test
    void renamesRegularFileWhenExtensionMatches() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        when(drivePort.getItem("access-token", "item-id")).thenReturn(Optional.of(
                regularFile("item-id", "원본.pdf", "root-id")));
        when(drivePort.updateItem("access-token", "item-id", "새이름.pdf", null, null))
                .thenReturn(regularFile("item-id", "새이름.pdf", "root-id"));

        SharedFileItemView view = service.update("item-id", "새이름.pdf", null);

        verify(rootGuard).requireDescendant("access-token", "root-id", "item-id");
        assertThat(view.name()).isEqualTo("새이름.pdf");
    }

    @Test
    void renamesFolderRegardlessOfExtensionLikeSuffix() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        when(drivePort.getItem("access-token", "folder-id")).thenReturn(Optional.of(
                folder("folder-id", "root-id")));
        when(drivePort.updateItem("access-token", "folder-id", "새 폴더.v2", null, null))
                .thenReturn(folder("folder-id", "root-id"));

        SharedFileItemView view = service.update("folder-id", "새 폴더.v2", null);

        assertThat(view.id()).isEqualTo("folder-id");
    }

    // --- 이동만 (name 없음) ---

    @Test
    void throwsWhenDestinationIsOutsideRoot() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        doThrow(new SharedFileOutOfRootException("outside-parent-id"))
                .when(rootGuard).requireDescendant("access-token", "root-id", "outside-parent-id");

        assertThatThrownBy(() -> service.update("item-id", null, "outside-parent-id"))
                .isInstanceOf(SharedFileOutOfRootException.class);

        verify(drivePort, never()).updateItem(any(), any(), any(), any(), any());
    }

    @Test
    void throwsWhenNewParentIsTheItemItself() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");

        assertThatThrownBy(() -> service.update("item-id", null, "item-id"))
                .isInstanceOf(SharedFileOutOfRootException.class);

        verify(drivePort, never()).getItem(any(), any());
        verify(drivePort, never()).updateItem(any(), any(), any(), any(), any());
    }

    @Test
    void throwsWhenDestinationIsARegularFileNotAFolder() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        when(drivePort.getItem("access-token", "file-id")).thenReturn(Optional.of(
                regularFile("file-id", "name", "root-id")));

        assertThatThrownBy(() -> service.update("item-id", null, "file-id"))
                .isInstanceOf(SharedFileOutOfRootException.class);

        verify(drivePort, never()).updateItem(any(), any(), any(), any(), any());
    }

    @Test
    void throwsWhenDestinationIsADescendantOfTheItemBeingMoved() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        // child-id의 부모가 곧 item-id다 — item-id를 그 자신의 하위(child-id)로 옮기면 순환이 생긴다.
        when(drivePort.getItem("access-token", "child-id")).thenReturn(Optional.of(
                folder("child-id", "item-id")));

        assertThatThrownBy(() -> service.update("item-id", null, "child-id"))
                .isInstanceOf(SharedFileOutOfRootException.class);

        verify(drivePort, never()).updateItem(any(), any(), any(), any(), any());
    }

    @Test
    void movesItemToDestinationFolderUnderRoot() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        when(drivePort.getItem("access-token", "new-parent-id")).thenReturn(Optional.of(
                folder("new-parent-id", "root-id")));
        when(drivePort.getItem("access-token", "item-id")).thenReturn(Optional.of(
                regularFile("item-id", "name", "old-parent-id")));
        when(drivePort.updateItem("access-token", "item-id", null, "old-parent-id", "new-parent-id"))
                .thenReturn(regularFile("item-id", "name", "new-parent-id"));

        SharedFileItemView view = service.update("item-id", null, "new-parent-id");

        assertThat(view.id()).isEqualTo("item-id");
    }

    @Test
    void movesItemToRootItselfWithoutGuardingOrCheckingDestination() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        when(drivePort.getItem("access-token", "item-id")).thenReturn(Optional.of(
                regularFile("item-id", "name", "old-parent-id")));
        when(drivePort.updateItem("access-token", "item-id", null, "old-parent-id", "root-id"))
                .thenReturn(regularFile("item-id", "name", "root-id"));

        service.update("item-id", null, "root-id");

        verify(rootGuard, never()).requireDescendant("access-token", "root-id", "root-id");
        verify(drivePort, never()).getItem("access-token", "root-id");
    }

    // --- 이름 변경 + 이동 동시(원자성의 핵심) ---

    // 이름변경+이동 원자성의 핵심 증거: 두 변경이 SharedFileDrivePort.updateItem() 호출 1번에 함께 실린다
    // (예전처럼 rename() 호출 후 move() 호출을 별도로 하지 않는다 — 그 사이에 한쪽만 성공하는 부분 실패가 없다).
    @Test
    void updatesNameAndParentInASingleDrivePortCallWhenBothGiven() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        when(drivePort.getItem("access-token", "new-parent-id")).thenReturn(Optional.of(
                folder("new-parent-id", "root-id")));
        when(drivePort.getItem("access-token", "item-id")).thenReturn(Optional.of(
                regularFile("item-id", "원본.pdf", "old-parent-id")));
        when(drivePort.updateItem("access-token", "item-id", "새이름.pdf", "old-parent-id", "new-parent-id"))
                .thenReturn(regularFile("item-id", "새이름.pdf", "new-parent-id"));

        SharedFileItemView view = service.update("item-id", "새이름.pdf", "new-parent-id");

        verify(drivePort, times(1)).updateItem(any(), any(), any(), any(), any());
        assertThat(view.name()).isEqualTo("새이름.pdf");
    }

    private DriveItem regularFile(String id, String name, String parentId) {
        return new DriveItem(id, name, "application/pdf", List.of(parentId), null, true, null, false);
    }

    private DriveItem folder(String id, String parentId) {
        return new DriveItem(id, "name", "application/vnd.google-apps.folder", List.of(parentId), null, false, null,
                false);
    }
}
