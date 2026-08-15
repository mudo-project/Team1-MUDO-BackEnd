package com.academy.mudogroupware.sharedfile.application.service;

import org.springframework.stereotype.Service;

import com.academy.mudogroupware.google.application.usecase.GetGoogleAccessTokenUseCase;
import com.academy.mudogroupware.sharedfile.application.port.DriveBinary;
import com.academy.mudogroupware.sharedfile.application.port.DriveItem;
import com.academy.mudogroupware.sharedfile.application.port.GoogleWorkspaceExportFormat;
import com.academy.mudogroupware.sharedfile.application.port.GoogleWorkspaceFileType;
import com.academy.mudogroupware.sharedfile.application.port.SharedFileDrivePort;
import com.academy.mudogroupware.sharedfile.application.query.ExportTargetFormat;
import com.academy.mudogroupware.sharedfile.application.usecase.DownloadSharedFileUseCase;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileInvalidExportFormatException;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileItemNotFoundException;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileRootUnavailableException;
import com.academy.mudogroupware.sharedfile.domain.model.SharedFileRoot;
import com.academy.mudogroupware.sharedfile.domain.repository.SharedFileRootRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DownloadSharedFileService implements DownloadSharedFileUseCase {

    private final SharedFileRootRepository sharedFileRootRepository;
    private final SharedFileRootGuard sharedFileRootGuard;
    private final SharedFileDrivePort sharedFileDrivePort;
    private final GetGoogleAccessTokenUseCase getGoogleAccessTokenUseCase;

    // format이 없으면 원본 그대로, 있으면 대상이 Google Workspace 파일인지 확인한 뒤 해당 유형에서
    // 지원하는 조합(예: SHEETS_XLSX)인지 확인해 변환 다운로드한다.
    @Override
    public DriveBinary download(String itemId, ExportTargetFormat format) {
        log.info("event=shared_file_download_시작 itemId={} format={}", itemId, format);
        SharedFileRoot root = sharedFileRootRepository.find()
                .filter(SharedFileRoot::isReady)
                .orElseThrow(SharedFileRootUnavailableException::new);
        String accessToken = getGoogleAccessTokenUseCase.getAccessToken();

        sharedFileRootGuard.requireDescendant(accessToken, root.getGoogleRootFolderId(), itemId);

        if (format == null) {
            DriveBinary result = sharedFileDrivePort.downloadOriginal(accessToken, itemId);
            log.info("event=shared_file_download_완료 itemId={} filename={} bytes={}",
                    itemId, result.filename(), result.content().length);
            return result;
        }

        DriveItem current = sharedFileDrivePort.getItem(accessToken, itemId)
                .orElseThrow(() -> new SharedFileItemNotFoundException(itemId));
        GoogleWorkspaceFileType workspaceType = current.workspaceType()
                .orElseThrow(SharedFileInvalidExportFormatException::new);
        GoogleWorkspaceExportFormat exportFormat = resolveExportFormat(workspaceType, format);

        DriveBinary result = sharedFileDrivePort.export(accessToken, itemId, exportFormat);
        log.info("event=shared_file_download_완료 itemId={} filename={} bytes={}",
                itemId, result.filename(), result.content().length);
        return result;
    }

    private GoogleWorkspaceExportFormat resolveExportFormat(GoogleWorkspaceFileType workspaceType, ExportTargetFormat format) {
        try {
            return GoogleWorkspaceExportFormat.valueOf(workspaceType.name() + "_" + format.name());
        } catch (IllegalArgumentException e) {
            throw new SharedFileInvalidExportFormatException();
        }
    }
}
