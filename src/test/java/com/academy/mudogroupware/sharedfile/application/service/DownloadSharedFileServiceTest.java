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
import com.academy.mudogroupware.sharedfile.application.port.DriveBinary;
import com.academy.mudogroupware.sharedfile.application.port.DriveItem;
import com.academy.mudogroupware.sharedfile.application.port.GoogleWorkspaceExportFormat;
import com.academy.mudogroupware.sharedfile.application.port.SharedFileDrivePort;
import com.academy.mudogroupware.sharedfile.application.query.ExportTargetFormat;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileInvalidExportFormatException;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileOutOfRootException;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileRootUnavailableException;
import com.academy.mudogroupware.sharedfile.domain.model.SharedFileRoot;
import com.academy.mudogroupware.sharedfile.domain.repository.SharedFileRootRepository;

class DownloadSharedFileServiceTest {

    private final SharedFileRootRepository rootRepository = mock(SharedFileRootRepository.class);
    private final SharedFileRootGuard rootGuard = mock(SharedFileRootGuard.class);
    private final SharedFileDrivePort drivePort = mock(SharedFileDrivePort.class);
    private final GetGoogleAccessTokenUseCase getGoogleAccessTokenUseCase = mock(GetGoogleAccessTokenUseCase.class);

    private final DownloadSharedFileService service =
            new DownloadSharedFileService(rootRepository, rootGuard, drivePort, getGoogleAccessTokenUseCase);

    @Test
    void throwsWhenRootIsNotReady() {
        when(rootRepository.find()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.download("item-id", null))
                .isInstanceOf(SharedFileRootUnavailableException.class);
    }

    @Test
    void throwsWhenItemIsOutsideRoot() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id", "academy@mudo.co.kr")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        doThrow(new SharedFileOutOfRootException("outside-id"))
                .when(rootGuard).requireDescendant("access-token", "root-id", "outside-id");

        assertThatThrownBy(() -> service.download("outside-id", null))
                .isInstanceOf(SharedFileOutOfRootException.class);

        verify(drivePort, never()).downloadOriginal(any(), any());
        verify(drivePort, never()).export(any(), any(), any());
    }

    @Test
    void downloadsOriginalWhenFormatIsAbsent() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id", "academy@mudo.co.kr")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        when(drivePort.downloadOriginal("access-token", "item-id"))
                .thenReturn(new DriveBinary("bytes".getBytes(), "원본.pdf", "application/pdf"));

        DriveBinary binary = service.download("item-id", null);

        assertThat(binary.filename()).isEqualTo("원본.pdf");
        verify(drivePort, never()).getItem(any(), any());
    }

    @Test
    void throwsWhenFormatRequestedForRegularFile() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id", "academy@mudo.co.kr")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        when(drivePort.getItem("access-token", "item-id")).thenReturn(Optional.of(
                new DriveItem("item-id", "원본.pdf", "application/pdf", List.of("root-id"), null, true, null, false)));

        assertThatThrownBy(() -> service.download("item-id", ExportTargetFormat.PDF))
                .isInstanceOf(SharedFileInvalidExportFormatException.class);
    }

    @Test
    void throwsWhenFormatDoesNotMatchWorkspaceType() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id", "academy@mudo.co.kr")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        when(drivePort.getItem("access-token", "docs-id")).thenReturn(Optional.of(
                new DriveItem("docs-id", "문서", "application/vnd.google-apps.document",
                        List.of("root-id"), null, false, null, false)));

        assertThatThrownBy(() -> service.download("docs-id", ExportTargetFormat.XLSX))
                .isInstanceOf(SharedFileInvalidExportFormatException.class);
    }

    @Test
    void exportsWorkspaceFileInRequestedFormat() {
        when(rootRepository.find()).thenReturn(Optional.of(SharedFileRoot.ready("root-id", "academy@mudo.co.kr")));
        when(getGoogleAccessTokenUseCase.getAccessToken()).thenReturn("access-token");
        when(drivePort.getItem("access-token", "sheet-id")).thenReturn(Optional.of(
                new DriveItem("sheet-id", "시트", "application/vnd.google-apps.spreadsheet",
                        List.of("root-id"), null, false, null, false)));
        when(drivePort.export("access-token", "sheet-id", GoogleWorkspaceExportFormat.SHEETS_XLSX))
                .thenReturn(new DriveBinary("bytes".getBytes(), "시트.xlsx", "application/vnd.openxmlformats"));

        DriveBinary binary = service.download("sheet-id", ExportTargetFormat.XLSX);

        assertThat(binary.filename()).isEqualTo("시트.xlsx");
    }
}
