package com.academy.mudogroupware.memo.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.memo.application.command.CreateMemoCommand;
import com.academy.mudogroupware.memo.application.usecase.CreateMemoUseCase;
import com.academy.mudogroupware.memo.domain.model.Memo;
import com.academy.mudogroupware.memo.domain.repository.MemoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateMemoService implements CreateMemoUseCase {

    private final MemoRepository memoRepository;
    private final Clock clock;

    @Override
    public Long createMemo(CreateMemoCommand command) {
        Memo memo = Memo.create(command.userId(), command.title(), command.content(), command.color(),
                LocalDateTime.now(clock));
        return memoRepository.save(memo).getId();
    }
}
