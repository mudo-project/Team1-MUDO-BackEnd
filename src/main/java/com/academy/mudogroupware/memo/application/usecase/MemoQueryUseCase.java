package com.academy.mudogroupware.memo.application.usecase;

import java.util.List;

import com.academy.mudogroupware.memo.application.query.MemoSortOrder;
import com.academy.mudogroupware.memo.domain.model.Memo;

public interface MemoQueryUseCase {

    List<Memo> getMemos(Long userId, MemoSortOrder sortOrder);
}
