package com.academy.mudogroupware.memo.presentation.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.memo.application.command.DeleteMemoCommand;
import com.academy.mudogroupware.memo.application.query.MemoSortOrder;
import com.academy.mudogroupware.memo.application.usecase.CreateMemoUseCase;
import com.academy.mudogroupware.memo.application.usecase.DeleteMemoUseCase;
import com.academy.mudogroupware.memo.application.usecase.MemoQueryUseCase;
import com.academy.mudogroupware.memo.application.usecase.UpdateMemoColorUseCase;
import com.academy.mudogroupware.memo.application.usecase.UpdateMemoContentUseCase;
import com.academy.mudogroupware.memo.application.usecase.UpdateMemoPositionUseCase;
import com.academy.mudogroupware.memo.domain.model.Memo;
import com.academy.mudogroupware.memo.domain.model.MemoColor;
import com.academy.mudogroupware.memo.presentation.api.request.CreateMemoRequest;
import com.academy.mudogroupware.memo.presentation.api.request.UpdateMemoColorRequest;
import com.academy.mudogroupware.memo.presentation.api.request.UpdateMemoContentRequest;
import com.academy.mudogroupware.memo.presentation.api.request.UpdateMemoPositionRequest;
import com.academy.mudogroupware.memo.presentation.api.response.MemoCreateResponse;
import com.academy.mudogroupware.memo.presentation.api.response.MemoResponse;

class MemoControllerTest {

    private static final AuthUser AUTH_USER = new AuthUser(10L, "user", 1L, 1L, "MEMBER");

    private final CreateMemoUseCase createMemoUseCase = mock(CreateMemoUseCase.class);
    private final MemoQueryUseCase memoQueryUseCase = mock(MemoQueryUseCase.class);
    private final UpdateMemoContentUseCase updateMemoContentUseCase = mock(UpdateMemoContentUseCase.class);
    private final UpdateMemoColorUseCase updateMemoColorUseCase = mock(UpdateMemoColorUseCase.class);
    private final UpdateMemoPositionUseCase updateMemoPositionUseCase = mock(UpdateMemoPositionUseCase.class);
    private final DeleteMemoUseCase deleteMemoUseCase = mock(DeleteMemoUseCase.class);

    private final MemoController controller = new MemoController(createMemoUseCase, memoQueryUseCase,
            updateMemoContentUseCase, updateMemoColorUseCase, updateMemoPositionUseCase, deleteMemoUseCase);

    @Test
    void createMemoReturnsCreatedWithId() {
        when(createMemoUseCase.createMemo(any())).thenReturn(1L);

        ResponseEntity<GlobalApiResponse<MemoCreateResponse>> response = controller.createMemo(AUTH_USER,
                new CreateMemoRequest("제목", "내용", MemoColor.MUSTARD));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(1L, response.getBody().data().id());
    }

    @Test
    void getMemosReturnsListFromUseCase() {
        Memo memo = Memo.create(10L, "제목", "내용", MemoColor.MUSTARD, LocalDateTime.now());
        when(memoQueryUseCase.getMemos(10L, MemoSortOrder.NEWEST)).thenReturn(List.of(memo));

        ResponseEntity<GlobalApiResponse<List<MemoResponse>>> response = controller.getMemos(AUTH_USER,
                MemoSortOrder.NEWEST);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().data().size());
    }

    @Test
    void updateContentReturnsNoContent() {
        ResponseEntity<Void> response = controller.updateContent(AUTH_USER, 1L,
                new UpdateMemoContentRequest("새 제목", "새 내용"));

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(updateMemoContentUseCase).updateContent(any());
    }

    @Test
    void updateColorReturnsNoContent() {
        ResponseEntity<Void> response = controller.updateColor(AUTH_USER, 1L,
                new UpdateMemoColorRequest(MemoColor.BLUE));

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(updateMemoColorUseCase).updateColor(any());
    }

    @Test
    void updatePositionReturnsNoContent() {
        ResponseEntity<Void> response = controller.updatePosition(AUTH_USER, 1L,
                new UpdateMemoPositionRequest(10, 20, 200, 150));

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(updateMemoPositionUseCase).updatePosition(any());
    }

    @Test
    void deleteMemoReturnsNoContent() {
        ResponseEntity<Void> response = controller.deleteMemo(AUTH_USER, 1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(deleteMemoUseCase).deleteMemo(new DeleteMemoCommand(1L, 10L));
    }
}
