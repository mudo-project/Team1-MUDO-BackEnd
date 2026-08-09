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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UpdateMemoPositionService implements UpdateMemoPositionUseCase {

    private final MemoRepository memoRepository;
    private final Clock clock;

    @Override
    public void updatePosition(UpdateMemoPositionCommand command) {
        log.info("event=memo_position_update_시작 memoId={}, userId={}", command.memoId(), command.userId());
        try {
            Memo memo = memoRepository.findById(command.memoId())
                    .orElseThrow(() -> new MemoException(MemoErrorCode.MEMO_NOT_FOUND));
            if (!memo.isOwnedBy(command.userId())) {
                throw new MemoException(MemoErrorCode.NOT_MEMO_OWNER);
            }
            LocalDateTime now = LocalDateTime.now(clock);
            memo.updatePosition(command.positionX(), command.positionY(), command.width(), command.height(), now);
            memoRepository.updatePosition(memo.getId(), memo.getPositionX(), memo.getPositionY(), memo.getWidth(),
                    memo.getHeight(), now);
            log.info("event=memo_position_update_완료 memoId={}, userId={}", command.memoId(), command.userId());
        } catch (RuntimeException e) {
            log.warn("event=memo_position_update_실패 memoId={}, userId={}, reason={}", command.memoId(),
                    command.userId(), e.getMessage(), e);
            throw e;
        }
    }
}
