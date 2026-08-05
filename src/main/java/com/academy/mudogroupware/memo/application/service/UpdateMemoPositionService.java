package com.academy.mudogroupware.memo.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.memo.application.command.UpdateMemoPositionCommand;
import com.academy.mudogroupware.memo.application.usecase.UpdateMemoPositionUseCase;
import com.academy.mudogroupware.memo.domain.exception.MemoErrorCode;
import com.academy.mudogroupware.memo.domain.exception.MemoException;
import com.academy.mudogroupware.memo.domain.model.Memo;
import com.academy.mudogroupware.memo.domain.repository.MemoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateMemoPositionService implements UpdateMemoPositionUseCase {

    private final MemoRepository memoRepository;
    private final Clock clock;

    @Override
    public void updatePosition(UpdateMemoPositionCommand command) {
        Memo memo = memoRepository.findById(command.memoId())
                .orElseThrow(() -> new MemoException(MemoErrorCode.MEMO_NOT_FOUND));
        if (!memo.isOwnedBy(command.userId())) {
            throw new MemoException(MemoErrorCode.NOT_MEMO_OWNER);
        }
        memo.updatePosition(command.positionX(), command.positionY(), command.width(), command.height(),
                LocalDateTime.now(clock));
        memoRepository.save(memo);
    }
}
