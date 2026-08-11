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
import com.academy.mudogroupware.sharedfile.application.port.DrivePage;
import com.academy.mudogroupware.sharedfile.application.port.SharedFileDrivePort;
import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemType;
import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemsView;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileOutOfRootException;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileRootUnavailableException;
import com.academy.mudogroupware.sharedfile.domain.model.SharedFileRoot;
import com.academy.mudogroupware.sharedfile.domain.repository.SharedFileRootRepository;

class SearchSharedFileItemsServiceTest {

    private final SharedFileRootRepository rootRepository = mock(SharedFileRootRepository.class);
    private final SharedFileRootGuard rootGuard = mock(SharedFileRootGuard.class);
    private final SharedFileDrivePort drivePort = mock(SharedFileDrivePort.class);
    private final GetGoogleAccessTokenUseCase getGoogleAccessTokenUseCase = mock(GetGoogleAccessTokenUseCase.class);

    private final SearchSharedFileItemsService service =
            new SearchSharedFileItemsService(rootRepository, rootGuard, drivePort, getGoogleAccessTokenUseCase);

    @Test
    void throwsWhenRootIsNotReady() {
        when(rootRepository.find()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.search("keyword", null, null, 20))
                .isInstanceOf(SharedFileRootUnavailableException.class);
    }

    @Test
    void excludesCandidatesOutsideRoot() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        when(drivePort.searchByName("access-token", "keyword", null, 20)).thenReturn(new DrivePage(
                List.of(file("inside-id"), file("outside-id")), null));
        doThrow(new SharedFileOutOfRootException("outside-id"))
                .when(rootGuard).requireDescendant("access-token", "root-id", "outside-id");

        SharedFileItemsView view = service.search("keyword", null, null, 20);

        assertThat(view.items()).hasSize(1);
        assertThat(view.items().get(0).id()).isEqualTo("inside-id");
    }

    @Test
    void doesNotCallDriveWhenRootIsNotReady() {
        when(rootRepository.find()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.search("keyword", null, null, 20))
                .isInstanceOf(SharedFileRootUnavailableException.class);

        verify(drivePort, never()).searchByName(any(), any(), any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void filtersOutNonFoldersWhenTypeIsFolder() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        when(drivePort.searchByName("access-token", "keyword", null, 20)).thenReturn(new DrivePage(
                List.of(folder("folder-id"), file("file-id")), null));

        SharedFileItemsView view = service.search("keyword", SharedFileItemType.FOLDER, null, 20);

        assertThat(view.items()).extracting("id").containsExactly("folder-id");
    }

    @Test
    void filtersOutFoldersWhenTypeIsFile() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        when(drivePort.searchByName("access-token", "keyword", null, 20)).thenReturn(new DrivePage(
                List.of(folder("folder-id"), file("file-id")), null));

        SharedFileItemsView view = service.search("keyword", SharedFileItemType.FILE, null, 20);

        assertThat(view.items()).extracting("id").containsExactly("file-id");
    }

    private DriveItem file(String id) {
        return new DriveItem(id, "name-" + id, "application/octet-stream", List.of(), null, true, null, false);
    }

    private DriveItem folder(String id) {
        return new DriveItem(id, "name-" + id, "application/vnd.google-apps.folder", List.of(), null, false, null, false);
    }
}
