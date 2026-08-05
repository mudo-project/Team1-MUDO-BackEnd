package com.academy.mudogroupware.memo.presentation.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
import com.academy.mudogroupware.memo.presentation.api.common.MemoResponseCode;
import com.academy.mudogroupware.memo.presentation.api.request.CreateMemoRequest;
import com.academy.mudogroupware.memo.presentation.api.request.UpdateMemoColorRequest;
import com.academy.mudogroupware.memo.presentation.api.request.UpdateMemoContentRequest;
import com.academy.mudogroupware.memo.presentation.api.request.UpdateMemoPositionRequest;
import com.academy.mudogroupware.memo.presentation.api.response.MemoCreateResponse;
import com.academy.mudogroupware.memo.presentation.api.response.MemoResponse;

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
    private final MemoQueryUseCase memoQueryUseCase;
    private final UpdateMemoContentUseCase updateMemoContentUseCase;
    private final UpdateMemoColorUseCase updateMemoColorUseCase;
    private final UpdateMemoPositionUseCase updateMemoPositionUseCase;
    private final DeleteMemoUseCase deleteMemoUseCase;

    @Operation(summary = "메모 생성", description = "제목/내용/색상으로 메모를 생성한다. 위치·크기는 생성 시점엔 비어있고 자유배치 시 별도로 설정한다.")
    @PostMapping
    public ResponseEntity<GlobalApiResponse<MemoCreateResponse>> createMemo(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody CreateMemoRequest request) {
        Long memoId = createMemoUseCase.createMemo(request.toCommand(authUser.userId()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalApiResponse.created(MemoResponseCode.MEMO_CREATED, MemoCreateResponse.from(memoId)));
    }

    @Operation(summary = "메모 목록조회", description = "내 메모 전체를 정렬 기준(sort)에 따라 조회한다.")
    @GetMapping
    public ResponseEntity<GlobalApiResponse<List<MemoResponse>>> getMemos(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "NEWEST") MemoSortOrder sort) {
        List<MemoResponse> responses = memoQueryUseCase.getMemos(authUser.userId(), sort).stream()
                .map(MemoResponse::from)
                .toList();
        return ResponseEntity.ok(GlobalApiResponse.ok(MemoResponseCode.MEMO_LIST_RETRIEVED, responses));
    }

    @Operation(summary = "메모 내용 수정", description = "본인 메모만 제목/내용을 수정할 수 있다.")
    @PatchMapping("/{memoId}")
    public ResponseEntity<Void> updateContent(@AuthenticationPrincipal AuthUser authUser,
                                               @PathVariable Long memoId,
                                               @Valid @RequestBody UpdateMemoContentRequest request) {
        updateMemoContentUseCase.updateContent(request.toCommand(memoId, authUser.userId()));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "메모 색상 변경", description = "본인 메모만 색상을 변경할 수 있다.")
    @PatchMapping("/{memoId}/color")
    public ResponseEntity<Void> updateColor(@AuthenticationPrincipal AuthUser authUser,
                                             @PathVariable Long memoId,
                                             @Valid @RequestBody UpdateMemoColorRequest request) {
        updateMemoColorUseCase.updateColor(request.toCommand(memoId, authUser.userId()));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "메모 위치·크기 변경", description = "자유배치 모드에서 드래그·리사이즈한 결과를 저장한다. 본인 메모만 가능하다.")
    @PatchMapping("/{memoId}/position")
    public ResponseEntity<Void> updatePosition(@AuthenticationPrincipal AuthUser authUser,
                                                @PathVariable Long memoId,
                                                @Valid @RequestBody UpdateMemoPositionRequest request) {
        updateMemoPositionUseCase.updatePosition(request.toCommand(memoId, authUser.userId()));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "메모 삭제", description = "본인 메모만 삭제할 수 있다.")
    @DeleteMapping("/{memoId}")
    public ResponseEntity<Void> deleteMemo(@AuthenticationPrincipal AuthUser authUser,
                                            @PathVariable Long memoId) {
        deleteMemoUseCase.deleteMemo(new DeleteMemoCommand(memoId, authUser.userId()));
        return ResponseEntity.noContent().build();
    }
}
