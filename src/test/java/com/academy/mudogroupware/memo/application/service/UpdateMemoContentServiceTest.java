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

import com.academy.mudogroupware.memo.application.command.UpdateMemoContentCommand;
import com.academy.mudogroupware.memo.domain.exception.MemoException;
import com.academy.mudogroupware.memo.domain.model.Memo;
import com.academy.mudogroupware.memo.domain.model.MemoColor;
import com.academy.mudogroupware.memo.domain.repository.MemoRepository;

@ExtendWith(MockitoExtension.class)
class UpdateMemoContentServiceTest {

    @Mock
    private MemoRepository memoRepository;

    private final Clock clock = Clock.fixed(Instant.parse("2026-08-05T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void updatesTitleAndContentWhenOwner() {
        UpdateMemoContentService service = new UpdateMemoContentService(memoRepository, clock);
        Memo memo = Memo.create(10L, "제목", "내용", MemoColor.YELLOW, LocalDateTime.now());
        when(memoRepository.findById(1L)).thenReturn(Optional.of(memo));

        service.updateContent(new UpdateMemoContentCommand(1L, 10L, "새 제목", "새 내용"));

        assertEquals("새 제목", memo.getTitle());
        assertEquals("새 내용", memo.getContent());
    }

    @Test
    void rejectsUpdateWhenNotOwner() {
        UpdateMemoContentService service = new UpdateMemoContentService(memoRepository, clock);
        Memo memo = Memo.create(10L, "제목", "내용", MemoColor.YELLOW, LocalDateTime.now());
        when(memoRepository.findById(1L)).thenReturn(Optional.of(memo));

        assertThrows(MemoException.class,
                () -> service.updateContent(new UpdateMemoContentCommand(1L, 99L, "새 제목", "새 내용")));
    }

    @Test
    void rejectsUpdateWhenMemoNotFound() {
        UpdateMemoContentService service = new UpdateMemoContentService(memoRepository, clock);
        when(memoRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(MemoException.class,
                () -> service.updateContent(new UpdateMemoContentCommand(1L, 10L, "새 제목", "새 내용")));
    }
}
