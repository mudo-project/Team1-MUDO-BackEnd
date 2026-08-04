package com.academy.mudogroupware.messenger.presentation.api;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import com.academy.mudogroupware.messenger.application.usecase.SendMessageUseCase;
import com.academy.mudogroupware.messenger.application.usecase.TaskCardQueryUseCase;
import com.academy.mudogroupware.messenger.presentation.api.common.MessengerResponseCode;
import com.academy.mudogroupware.messenger.presentation.api.request.CreateChatRoomRequest;
import com.academy.mudogroupware.messenger.presentation.api.request.CreateTaskCardRequest;
import com.academy.mudogroupware.messenger.presentation.api.request.SendMessageRequest;
import com.academy.mudogroupware.messenger.presentation.api.response.ChatMessagePageResponse;
import com.academy.mudogroupware.messenger.presentation.api.response.ChatRoomCreateResponse;
import com.academy.mudogroupware.messenger.presentation.api.response.ChatRoomMemberResponse;
import com.academy.mudogroupware.messenger.presentation.api.response.ChatRoomSummaryResponse;
import com.academy.mudogroupware.messenger.presentation.api.response.MessageSendResponse;
import com.academy.mudogroupware.messenger.presentation.api.response.TaskCardCreateResponse;
import com.academy.mudogroupware.messenger.presentation.api.response.TaskCardResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/messenger/rooms")
@RequiredArgsConstructor
public class MessengerController {

    private final CreateChatRoomUseCase createChatRoomUseCase;
    private final ChatRoomQueryUseCase chatRoomQueryUseCase;
    private final ChatRoomMemberQueryUseCase chatRoomMemberQueryUseCase;
    private final SendMessageUseCase sendMessageUseCase;
    private final ChatMessageQueryUseCase chatMessageQueryUseCase;
    private final CreateTaskCardUseCase createTaskCardUseCase;
    private final TaskCardQueryUseCase taskCardQueryUseCase;
    private final CompleteTaskUseCase completeTaskUseCase;

    @PostMapping
    public ResponseEntity<GlobalApiResponse<ChatRoomCreateResponse>> createRoom(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody CreateChatRoomRequest request) {
        Long chatRoomId = createChatRoomUseCase.createRoom(request.toCommand(authUser.userId()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalApiResponse.created(MessengerResponseCode.CHAT_ROOM_CREATED,
                        ChatRoomCreateResponse.from(chatRoomId)));
    }

    @GetMapping
    public ResponseEntity<GlobalApiResponse<List<ChatRoomSummaryResponse>>> getRooms(
            @AuthenticationPrincipal AuthUser authUser) {
        List<ChatRoomSummaryResponse> responses = chatRoomQueryUseCase.getRooms(authUser.userId()).stream()
                .map(ChatRoomSummaryResponse::from)
                .toList();
        return ResponseEntity.ok(GlobalApiResponse.ok(MessengerResponseCode.CHAT_ROOM_LIST_RETRIEVED, responses));
    }

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

    @GetMapping("/{roomId}/messages")
    public ResponseEntity<GlobalApiResponse<ChatMessagePageResponse>> getMessages(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long roomId,
            @RequestParam(required = false) LocalDateTime cursorCreatedAt,
            @RequestParam(required = false) Long cursorMessageId,
            @RequestParam(defaultValue = "20") int size) {
        ChatMessagePageResponse response = ChatMessagePageResponse.from(chatMessageQueryUseCase.getMessages(
                roomId, authUser.userId(), cursorCreatedAt, cursorMessageId, size));
        return ResponseEntity.ok(GlobalApiResponse.ok(MessengerResponseCode.MESSAGE_LIST_RETRIEVED, response));
    }

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

    @GetMapping("/{roomId}/task-cards")
    public ResponseEntity<GlobalApiResponse<List<TaskCardResponse>>> getTaskCards(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long roomId) {
        List<TaskCardResponse> responses = taskCardQueryUseCase.getTaskCards(roomId, authUser.userId()).stream()
                .map(TaskCardResponse::from)
                .toList();
        return ResponseEntity.ok(GlobalApiResponse.ok(MessengerResponseCode.TASK_CARD_LIST_RETRIEVED, responses));
    }

    @PatchMapping("/{roomId}/task-cards/{cardId}/complete")
    public ResponseEntity<Void> completeTaskCard(@AuthenticationPrincipal AuthUser authUser,
                                                  @PathVariable Long roomId,
                                                  @PathVariable Long cardId) {
        completeTaskUseCase.complete(new CompleteTaskCommand(roomId, cardId, authUser.userId()));
        return ResponseEntity.noContent().build();
    }
}
