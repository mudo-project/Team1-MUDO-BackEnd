package com.academy.mudogroupware.sharedfile.application.usecase;

import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemView;

public interface MoveSharedFileItemUseCase {

    SharedFileItemView move(String itemId, String newParentId);
}
