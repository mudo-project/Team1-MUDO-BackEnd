package com.academy.mudogroupware.memo.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.memo.application.command.DeleteMemoCommand;
import com.academy.mudogroupware.memo.application.usecase.DeleteMemoUseCase;
import com.academy.mudogroupware.memo.domain.exception.MemoErrorCode;
import com.academy.mudogroupware.memo.domain.exception.MemoException;
import com.academy.mudogroupware.memo.domain.model.Memo;
import com.academy.mudogroupware.memo.domain.repository.MemoRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DeleteMemoService implements DeleteMemoUseCase {

    private final MemoRepository memoRepository;

    @Override
    public void deleteMemo(DeleteMemoCommand command) {
        log.info("event=memo_delete_시작 memoId={}, userId={}", command.memoId(), command.userId());
        Memo memo = memoRepository.findById(command.memoId())
                .orElseThrow(() -> new MemoException(MemoErrorCode.MEMO_NOT_FOUND));
        if (!memo.isOwnedBy(command.userId())) {
            throw new MemoException(MemoErrorCode.NOT_MEMO_OWNER);
        }
        memoRepository.deleteById(command.memoId());
        log.info("event=memo_delete_완료 memoId={}, userId={}", command.memoId(), command.userId());
    }
}
