package com.academy.mudogroupware.memo.domain.repository;

import java.util.List;
import java.util.Optional;

import com.academy.mudogroupware.memo.domain.model.Memo;

public interface MemoRepository {

    Memo save(Memo memo);

    Optional<Memo> findById(Long id);

    List<Memo> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    List<Memo> findAllByUserIdOrderByCreatedAtAsc(Long userId);

    void deleteById(Long id);
}
