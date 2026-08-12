package com.academy.mudogroupware.sharedfile.application.usecase;

import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemView;

public interface CreateSharedFolderUseCase {

    SharedFileItemView create(String parentId, String name);
}
