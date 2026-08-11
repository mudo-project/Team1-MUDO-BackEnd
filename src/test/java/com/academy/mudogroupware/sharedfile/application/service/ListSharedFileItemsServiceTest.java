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
import com.academy.mudogroupware.sharedfile.application.port.DrivePage;
import com.academy.mudogroupware.sharedfile.application.port.SharedFileDrivePort;
import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemsView;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileOutOfRootException;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileRootUnavailableException;
import com.academy.mudogroupware.sharedfile.domain.model.SharedFileRoot;
import com.academy.mudogroupware.sharedfile.domain.repository.SharedFileRootRepository;

class ListSharedFileItemsServiceTest {

    private final SharedFileRootRepository rootRepository = mock(SharedFileRootRepository.class);
    private final SharedFileRootGuard rootGuard = mock(SharedFileRootGuard.class);
    private final SharedFileDrivePort drivePort = mock(SharedFileDrivePort.class);
    private final GetGoogleAccessTokenUseCase getGoogleAccessTokenUseCase = mock(GetGoogleAccessTokenUseCase.class);

    private final ListSharedFileItemsService service =
            new ListSharedFileItemsService(rootRepository, rootGuard, drivePort, getGoogleAccessTokenUseCase);

    @Test
    void throwsWhenRootIsNotReady() {
        when(rootRepository.find()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.list(null, null, 20))
                .isInstanceOf(SharedFileRootUnavailableException.class);

        verify(drivePort, never()).listChildren(any(), any(), any(), anyInt());
    }

    @Test
    void listsRootChildrenWhenParentIdIsAbsent() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        when(drivePort.listChildren("access-token", "root-id", null, 20))
                .thenReturn(new DrivePage(List.of(item("child-1")), "next-cursor"));

        SharedFileItemsView view = service.list(null, null, 20);

        assertThat(view.items()).hasSize(1);
        assertThat(view.hasNext()).isTrue();
        assertThat(view.nextCursor()).isEqualTo("next-cursor");
        verify(rootGuard, never()).requireDescendant(any(), any(), any());
    }

    @Test
    void listsGivenParentAfterValidatingItIsUnderRoot() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        when(drivePort.listChildren("access-token", "child-folder-id", null, 20))
                .thenReturn(new DrivePage(List.of(), null));

        SharedFileItemsView view = service.list("child-folder-id", null, 20);

        verify(rootGuard).requireDescendant("access-token", "root-id", "child-folder-id");
        assertThat(view.hasNext()).isFalse();
    }

    @Test
    void propagatesGuardRejectionForOutOfRootParent() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        org.mockito.Mockito.doThrow(new SharedFileOutOfRootException("outside-id"))
                .when(rootGuard).requireDescendant("access-token", "root-id", "outside-id");

        assertThatThrownBy(() -> service.list("outside-id", null, 20))
                .isInstanceOf(SharedFileOutOfRootException.class);

        verify(drivePort, never()).listChildren(any(), any(), any(), anyInt());
    }

    private DriveItem item(String id) {
        return new DriveItem(id, "name", "application/octet-stream", List.of("root-id"), null, true, null, false);
    }

    private static int anyInt() {
        return org.mockito.ArgumentMatchers.anyInt();
    }
}
