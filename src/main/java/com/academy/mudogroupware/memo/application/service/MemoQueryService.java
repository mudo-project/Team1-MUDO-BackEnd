package com.academy.mudogroupware.memo.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.memo.application.query.MemoSortOrder;
import com.academy.mudogroupware.memo.application.usecase.MemoQueryUseCase;
import com.academy.mudogroupware.memo.domain.model.Memo;
import com.academy.mudogroupware.memo.domain.repository.MemoRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemoQueryService implements MemoQueryUseCase {

    private final MemoRepository memoRepository;

    @Override
    public List<Memo> getMemos(Long userId, MemoSortOrder sortOrder) {
        log.info("event=memo_list_시작 userId={}, sortOrder={}", userId, sortOrder);
        List<Memo> memos = sortOrder == MemoSortOrder.OLDEST
                ? memoRepository.findAllByUserIdOrderByCreatedAtAscIdAsc(userId)
                : memoRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(userId);
        log.info("event=memo_list_완료 userId={}, sortOrder={}, count={}", userId, sortOrder, memos.size());
        return memos;
    }
}
