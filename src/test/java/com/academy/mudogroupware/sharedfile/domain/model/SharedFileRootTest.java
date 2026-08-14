package com.academy.mudogroupware.sharedfile.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SharedFileRootTest {

    @Test
    void readyBuildsRootWithGoogleFolderId() {
        SharedFileRoot root = SharedFileRoot.ready("drive-folder-1");

        assertThat(root.isReady()).isTrue();
        assertThat(root.getStatus()).isEqualTo(SharedFileRootStatus.READY);
        assertThat(root.getGoogleRootFolderId()).isEqualTo("drive-folder-1");
    }

    @Test
    void readyThrowsWhenGoogleFolderIdIsBlank() {
        assertThatThrownBy(() -> SharedFileRoot.ready(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void failedBuildsRootWithoutGoogleFolderId() {
        SharedFileRoot root = SharedFileRoot.failed();

        assertThat(root.isReady()).isFalse();
        assertThat(root.getStatus()).isEqualTo(SharedFileRootStatus.FAILED);
        assertThat(root.getGoogleRootFolderId()).isNull();
    }

    @Test
    void markFailedClearsThePreviousGoogleFolderId() {
        SharedFileRoot root = SharedFileRoot.ready("drive-folder-1");

        root.markFailed();

        assertThat(root.isReady()).isFalse();
        assertThat(root.getStatus()).isEqualTo(SharedFileRootStatus.FAILED);
        assertThat(root.getGoogleRootFolderId()).isNull();
    }

    @Test
    void replaceWithMakesTheRootReadyWithTheSuppliedId() {
        SharedFileRoot root = SharedFileRoot.failed();

        root.replaceWith("drive-folder-2");

        assertThat(root.isReady()).isTrue();
        assertThat(root.getStatus()).isEqualTo(SharedFileRootStatus.READY);
        assertThat(root.getGoogleRootFolderId()).isEqualTo("drive-folder-2");
    }

    @Test
    void replaceWithThrowsWhenGoogleFolderIdIsBlank() {
        SharedFileRoot root = SharedFileRoot.failed();

        assertThatThrownBy(() -> root.replaceWith(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void readyBuildsRootWithNullVersionBecauseItIsNotYetPersisted() {
        SharedFileRoot root = SharedFileRoot.ready("drive-folder-1");

        assertThat(root.getVersion()).isNull();
    }

    @Test
    void failedBuildsRootWithNullVersionBecauseItIsNotYetPersisted() {
        SharedFileRoot root = SharedFileRoot.failed();

        assertThat(root.getVersion()).isNull();
    }

    @Test
    void restorePreservesTheVersionReadFromPersistence() {
        SharedFileRoot root = SharedFileRoot.restore(SharedFileRootStatus.READY, "drive-folder-1", 3L);

        assertThat(root.isReady()).isTrue();
        assertThat(root.getGoogleRootFolderId()).isEqualTo("drive-folder-1");
        assertThat(root.getVersion()).isEqualTo(3L);
    }
}
