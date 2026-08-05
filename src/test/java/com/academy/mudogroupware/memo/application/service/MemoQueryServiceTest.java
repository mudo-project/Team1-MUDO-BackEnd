package com.academy.mudogroupware.memo.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.memo.application.query.MemoSortOrder;
import com.academy.mudogroupware.memo.domain.model.Memo;
import com.academy.mudogroupware.memo.domain.model.MemoColor;
import com.academy.mudogroupware.memo.domain.repository.MemoRepository;

@ExtendWith(MockitoExtension.class)
class MemoQueryServiceTest {

    @Mock
    private MemoRepository memoRepository;

    @Test
    void getMemosUsesDescendingOrderForNewest() {
        MemoQueryService service = new MemoQueryService(memoRepository);
        Memo memo = Memo.create(10L, "제목", "내용", MemoColor.YELLOW, LocalDateTime.now());
        when(memoRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(10L)).thenReturn(List.of(memo));

        List<Memo> result = service.getMemos(10L, MemoSortOrder.NEWEST);

        assertEquals(1, result.size());
    }

    @Test
    void getMemosUsesAscendingOrderForOldest() {
        MemoQueryService service = new MemoQueryService(memoRepository);
        Memo memo = Memo.create(10L, "제목", "내용", MemoColor.YELLOW, LocalDateTime.now());
        when(memoRepository.findAllByUserIdOrderByCreatedAtAscIdAsc(10L)).thenReturn(List.of(memo));

        List<Memo> result = service.getMemos(10L, MemoSortOrder.OLDEST);

        assertEquals(1, result.size());
    }
}
