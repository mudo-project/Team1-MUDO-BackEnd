package com.academy.mudogroupware.sharedfile.application.usecase;

import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemView;

public interface GetSharedFileItemUseCase {

    SharedFileItemView get(String itemId);
}
