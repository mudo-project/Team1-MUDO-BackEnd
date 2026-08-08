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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UpdateMemoColorService implements UpdateMemoColorUseCase {

    private final MemoRepository memoRepository;
    private final Clock clock;

    @Override
    public void updateColor(UpdateMemoColorCommand command) {
        log.info("event=memo_color_update_시작 memoId={}, userId={}, color={}", command.memoId(), command.userId(),
                command.color());
        try {
            Memo memo = memoRepository.findById(command.memoId())
                    .orElseThrow(() -> new MemoException(MemoErrorCode.MEMO_NOT_FOUND));
            if (!memo.isOwnedBy(command.userId())) {
                throw new MemoException(MemoErrorCode.NOT_MEMO_OWNER);
            }
            LocalDateTime now = LocalDateTime.now(clock);
            memo.updateColor(command.color(), now);
            memoRepository.updateColor(memo.getId(), memo.getColor(), now);
            log.info("event=memo_color_update_완료 memoId={}, userId={}, color={}", command.memoId(),
                    command.userId(), memo.getColor());
        } catch (RuntimeException e) {
            log.warn("event=memo_color_update_실패 memoId={}, userId={}, reason={}", command.memoId(),
                    command.userId(), e.getMessage());
            throw e;
        }
    }
}
