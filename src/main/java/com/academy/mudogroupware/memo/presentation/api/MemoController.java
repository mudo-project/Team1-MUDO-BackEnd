package com.academy.mudogroupware.memo.presentation.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.memo.application.usecase.CreateMemoUseCase;
import com.academy.mudogroupware.memo.presentation.api.common.MemoResponseCode;
import com.academy.mudogroupware.memo.presentation.api.request.CreateMemoRequest;
import com.academy.mudogroupware.memo.presentation.api.response.MemoCreateResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "개인메모", description = "개인메모 생성/목록조회/수정/삭제 API")
@RestController
@RequestMapping("/api/memos")
@RequiredArgsConstructor
@Validated
public class MemoController {

    private final CreateMemoUseCase createMemoUseCase;

    @Operation(summary = "메모 생성", description = "제목/내용/색상으로 메모를 생성한다. 위치·크기는 생성 시점엔 비어있고 자유배치 시 별도로 설정한다.")
    @PostMapping
    public ResponseEntity<GlobalApiResponse<MemoCreateResponse>> createMemo(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody CreateMemoRequest request) {
        Long memoId = createMemoUseCase.createMemo(request.toCommand(authUser.userId()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalApiResponse.created(MemoResponseCode.MEMO_CREATED, MemoCreateResponse.from(memoId)));
    }
}
