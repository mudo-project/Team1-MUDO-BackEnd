package com.academy.mudogroupware.sharedfile.application.service;

import org.springframework.stereotype.Service;

import com.academy.mudogroupware.google.application.usecase.GetGoogleAccessTokenUseCase;
import com.academy.mudogroupware.sharedfile.application.port.DriveItem;
import com.academy.mudogroupware.sharedfile.application.port.SharedFileDrivePort;
import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemView;
import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemViewMapper;
import com.academy.mudogroupware.sharedfile.application.usecase.UploadSharedFileUseCase;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileInvalidNameException;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileRootUnavailableException;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileUploadTooLargeException;
import com.academy.mudogroupware.sharedfile.domain.model.SharedFileRoot;
import com.academy.mudogroupware.sharedfile.domain.repository.SharedFileRootRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UploadSharedFileService implements UploadSharedFileUseCase {

    private static final long MAX_UPLOAD_BYTES = 100L * 1024 * 1024;

    private final SharedFileRootRepository sharedFileRootRepository;
    private final SharedFileRootGuard sharedFileRootGuard;
    private final SharedFileDrivePort sharedFileDrivePort;
    private final GetGoogleAccessTokenUseCase getGoogleAccessTokenUseCase;

    @Override
    public SharedFileItemView upload(String parentId, String filename, String contentType, long size, byte[] content) {
        if (size > MAX_UPLOAD_BYTES) {
            throw new SharedFileUploadTooLargeException();
        }
        if (filename == null || filename.isBlank()) {
            throw new SharedFileInvalidNameException();
        }
        SharedFileRoot root = sharedFileRootRepository.find()
                .filter(SharedFileRoot::isReady)
                .orElseThrow(SharedFileRootUnavailableException::new);
        String accessToken = getGoogleAccessTokenUseCase.getAccessToken();

        String rootId = root.getGoogleRootFolderId();
        if (!parentId.equals(rootId)) {
            sharedFileRootGuard.requireDescendant(accessToken, rootId, parentId);
        }

        DriveItem uploaded = sharedFileDrivePort.upload(accessToken, parentId, filename, contentType, content);
        return SharedFileItemViewMapper.toView(uploaded);
    }
}
