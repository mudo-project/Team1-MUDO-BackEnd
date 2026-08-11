package com.academy.mudogroupware.sharedfile.application.port;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class DriveItemTest {

    @Test
    void isRegularFileIsTrueForOrdinaryUpload() {
        DriveItem item = item("application/pdf");

        assertThat(item.isFolder()).isFalse();
        assertThat(item.workspaceType()).isEmpty();
        assertThat(item.isRegularFile()).isTrue();
    }

    @Test
    void isFolderIsTrueAndNotARegularFile() {
        DriveItem item = item("application/vnd.google-apps.folder");

        assertThat(item.isFolder()).isTrue();
        assertThat(item.isRegularFile()).isFalse();
    }

    @Test
    void workspaceTypeDetectsDocsSheetsSlides() {
        assertThat(item("application/vnd.google-apps.document").workspaceType())
                .contains(GoogleWorkspaceFileType.DOCS);
        assertThat(item("application/vnd.google-apps.spreadsheet").workspaceType())
                .contains(GoogleWorkspaceFileType.SHEETS);
        assertThat(item("application/vnd.google-apps.presentation").workspaceType())
                .contains(GoogleWorkspaceFileType.SLIDES);
    }

    @Test
    void workspaceFileIsNotARegularFile() {
        DriveItem item = item("application/vnd.google-apps.document");

        assertThat(item.isRegularFile()).isFalse();
    }

    private DriveItem item(String mimeType) {
        return new DriveItem("id", "name", mimeType, List.of("root-id"), null, true, null, false);
    }
}
