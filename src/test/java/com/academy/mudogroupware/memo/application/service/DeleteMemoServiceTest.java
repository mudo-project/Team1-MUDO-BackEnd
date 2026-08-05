package com.academy.mudogroupware.memo.application.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.memo.application.command.DeleteMemoCommand;
import com.academy.mudogroupware.memo.domain.exception.MemoException;
import com.academy.mudogroupware.memo.domain.model.Memo;
import com.academy.mudogroupware.memo.domain.model.MemoColor;
import com.academy.mudogroupware.memo.domain.repository.MemoRepository;

@ExtendWith(MockitoExtension.class)
class DeleteMemoServiceTest {

    @Mock
    private MemoRepository memoRepository;

    @Test
    void deletesMemoWhenOwner() {
        DeleteMemoService service = new DeleteMemoService(memoRepository);
        Memo memo = Memo.create(10L, "제목", "내용", MemoColor.YELLOW, LocalDateTime.now());
        when(memoRepository.findById(1L)).thenReturn(Optional.of(memo));

        service.deleteMemo(new DeleteMemoCommand(1L, 10L));

        verify(memoRepository, times(1)).deleteById(1L);
    }

    @Test
    void rejectsDeleteWhenNotOwner() {
        DeleteMemoService service = new DeleteMemoService(memoRepository);
        Memo memo = Memo.create(10L, "제목", "내용", MemoColor.YELLOW, LocalDateTime.now());
        when(memoRepository.findById(1L)).thenReturn(Optional.of(memo));

        assertThrows(MemoException.class, () -> service.deleteMemo(new DeleteMemoCommand(1L, 99L)));
    }

    @Test
    void rejectsDeleteWhenMemoNotFound() {
        DeleteMemoService service = new DeleteMemoService(memoRepository);
        when(memoRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(MemoException.class, () -> service.deleteMemo(new DeleteMemoCommand(1L, 10L)));
    }
}
