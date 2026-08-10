package com.academy.mudogroupware.memo.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.memo.application.command.UpdateMemoColorCommand;
import com.academy.mudogroupware.memo.domain.exception.MemoErrorCode;
import com.academy.mudogroupware.memo.domain.exception.MemoException;
import com.academy.mudogroupware.memo.domain.model.Memo;
import com.academy.mudogroupware.memo.domain.model.MemoColor;
import com.academy.mudogroupware.memo.domain.repository.MemoRepository;

@ExtendWith(MockitoExtension.class)
class UpdateMemoColorServiceTest {

    @Mock
    private MemoRepository memoRepository;

    private final Clock clock = Clock.fixed(Instant.parse("2026-08-05T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void updatesColorWhenOwner() {
        UpdateMemoColorService service = new UpdateMemoColorService(memoRepository, clock);
        Memo memo = Memo.create(10L, "제목", "내용", MemoColor.MUSTARD, LocalDateTime.now());
        when(memoRepository.findById(1L)).thenReturn(Optional.of(memo));

        service.updateColor(new UpdateMemoColorCommand(1L, 10L, MemoColor.BLUE));

        assertEquals(MemoColor.BLUE, memo.getColor());
    }

    @Test
    void rejectsUpdateWhenNotOwner() {
        UpdateMemoColorService service = new UpdateMemoColorService(memoRepository, clock);
        Memo memo = Memo.create(10L, "제목", "내용", MemoColor.MUSTARD, LocalDateTime.now());
        when(memoRepository.findById(1L)).thenReturn(Optional.of(memo));

        MemoException exception = assertThrows(MemoException.class,
                () -> service.updateColor(new UpdateMemoColorCommand(1L, 99L, MemoColor.BLUE)));
        assertEquals(MemoErrorCode.NOT_MEMO_OWNER, exception.getErrorCode());
    }

    @Test
    void rejectsUpdateWhenMemoNotFound() {
        UpdateMemoColorService service = new UpdateMemoColorService(memoRepository, clock);
        when(memoRepository.findById(1L)).thenReturn(Optional.empty());

        MemoException exception = assertThrows(MemoException.class,
                () -> service.updateColor(new UpdateMemoColorCommand(1L, 10L, MemoColor.BLUE)));
        assertEquals(MemoErrorCode.MEMO_NOT_FOUND, exception.getErrorCode());
    }
}
