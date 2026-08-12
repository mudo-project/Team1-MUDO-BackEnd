package com.academy.mudogroupware.sharedfile.application.usecase;

import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemsView;

public interface ListSharedFileItemsUseCase {

    SharedFileItemsView list(String parentId, String cursor, int size);
}
