package com.academy.mudogroupware.sharedfile.application.usecase;

import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemType;
import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemsView;

public interface SearchSharedFileItemsUseCase {

    SharedFileItemsView search(String keyword, SharedFileItemType type, String cursor, int size);
}
