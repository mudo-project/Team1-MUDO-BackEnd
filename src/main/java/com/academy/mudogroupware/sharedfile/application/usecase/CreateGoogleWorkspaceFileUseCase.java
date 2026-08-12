package com.academy.mudogroupware.sharedfile.application.usecase;

import com.academy.mudogroupware.sharedfile.application.port.GoogleWorkspaceFileType;
import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemView;

public interface CreateGoogleWorkspaceFileUseCase {

    SharedFileItemView create(String parentId, String name, GoogleWorkspaceFileType type);
}
