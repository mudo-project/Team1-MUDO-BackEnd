package com.academy.mudogroupware.memo.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.memo.application.command.CreateMemoCommand;
import com.academy.mudogroupware.memo.application.usecase.CreateMemoUseCase;
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
public class CreateMemoService implements CreateMemoUseCase {

    // GET /api/memos가 페이지네이션 없이 사용자의 메모 전체를 반환하므로, 응답 크기가 무한히 커지지 않도록
    // 생성 시점에 상한을 둔다(부하테스트로 5,000건 기준 응답 2.2MB/건당 조회 확인, 2026-08-07).
    private static final long MAX_MEMO_COUNT_PER_USER = 200;

    private final MemoRepository memoRepository;
    private final Clock clock;

    @Override
    public Long createMemo(CreateMemoCommand command) {
        log.info("event=memo_create_시작 userId={}", command.userId());
        try {
            if (memoRepository.countByUserId(command.userId()) >= MAX_MEMO_COUNT_PER_USER) {
                throw new MemoException(MemoErrorCode.MEMO_LIMIT_EXCEEDED);
            }
            Memo memo = Memo.create(command.userId(), command.title(), command.content(), command.color(),
                    LocalDateTime.now(clock));
            Long memoId = memoRepository.save(memo).getId();
            log.info("event=memo_create_완료 userId={}, memoId={}", command.userId(), memoId);
            return memoId;
        } catch (RuntimeException e) {
            log.warn("event=memo_create_실패 userId={}, reason={}", command.userId(), e.getMessage(), e);
            throw e;
        }
    }
}
