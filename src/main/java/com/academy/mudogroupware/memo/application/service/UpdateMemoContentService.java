package com.academy.mudogroupware.memo.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.memo.application.command.UpdateMemoContentCommand;
import com.academy.mudogroupware.memo.application.usecase.UpdateMemoContentUseCase;
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
public class UpdateMemoContentService implements UpdateMemoContentUseCase {

    private final MemoRepository memoRepository;
    private final Clock clock;

    @Override
    public void updateContent(UpdateMemoContentCommand command) {
        log.info("event=memo_content_update_시작 memoId={}, userId={}", command.memoId(), command.userId());
        Memo memo = memoRepository.findById(command.memoId())
                .orElseThrow(() -> new MemoException(MemoErrorCode.MEMO_NOT_FOUND));
        if (!memo.isOwnedBy(command.userId())) {
            throw new MemoException(MemoErrorCode.NOT_MEMO_OWNER);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        memo.updateContent(command.title(), command.content(), now);
        memoRepository.updateContent(memo.getId(), memo.getTitle(), memo.getContent(), now);
        log.info("event=memo_content_update_완료 memoId={}, userId={}", command.memoId(), command.userId());
    }
}
