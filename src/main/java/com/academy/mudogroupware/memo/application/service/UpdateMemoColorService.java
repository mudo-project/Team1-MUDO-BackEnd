package com.academy.mudogroupware.memo.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.memo.application.command.UpdateMemoColorCommand;
import com.academy.mudogroupware.memo.application.usecase.UpdateMemoColorUseCase;
import com.academy.mudogroupware.memo.domain.exception.MemoErrorCode;
import com.academy.mudogroupware.memo.domain.exception.MemoException;
import com.academy.mudogroupware.memo.domain.model.Memo;
import com.academy.mudogroupware.memo.domain.repository.MemoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateMemoColorService implements UpdateMemoColorUseCase {

    private final MemoRepository memoRepository;
    private final Clock clock;

    @Override
    public void updateColor(UpdateMemoColorCommand command) {
        Memo memo = memoRepository.findById(command.memoId())
                .orElseThrow(() -> new MemoException(MemoErrorCode.MEMO_NOT_FOUND));
        if (!memo.isOwnedBy(command.userId())) {
            throw new MemoException(MemoErrorCode.NOT_MEMO_OWNER);
        }
        memo.updateColor(command.color(), LocalDateTime.now(clock));
        memoRepository.save(memo);
    }
}
