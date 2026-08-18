package com.academy.mudogroupware.sharedfile.application.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.OptimisticLockingFailureException;

import com.academy.mudogroupware.google.application.event.GoogleAccountConnectedEvent;
import com.academy.mudogroupware.google.application.usecase.GetGoogleAccessTokenUseCase;
import com.academy.mudogroupware.sharedfile.application.port.DriveItem;
import com.academy.mudogroupware.sharedfile.application.port.SharedFileDrivePort;
import com.academy.mudogroupware.sharedfile.domain.model.SharedFileRoot;
import com.academy.mudogroupware.sharedfile.domain.model.SharedFileRootStatus;
import com.academy.mudogroupware.sharedfile.domain.repository.SharedFileRootConnectionHistoryRepository;
import com.academy.mudogroupware.sharedfile.domain.repository.SharedFileRootRepository;

class SharedFileRootInitializerTest {

    private static final Instant NOW = Instant.parse("2026-08-17T00:00:00Z");

    private final SharedFileRootRepository repository = mock(SharedFileRootRepository.class);
    private final SharedFileRootConnectionHistoryRepository connectionHistoryRepository =
            mock(SharedFileRootConnectionHistoryRepository.class);
    private final SharedFileDrivePort drivePort = mock(SharedFileDrivePort.class);
    private final GetGoogleAccessTokenUseCase getGoogleAccessTokenUseCase = mock(GetGoogleAccessTokenUseCase.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private final SharedFileRootInitializer initializer = new SharedFileRootInitializer(
            repository, connectionHistoryRepository, drivePort, getGoogleAccessTokenUseCase, clock);

    @Test
    void createsReadyRootOnFirstConnection() {
        when(repository.find()).thenReturn(Optional.empty());
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        noReusableHistoryFor("academy@mudo.co.kr");
        when(drivePort.createRootFolder("access-token", "이음 그룹웨어 - 공유파일"))
                .thenReturn(driveItem("folder-id", false));

        initializer.handle(new GoogleAccountConnectedEvent("academy@mudo.co.kr"));

        ArgumentCaptor<SharedFileRoot> captor = ArgumentCaptor.forClass(SharedFileRoot.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().isReady()).isTrue();
        assertThat(captor.getValue().getGoogleRootFolderId()).isEqualTo("folder-id");
        assertThat(captor.getValue().getConnectedGoogleEmail()).isEqualTo("academy@mudo.co.kr");
        verify(connectionHistoryRepository).upsert("academy@mudo.co.kr", "folder-id", NOW.atZone(ZoneOffset.UTC).toLocalDateTime());
    }

    @Test
    void keepsExistingReadyRootOnSameAccountReconnection() {
        when(repository.find())
                .thenReturn(Optional.of(SharedFileRoot.ready("existing-folder-id", "academy@mudo.co.kr")));

        initializer.handle(new GoogleAccountConnectedEvent("academy@mudo.co.kr"));

        verify(repository, never()).save(any());
        verify(drivePort, never()).createRootFolder(any(), any());
    }

    @Test
    void recreatesRootOnAccountReplacementWhenNewAccountHasNoHistory() {
        when(repository.find())
                .thenReturn(Optional.of(SharedFileRoot.ready("old-folder-id", "old@mudo.co.kr")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        noReusableHistoryFor("new@mudo.co.kr");
        when(drivePort.createRootFolder("access-token", "이음 그룹웨어 - 공유파일"))
                .thenReturn(driveItem("new-folder-id", false));

        initializer.handle(new GoogleAccountConnectedEvent("new@mudo.co.kr"));

        ArgumentCaptor<SharedFileRoot> captor = ArgumentCaptor.forClass(SharedFileRoot.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().isReady()).isTrue();
        assertThat(captor.getValue().getGoogleRootFolderId()).isEqualTo("new-folder-id");
        assertThat(captor.getValue().getConnectedGoogleEmail()).isEqualTo("new@mudo.co.kr");
    }

    // A -> B -> A 핵심 시나리오: B가 연결돼 있다가 예전에 쓰던 A로 다시 연결하면, A의 이력에 남은
    // 폴더가 지금도 유효하면(존재+휴지통 아님+폴더) 새로 만들지 않고 그대로 재사용한다.
    @Test
    void revivesHistoricalFolderWhenReconnectingToAPreviouslyConnectedAccount() {
        when(repository.find())
                .thenReturn(Optional.of(SharedFileRoot.ready("b-folder-id", "b@mudo.co.kr")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        when(connectionHistoryRepository.findGoogleRootFolderIdByEmail("a@mudo.co.kr"))
                .thenReturn(Optional.of("a-folder-id"));
        when(drivePort.getItem("access-token", "a-folder-id"))
                .thenReturn(Optional.of(driveItem("a-folder-id", false)));

        initializer.handle(new GoogleAccountConnectedEvent("a@mudo.co.kr"));

        ArgumentCaptor<SharedFileRoot> captor = ArgumentCaptor.forClass(SharedFileRoot.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().isReady()).isTrue();
        assertThat(captor.getValue().getGoogleRootFolderId()).isEqualTo("a-folder-id");
        assertThat(captor.getValue().getConnectedGoogleEmail()).isEqualTo("a@mudo.co.kr");
        verify(drivePort, never()).createRootFolder(any(), any());
        verify(connectionHistoryRepository).upsert("a@mudo.co.kr", "a-folder-id", NOW.atZone(ZoneOffset.UTC).toLocalDateTime());
    }

    @Test
    void doesNotReviveWhenHistoricalFolderNoLongerExists() {
        when(repository.find())
                .thenReturn(Optional.of(SharedFileRoot.ready("b-folder-id", "b@mudo.co.kr")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        when(connectionHistoryRepository.findGoogleRootFolderIdByEmail("a@mudo.co.kr"))
                .thenReturn(Optional.of("deleted-folder-id"));
        when(drivePort.getItem("access-token", "deleted-folder-id")).thenReturn(Optional.empty());
        when(drivePort.createRootFolder("access-token", "이음 그룹웨어 - 공유파일"))
                .thenReturn(driveItem("brand-new-folder-id", false));

        initializer.handle(new GoogleAccountConnectedEvent("a@mudo.co.kr"));

        ArgumentCaptor<SharedFileRoot> captor = ArgumentCaptor.forClass(SharedFileRoot.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getGoogleRootFolderId()).isEqualTo("brand-new-folder-id");
    }

    @Test
    void doesNotReviveWhenHistoricalFolderIsTrashed() {
        when(repository.find())
                .thenReturn(Optional.of(SharedFileRoot.ready("b-folder-id", "b@mudo.co.kr")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        when(connectionHistoryRepository.findGoogleRootFolderIdByEmail("a@mudo.co.kr"))
                .thenReturn(Optional.of("trashed-folder-id"));
        when(drivePort.getItem("access-token", "trashed-folder-id"))
                .thenReturn(Optional.of(driveItem("trashed-folder-id", true)));
        when(drivePort.createRootFolder("access-token", "이음 그룹웨어 - 공유파일"))
                .thenReturn(driveItem("brand-new-folder-id", false));

        initializer.handle(new GoogleAccountConnectedEvent("a@mudo.co.kr"));

        ArgumentCaptor<SharedFileRoot> captor = ArgumentCaptor.forClass(SharedFileRoot.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getGoogleRootFolderId()).isEqualTo("brand-new-folder-id");
    }

    @Test
    void retriesCreationWhenExistingRootIsFailed() {
        when(repository.find()).thenReturn(Optional.of(SharedFileRoot.failed()));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        noReusableHistoryFor("academy@mudo.co.kr");
        when(drivePort.createRootFolder("access-token", "이음 그룹웨어 - 공유파일"))
                .thenReturn(driveItem("recovered-folder-id", false));

        initializer.handle(new GoogleAccountConnectedEvent("academy@mudo.co.kr"));

        ArgumentCaptor<SharedFileRoot> captor = ArgumentCaptor.forClass(SharedFileRoot.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().isReady()).isTrue();
    }

    @Test
    void savesFailedRootWhenDriveCreationFails() {
        when(repository.find()).thenReturn(Optional.empty());
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        noReusableHistoryFor("academy@mudo.co.kr");
        when(drivePort.createRootFolder(any(), any())).thenThrow(new RuntimeException("drive down"));

        initializer.handle(new GoogleAccountConnectedEvent("academy@mudo.co.kr"));

        ArgumentCaptor<SharedFileRoot> captor = ArgumentCaptor.forClass(SharedFileRoot.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().isReady()).isFalse();
    }

    @Test
    void doesNotOverwriteWithFailedWhenSaveConflictsWithConcurrentWinner() {
        when(repository.find()).thenReturn(Optional.empty());
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        noReusableHistoryFor("academy@mudo.co.kr");
        when(drivePort.createRootFolder("access-token", "이음 그룹웨어 - 공유파일"))
                .thenReturn(driveItem("folder-id", false));
        when(repository.save(any())).thenThrow(new OptimisticLockingFailureException("concurrent update"));

        assertThatCode(() -> initializer.handle(new GoogleAccountConnectedEvent("academy@mudo.co.kr")))
                .doesNotThrowAnyException();

        // 충돌이 났을 때 "실패로 덮어쓰기"를 재시도하지 않는다 — 먼저 커밋한 쪽의 결과를 신뢰하고 포기한다.
        verify(repository, times(1)).save(any());
        // 대신 저장 직전에 새로 만든 Drive 폴더가 고아로 남지 않도록 trash로 보상한다.
        verify(drivePort).trash("access-token", "folder-id");
    }

    @Test
    void preservesExistingVersionWhenRecreatingOnAccountReplacement() {
        // find()로 읽은 기존 루트의 version(5)이 재생성 후 저장하는 객체에도 그대로 남아있어야
        // SharedFileRootPersistenceAdapter가 insert가 아니라 update로 처리하고 낙관적 락도 걸린다.
        when(repository.find())
                .thenReturn(Optional.of(
                        SharedFileRoot.restore(SharedFileRootStatus.READY, "old-folder-id", "old@mudo.co.kr", 5L)));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        noReusableHistoryFor("new@mudo.co.kr");
        when(drivePort.createRootFolder("access-token", "이음 그룹웨어 - 공유파일"))
                .thenReturn(driveItem("new-folder-id", false));

        initializer.handle(new GoogleAccountConnectedEvent("new@mudo.co.kr"));

        ArgumentCaptor<SharedFileRoot> captor = ArgumentCaptor.forClass(SharedFileRoot.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getVersion()).isEqualTo(5L);
        assertThat(captor.getValue().getGoogleRootFolderId()).isEqualTo("new-folder-id");
    }

    @Test
    void preservesExistingVersionWhenRetryingFailedRoot() {
        when(repository.find())
                .thenReturn(Optional.of(SharedFileRoot.restore(SharedFileRootStatus.FAILED, null, null, 2L)));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        noReusableHistoryFor("academy@mudo.co.kr");
        when(drivePort.createRootFolder("access-token", "이음 그룹웨어 - 공유파일"))
                .thenReturn(driveItem("recovered-folder-id", false));

        initializer.handle(new GoogleAccountConnectedEvent("academy@mudo.co.kr"));

        ArgumentCaptor<SharedFileRoot> captor = ArgumentCaptor.forClass(SharedFileRoot.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getVersion()).isEqualTo(2L);
    }

    private void noReusableHistoryFor(String googleEmail) {
        when(connectionHistoryRepository.findGoogleRootFolderIdByEmail(googleEmail)).thenReturn(Optional.empty());
    }

    private DriveItem driveItem(String id, boolean trashed) {
        return new DriveItem(id, "이음 그룹웨어 - 공유파일", "application/vnd.google-apps.folder",
                java.util.List.of(), null, false, null, trashed);
    }
}
