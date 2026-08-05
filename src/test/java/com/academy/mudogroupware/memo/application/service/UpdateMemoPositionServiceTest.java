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

import com.academy.mudogroupware.memo.application.command.UpdateMemoPositionCommand;
import com.academy.mudogroupware.memo.domain.exception.MemoException;
import com.academy.mudogroupware.memo.domain.model.Memo;
import com.academy.mudogroupware.memo.domain.model.MemoColor;
import com.academy.mudogroupware.memo.domain.repository.MemoRepository;

@ExtendWith(MockitoExtension.class)
class UpdateMemoPositionServiceTest {

    @Mock
    private MemoRepository memoRepository;

    private final Clock clock = Clock.fixed(Instant.parse("2026-08-05T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void updatesPositionAndSizeWhenOwner() {
        UpdateMemoPositionService service = new UpdateMemoPositionService(memoRepository, clock);
        Memo memo = Memo.create(10L, "제목", "내용", MemoColor.YELLOW, LocalDateTime.now());
        when(memoRepository.findById(1L)).thenReturn(Optional.of(memo));

        service.updatePosition(new UpdateMemoPositionCommand(1L, 10L, 10, 20, 200, 150));

        assertEquals(10, memo.getPositionX());
        assertEquals(20, memo.getPositionY());
        assertEquals(200, memo.getWidth());
        assertEquals(150, memo.getHeight());
    }

    @Test
    void rejectsUpdateWhenNotOwner() {
        UpdateMemoPositionService service = new UpdateMemoPositionService(memoRepository, clock);
        Memo memo = Memo.create(10L, "제목", "내용", MemoColor.YELLOW, LocalDateTime.now());
        when(memoRepository.findById(1L)).thenReturn(Optional.of(memo));

        assertThrows(MemoException.class,
                () -> service.updatePosition(new UpdateMemoPositionCommand(1L, 99L, 10, 20, 200, 150)));
    }

    @Test
    void rejectsUpdateWhenMemoNotFound() {
        UpdateMemoPositionService service = new UpdateMemoPositionService(memoRepository, clock);
        when(memoRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(MemoException.class,
                () -> service.updatePosition(new UpdateMemoPositionCommand(1L, 10L, 10, 20, 200, 150)));
    }
}
