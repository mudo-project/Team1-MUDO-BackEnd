package com.academy.mudogroupware.messenger.presentation.api;

import java.time.LocalDateTime;
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
import com.academy.mudogroupware.messenger.application.command.CompleteTaskCommand;
import com.academy.mudogroupware.messenger.application.usecase.ChatMessageQueryUseCase;
import com.academy.mudogroupware.messenger.application.usecase.ChatRoomMemberQueryUseCase;
import com.academy.mudogroupware.messenger.application.usecase.ChatRoomQueryUseCase;
import com.academy.mudogroupware.messenger.application.usecase.CompleteTaskUseCase;
import com.academy.mudogroupware.messenger.application.usecase.CreateChatRoomUseCase;
import com.academy.mudogroupware.messenger.application.usecase.CreateTaskCardUseCase;
import com.academy.mudogroupware.messenger.application.usecase.DeleteMessageUseCase;
import com.academy.mudogroupware.messenger.application.usecase.SendMessageUseCase;
import com.academy.mudogroupware.messenger.application.usecase.TaskCardQueryUseCase;
import com.academy.mudogroupware.messenger.application.usecase.UpdateMessageUseCase;
import com.academy.mudogroupware.messenger.presentation.api.common.MessengerResponseCode;
import com.academy.mudogroupware.messenger.presentation.api.request.CreateChatRoomRequest;
import com.academy.mudogroupware.messenger.presentation.api.request.CreateTaskCardRequest;
import com.academy.mudogroupware.messenger.presentation.api.request.SendMessageRequest;
import com.academy.mudogroupware.messenger.presentation.api.request.UpdateMessageRequest;
import com.academy.mudogroupware.messenger.presentation.api.response.ChatMessagePageResponse;
import com.academy.mudogroupware.messenger.presentation.api.response.ChatRoomCreateResponse;
import com.academy.mudogroupware.messenger.presentation.api.response.ChatRoomMemberResponse;
import com.academy.mudogroupware.messenger.presentation.api.response.ChatRoomSummaryResponse;
import com.academy.mudogroupware.messenger.presentation.api.response.MessageSendResponse;
import com.academy.mudogroupware.messenger.presentation.api.response.TaskCardCreateResponse;
import com.academy.mudogroupware.messenger.presentation.api.response.TaskCardResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@Tag(name = "메신저", description = "채팅방, 메시지, 업무지시 카드 API")
@RestController
@RequestMapping("/api/messenger/rooms")
@RequiredArgsConstructor
@Validated
public class MessengerController {

    private final CreateChatRoomUseCase createChatRoomUseCase;
    private final ChatRoomQueryUseCase chatRoomQueryUseCase;
    private final ChatRoomMemberQueryUseCase chatRoomMemberQueryUseCase;
    private final SendMessageUseCase sendMessageUseCase;
    private final UpdateMessageUseCase updateMessageUseCase;
    private final DeleteMessageUseCase deleteMessageUseCase;
    private final ChatMessageQueryUseCase chatMessageQueryUseCase;
    private final CreateTaskCardUseCase createTaskCardUseCase;
    private final TaskCardQueryUseCase taskCardQueryUseCase;
    private final CompleteTaskUseCase completeTaskUseCase;

