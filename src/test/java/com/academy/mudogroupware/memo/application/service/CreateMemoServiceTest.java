package com.academy.mudogroupware.memo.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.memo.application.command.CreateMemoCommand;
import com.academy.mudogroupware.memo.domain.exception.MemoErrorCode;
import com.academy.mudogroupware.memo.domain.exception.MemoException;
import com.academy.mudogroupware.memo.domain.model.Memo;
import com.academy.mudogroupware.memo.domain.repository.MemoRepository;

@ExtendWith(MockitoExtension.class)
class CreateMemoServiceTest {

    @Mock
    private MemoRepository memoRepository;

    private final Clock clock = Clock.fixed(Instant.parse("2026-08-05T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void createsMemoAndReturnsSavedId() {
        CreateMemoService service = new CreateMemoService(memoRepository, clock);
        when(memoRepository.countByUserId(10L)).thenReturn(0L);
        when(memoRepository.save(any(Memo.class))).thenAnswer(invocation -> {
            Memo memo = invocation.getArgument(0);
            return Memo.restore(1L, memo.getUserId(), memo.getTitle(), memo.getContent(), memo.getColor(),
                    memo.getPositionX(), memo.getPositionY(), memo.getWidth(), memo.getHeight(),
                    memo.getCreatedAt(), memo.getUpdatedAt());
        });

        Long memoId = service.createMemo(new CreateMemoCommand(10L, "제목", "내용", "D3A340"));

        assertEquals(1L, memoId);
    }

    @Test
    void allowsCreationWhenUserHas199Memos() {
        CreateMemoService service = new CreateMemoService(memoRepository, clock);
        when(memoRepository.countByUserId(10L)).thenReturn(199L);
        when(memoRepository.save(any(Memo.class))).thenAnswer(invocation -> {
            Memo memo = invocation.getArgument(0);
            return Memo.restore(200L, memo.getUserId(), memo.getTitle(), memo.getContent(), memo.getColor(),
                    memo.getPositionX(), memo.getPositionY(), memo.getWidth(), memo.getHeight(),
                    memo.getCreatedAt(), memo.getUpdatedAt());
        });

        Long memoId = service.createMemo(new CreateMemoCommand(10L, "제목", "내용", "D3A340"));

        assertEquals(200L, memoId);
    }

    @Test
    void throwsWhenUserAlreadyHas200Memos() {
        CreateMemoService service = new CreateMemoService(memoRepository, clock);
        when(memoRepository.countByUserId(10L)).thenReturn(200L);

        MemoException exception = assertThrows(MemoException.class,
                () -> service.createMemo(new CreateMemoCommand(10L, "제목", "내용", "D3A340")));

        assertEquals(MemoErrorCode.MEMO_LIMIT_EXCEEDED, exception.getErrorCode());
        verify(memoRepository, never()).save(any(Memo.class));
    }

    @Test
    void returnsExistingMemoIdWhenSameContentSubmittedWithinWindow() {
        CreateMemoService service = new CreateMemoService(memoRepository, clock);
        Memo recent = Memo.restore(1L, 10L, "제목", "내용", "D3A340", null, null, null, null,
                clock.instant().minusSeconds(1).atZone(ZoneOffset.UTC).toLocalDateTime(),
                clock.instant().minusSeconds(1).atZone(ZoneOffset.UTC).toLocalDateTime());
        when(memoRepository.findMostRecentByUserId(10L)).thenReturn(Optional.of(recent));

        Long memoId = service.createMemo(new CreateMemoCommand(10L, "제목", "내용", "D3A340"));

        assertEquals(1L, memoId);
        verify(memoRepository, never()).countByUserId(10L);
        verify(memoRepository, never()).save(any(Memo.class));
    }

    @Test
    void createsNewMemoWhenRecentMemoHasDifferentContent() {
        CreateMemoService service = new CreateMemoService(memoRepository, clock);
        Memo recent = Memo.restore(1L, 10L, "다른 제목", "내용", "D3A340", null, null, null, null,
                clock.instant().minusSeconds(1).atZone(ZoneOffset.UTC).toLocalDateTime(),
                clock.instant().minusSeconds(1).atZone(ZoneOffset.UTC).toLocalDateTime());
        when(memoRepository.findMostRecentByUserId(10L)).thenReturn(Optional.of(recent));
        when(memoRepository.countByUserId(10L)).thenReturn(0L);
        when(memoRepository.save(any(Memo.class))).thenAnswer(invocation -> {
            Memo memo = invocation.getArgument(0);
            return Memo.restore(2L, memo.getUserId(), memo.getTitle(), memo.getContent(), memo.getColor(),
                    memo.getPositionX(), memo.getPositionY(), memo.getWidth(), memo.getHeight(),
                    memo.getCreatedAt(), memo.getUpdatedAt());
        });

        Long memoId = service.createMemo(new CreateMemoCommand(10L, "제목", "내용", "D3A340"));

        assertEquals(2L, memoId);
    }

    @Test
    void createsNewMemoWhenSameContentIsOutsideDuplicateWindow() {
        CreateMemoService service = new CreateMemoService(memoRepository, clock);
        Memo recent = Memo.restore(1L, 10L, "제목", "내용", "D3A340", null, null, null, null,
                clock.instant().minusSeconds(10).atZone(ZoneOffset.UTC).toLocalDateTime(),
                clock.instant().minusSeconds(10).atZone(ZoneOffset.UTC).toLocalDateTime());
        when(memoRepository.findMostRecentByUserId(10L)).thenReturn(Optional.of(recent));
        when(memoRepository.countByUserId(10L)).thenReturn(0L);
        when(memoRepository.save(any(Memo.class))).thenAnswer(invocation -> {
            Memo memo = invocation.getArgument(0);
            return Memo.restore(2L, memo.getUserId(), memo.getTitle(), memo.getContent(), memo.getColor(),
                    memo.getPositionX(), memo.getPositionY(), memo.getWidth(), memo.getHeight(),
                    memo.getCreatedAt(), memo.getUpdatedAt());
        });

        Long memoId = service.createMemo(new CreateMemoCommand(10L, "제목", "내용", "D3A340"));

        assertEquals(2L, memoId);
    }
}
