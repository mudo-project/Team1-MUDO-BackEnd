package com.academy.mudogroupware.memo.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.memo.application.command.CreateMemoCommand;
import com.academy.mudogroupware.memo.domain.model.Memo;
import com.academy.mudogroupware.memo.domain.model.MemoColor;
import com.academy.mudogroupware.memo.domain.repository.MemoRepository;

@ExtendWith(MockitoExtension.class)
class CreateMemoServiceTest {

    @Mock
    private MemoRepository memoRepository;

    private final Clock clock = Clock.fixed(Instant.parse("2026-08-05T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void createsMemoAndReturnsSavedId() {
        CreateMemoService service = new CreateMemoService(memoRepository, clock);
        when(memoRepository.save(any(Memo.class))).thenAnswer(invocation -> {
            Memo memo = invocation.getArgument(0);
            return Memo.restore(1L, memo.getUserId(), memo.getTitle(), memo.getContent(), memo.getColor(),
                    memo.getPositionX(), memo.getPositionY(), memo.getWidth(), memo.getHeight(),
                    memo.getCreatedAt(), memo.getUpdatedAt());
        });

        Long memoId = service.createMemo(new CreateMemoCommand(10L, "제목", "내용", MemoColor.YELLOW));

        assertEquals(1L, memoId);
    }
}
