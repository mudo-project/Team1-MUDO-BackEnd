package com.academy.mudogroupware.sharedfile.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SharedFileRootTest {

    @Test
    void readyBuildsRootWithGoogleFolderIdAndConnectedEmail() {
        SharedFileRoot root = SharedFileRoot.ready("drive-folder-1", "academy@mudo.co.kr");

        assertThat(root.isReady()).isTrue();
        assertThat(root.getStatus()).isEqualTo(SharedFileRootStatus.READY);
        assertThat(root.getGoogleRootFolderId()).isEqualTo("drive-folder-1");
        assertThat(root.getConnectedGoogleEmail()).isEqualTo("academy@mudo.co.kr");
    }

    @Test
    void readyThrowsWhenGoogleFolderIdIsBlank() {
        assertThatThrownBy(() -> SharedFileRoot.ready(" ", "academy@mudo.co.kr"))
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
    void markFailedClearsThePreviousGoogleFolderIdAndConnectedEmail() {
        SharedFileRoot root = SharedFileRoot.ready("drive-folder-1", "academy@mudo.co.kr");

        root.markFailed();

        assertThat(root.isReady()).isFalse();
        assertThat(root.getStatus()).isEqualTo(SharedFileRootStatus.FAILED);
        assertThat(root.getGoogleRootFolderId()).isNull();
        assertThat(root.getConnectedGoogleEmail()).isNull();
    }

    @Test
    void replaceWithMakesTheRootReadyWithTheSuppliedIdAndEmail() {
        SharedFileRoot root = SharedFileRoot.failed();

        root.replaceWith("drive-folder-2", "new@mudo.co.kr");

        assertThat(root.isReady()).isTrue();
        assertThat(root.getStatus()).isEqualTo(SharedFileRootStatus.READY);
        assertThat(root.getGoogleRootFolderId()).isEqualTo("drive-folder-2");
        assertThat(root.getConnectedGoogleEmail()).isEqualTo("new@mudo.co.kr");
    }

    @Test
    void replaceWithThrowsWhenGoogleFolderIdIsBlank() {
        SharedFileRoot root = SharedFileRoot.failed();

        assertThatThrownBy(() -> root.replaceWith("", "new@mudo.co.kr"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void readyBuildsRootWithNullVersionBecauseItIsNotYetPersisted() {
        SharedFileRoot root = SharedFileRoot.ready("drive-folder-1", "academy@mudo.co.kr");

        assertThat(root.getVersion()).isNull();
    }

    @Test
    void failedBuildsRootWithNullVersionBecauseItIsNotYetPersisted() {
        SharedFileRoot root = SharedFileRoot.failed();

        assertThat(root.getVersion()).isNull();
    }

    @Test
    void restorePreservesTheVersionAndConnectedEmailReadFromPersistence() {
        SharedFileRoot root =
                SharedFileRoot.restore(SharedFileRootStatus.READY, "drive-folder-1", "academy@mudo.co.kr", 3L);

        assertThat(root.isReady()).isTrue();
        assertThat(root.getGoogleRootFolderId()).isEqualTo("drive-folder-1");
        assertThat(root.getConnectedGoogleEmail()).isEqualTo("academy@mudo.co.kr");
        assertThat(root.getVersion()).isEqualTo(3L);
    }
}
