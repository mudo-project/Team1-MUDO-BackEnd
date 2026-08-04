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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "메신저", description = "채팅방 생성/목록조회/참여자조회, 메시지 전송/목록조회, 업무지시 카드 API")
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

    @Operation(summary = "채팅방 목록조회", description = "내가 참여 중인 채팅방을 최근 활동순으로 조회. 안읽은 메시지 수·최근 메시지 미리보기 포함.")
    @GetMapping
    public ResponseEntity<GlobalApiResponse<List<ChatRoomSummaryResponse>>> getRooms(
            @AuthenticationPrincipal AuthUser authUser) {
        List<ChatRoomSummaryResponse> responses = chatRoomQueryUseCase.getRooms(authUser.userId()).stream()
                .map(ChatRoomSummaryResponse::from)
                .toList();
        return ResponseEntity.ok(GlobalApiResponse.ok(MessengerResponseCode.CHAT_ROOM_LIST_RETRIEVED, responses));
    }

    @Operation(summary = "채팅방 참여자 목록조회", description = "요청자가 참여 중인 방에 한해 참여자 목록을 조회.")
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

    @Operation(summary = "메시지 전송", description = "TEXT/IMAGE/FILE 메시지 전송. 이미지·파일은 messageType+fileUrl/fileName으로 전달(별도 업로드 API 없음).")
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

    @Operation(summary = "메시지 목록조회", description = "cursor(createdAt+messageId) 기반 페이지네이션. cursor가 없는 첫 조회일 때만 읽음 처리.")
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

    @Operation(summary = "업무지시 카드 등록", description = "담당자는 반드시 해당 채팅방 멤버여야 한다.")
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

    @Operation(summary = "업무지시 카드 목록조회", description = "완료 인원/전체 담당자 수, 전원완료 여부를 포함해 조회.")
    @GetMapping("/{roomId}/task-cards")
    public ResponseEntity<GlobalApiResponse<List<TaskCardResponse>>> getTaskCards(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long roomId) {
        List<TaskCardResponse> responses = taskCardQueryUseCase.getTaskCards(roomId, authUser.userId()).stream()
                .map(TaskCardResponse::from)
                .toList();
        return ResponseEntity.ok(GlobalApiResponse.ok(MessengerResponseCode.TASK_CARD_LIST_RETRIEVED, responses));
    }

    @Operation(summary = "업무지시 완료 처리", description = "담당자 본인만 가능. 이미 완료한 담당자가 다시 호출해도 시각은 덮어쓰지 않는다.")
    @PatchMapping("/{roomId}/task-cards/{cardId}/complete")
    public ResponseEntity<Void> completeTaskCard(@AuthenticationPrincipal AuthUser authUser,
                                                  @PathVariable Long roomId,
                                                  @PathVariable Long cardId) {
        completeTaskUseCase.complete(new CompleteTaskCommand(roomId, cardId, authUser.userId()));
        return ResponseEntity.noContent().build();
    }
}
