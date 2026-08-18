package com.academy.mudogroupware.messenger.presentation.api;

import java.time.LocalDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.messenger.application.query.TaskCardRole;
import com.academy.mudogroupware.messenger.application.usecase.TaskCardQueryUseCase;
import com.academy.mudogroupware.messenger.presentation.api.common.MessengerResponseCode;
import com.academy.mudogroupware.messenger.presentation.api.response.TaskCardPageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@Tag(name = "메신저", description = "채팅방, 메시지, 업무지시 카드 API")
@RestController
@RequestMapping("/api/messenger/task-cards")
@RequiredArgsConstructor
@Validated
public class MessengerTaskCardController {

    private final TaskCardQueryUseCase taskCardQueryUseCase;

    @Operation(summary = "내 업무지시 카드 목록조회",
            description = "role=SENT(내가 전달한 업무)/RECEIVED(내가 받은 업무) 기준으로, 참여 중인 모든 채팅방을 가로질러 "
                    + "cursor(createdAt+cardId) 페이지네이션으로 조회합니다.")
    @GetMapping
    public ResponseEntity<GlobalApiResponse<TaskCardPageResponse>> getMyTaskCards(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam TaskCardRole role,
            @RequestParam(required = false) LocalDateTime cursorCreatedAt,
            @RequestParam(required = false) Long cursorCardId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        TaskCardPageResponse response = TaskCardPageResponse.from(taskCardQueryUseCase.getMyTaskCards(
                authUser.userId(), role, cursorCreatedAt, cursorCardId, size));
        return ResponseEntity.ok(GlobalApiResponse.ok(MessengerResponseCode.MY_TASK_CARD_LIST_RETRIEVED, response));
    }
}