    @Operation(summary = "채팅방 생성", description = "참여자 1명이면 1:1(DM), 2명 이상이면 그룹 생성. 그룹일 때만 name 필수.")
    @PostMapping
    public ResponseEntity<GlobalApiResponse<ChatRoomCreateResponse>> createRoom(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody CreateChatRoomRequest request) {
        Long chatRoomId = createChatRoomUseCase.createRoom(request.toCommand(authUser.userId()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalApiResponse.created(MessengerResponseCode.CHAT_ROOM_CREATED,
                        ChatRoomCreateResponse.from(chatRoomId)));
    }

    @Operation(summary = "채팅방 목록조회", description = "내가 참여 중인 채팅방을 최근 활동순으로 조회합니다.")
    @GetMapping
    public ResponseEntity<GlobalApiResponse<List<ChatRoomSummaryResponse>>> getRooms(
            @AuthenticationPrincipal AuthUser authUser) {
        List<ChatRoomSummaryResponse> responses = chatRoomQueryUseCase.getRooms(authUser.userId()).stream()
                .map(ChatRoomSummaryResponse::from)
                .toList();
        return ResponseEntity.ok(GlobalApiResponse.ok(MessengerResponseCode.CHAT_ROOM_LIST_RETRIEVED, responses));
    }

    @Operation(summary = "채팅방 참여자 목록조회", description = "요청자가 참여 중인 방의 참여자 목록을 조회합니다.")
    @GetMapping("/{roomId}/members")
    public ResponseEntity<GlobalApiResponse<List<ChatRoomMemberResponse>>> getMembers(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long roomId) {
        List<ChatRoomMemberResponse> responses = chatRoomMemberQueryUseCase.getMembers(roomId, authUser.userId())
                .stream()
                .map(ChatRoomMemberResponse::from)
                .toList();
        return ResponseEntity.ok(GlobalApiResponse.ok(MessengerResponseCode.CHAT_ROOM_MEMBERS_RETRIEVED, responses));
    }

    @Operation(summary = "메시지 전송", description = "TEXT/IMAGE/FILE 메시지를 전송합니다.")
    @PostMapping("/{roomId}/messages")
    public ResponseEntity<GlobalApiResponse<MessageSendResponse>> sendMessage(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long roomId,
            @Valid @RequestBody SendMessageRequest request) {
        Long messageId = sendMessageUseCase.sendMessage(request.toCommand(roomId, authUser.userId()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalApiResponse.created(MessengerResponseCode.MESSAGE_SENT,
                        MessageSendResponse.from(messageId)));
    }

    @Operation(summary = "메시지 수정", description = "본인이 보낸 TEXT 메시지만 수정합니다.")
    @PatchMapping("/{roomId}/messages/{messageId}")
    public ResponseEntity<Void> updateMessage(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long roomId,
            @PathVariable Long messageId,
            @Valid @RequestBody UpdateMessageRequest request) {
        updateMessageUseCase.update(request.toCommand(roomId, messageId, authUser.userId()));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "메시지 삭제", description = "본인이 보낸 메시지를 소프트 삭제합니다.")
    @DeleteMapping("/{roomId}/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long roomId,
            @PathVariable Long messageId) {
        deleteMessageUseCase.delete(roomId, messageId, authUser.userId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "메시지 목록조회", description = "cursor(createdAt+messageId) 기반 페이지네이션으로 조회합니다.")
    @GetMapping("/{roomId}/messages")
    public ResponseEntity<GlobalApiResponse<ChatMessagePageResponse>> getMessages(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long roomId,
            @RequestParam(required = false) LocalDateTime cursorCreatedAt,
            @RequestParam(required = false) Long cursorMessageId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        ChatMessagePageResponse response = ChatMessagePageResponse.from(chatMessageQueryUseCase.getMessages(
                roomId, authUser.userId(), cursorCreatedAt, cursorMessageId, size));
        return ResponseEntity.ok(GlobalApiResponse.ok(MessengerResponseCode.MESSAGE_LIST_RETRIEVED, response));
    }

    @Operation(summary = "업무지시 카드 등록", description = "담당자는 반드시 해당 채팅방 멤버여야 합니다.")
    @PostMapping("/{roomId}/task-cards")
    public ResponseEntity<GlobalApiResponse<TaskCardCreateResponse>> createTaskCard(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long roomId,
            @Valid @RequestBody CreateTaskCardRequest request) {
        Long cardId = createTaskCardUseCase.createTaskCard(request.toCommand(roomId, authUser.userId()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalApiResponse.created(MessengerResponseCode.TASK_CARD_CREATED,
                        TaskCardCreateResponse.from(cardId)));
    }

    @Operation(summary = "업무지시 카드 목록조회", description = "완료 인원과 전체 담당자 수를 포함해 조회합니다.")
    @GetMapping("/{roomId}/task-cards")
    public ResponseEntity<GlobalApiResponse<List<TaskCardResponse>>> getTaskCards(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long roomId) {
        List<TaskCardResponse> responses = taskCardQueryUseCase.getTaskCards(roomId, authUser.userId()).stream()
                .map(TaskCardResponse::from)
                .toList();
        return ResponseEntity.ok(GlobalApiResponse.ok(MessengerResponseCode.TASK_CARD_LIST_RETRIEVED, responses));
    }

    @Operation(summary = "업무지시 완료 처리", description = "담당자 본인만 완료 처리할 수 있습니다.")
    @PatchMapping("/{roomId}/task-cards/{cardId}/complete")
    public ResponseEntity<Void> completeTaskCard(@AuthenticationPrincipal AuthUser authUser,
                                                  @PathVariable Long roomId,
                                                  @PathVariable Long cardId) {
        completeTaskUseCase.complete(new CompleteTaskCommand(roomId, cardId, authUser.userId()));
        return ResponseEntity.noContent().build();
    }
}
