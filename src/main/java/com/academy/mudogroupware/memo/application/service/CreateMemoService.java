package com.academy.mudogroupware.memo.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

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

    // 저장 버튼 더블클릭 등으로 같은 내용의 생성 요청이 짧은 간격으로 두 번 오면, 같은 사용자의
    // 가장 최근 메모와 제목·내용·색상이 모두 같고 이 시간 안에 만들어졌을 때 새로 만들지 않고
    // 기존 메모 id를 그대로 반환한다.
    private static final Duration DUPLICATE_SUBMIT_WINDOW = Duration.ofSeconds(3);

    private final MemoRepository memoRepository;
    private final Clock clock;

    @Override
    public Long createMemo(CreateMemoCommand command) {
        log.info("event=memo_create_시작 userId={}", command.userId());
        LocalDateTime now = LocalDateTime.now(clock);

        Optional<Memo> duplicate = memoRepository.findMostRecentByUserId(command.userId())
                .filter(recent -> isDuplicateSubmit(recent, command, now));
        if (duplicate.isPresent()) {
            Long memoId = duplicate.get().getId();
            log.info("event=memo_create_중복요청_무시 userId={}, memoId={}", command.userId(), memoId);
            return memoId;
        }

        if (memoRepository.countByUserId(command.userId()) >= MAX_MEMO_COUNT_PER_USER) {
            throw new MemoException(MemoErrorCode.MEMO_LIMIT_EXCEEDED);
        }
        Memo memo = Memo.create(command.userId(), command.title(), command.content(), command.color(), now);
        Long memoId = memoRepository.save(memo).getId();
        log.info("event=memo_create_완료 userId={}, memoId={}", command.userId(), memoId);
        return memoId;
    }

    private boolean isDuplicateSubmit(Memo recent, CreateMemoCommand command, LocalDateTime now) {
        return Objects.equals(recent.getTitle(), command.title())
                && Objects.equals(recent.getContent(), command.content())
                && Objects.equals(recent.getColor(), command.color())
                && recent.getCreatedAt().isAfter(now.minus(DUPLICATE_SUBMIT_WINDOW));
    }
}
