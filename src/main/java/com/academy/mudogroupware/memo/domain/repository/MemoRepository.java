package com.academy.mudogroupware.memo.domain.repository;

import com.academy.mudogroupware.memo.domain.model.Memo;

public interface MemoRepository {

    Memo save(Memo memo);
}
