package com.academy.mudogroupware.memo.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.memo.application.query.MemoSortOrder;
import com.academy.mudogroupware.memo.application.usecase.MemoQueryUseCase;
import com.academy.mudogroupware.memo.domain.model.Memo;
import com.academy.mudogroupware.memo.domain.repository.MemoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemoQueryService implements MemoQueryUseCase {

    private final MemoRepository memoRepository;

    @Override
    public List<Memo> getMemos(Long userId, MemoSortOrder sortOrder) {
        if (sortOrder == MemoSortOrder.OLDEST) {
            return memoRepository.findAllByUserIdOrderByCreatedAtAsc(userId);
        }
        return memoRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }
}
