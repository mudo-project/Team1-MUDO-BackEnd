package com.academy.mudogroupware.sharedfile.application.usecase;

import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemView;

public interface RenameSharedFileItemUseCase {

    SharedFileItemView rename(String itemId, String newName);
}
