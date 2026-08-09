package com.academy.mudogroupware.messenger.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.messenger.application.command.CreateTaskCardCommand;
import com.academy.mudogroupware.messenger.application.usecase.CreateTaskCardUseCase;
import com.academy.mudogroupware.messenger.domain.event.TaskCardCreatedEvent;
import com.academy.mudogroupware.messenger.domain.exception.MessengerErrorCode;
import com.academy.mudogroupware.messenger.domain.exception.MessengerException;
import com.academy.mudogroupware.messenger.domain.model.ChatRoom;
import com.academy.mudogroupware.messenger.domain.model.ChatTaskAssignee;
import com.academy.mudogroupware.messenger.domain.model.ChatTaskCard;
import com.academy.mudogroupware.messenger.domain.repository.ChatRoomRepository;
import com.academy.mudogroupware.messenger.domain.repository.ChatTaskCardRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CreateTaskCardService implements CreateTaskCardUseCase {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatTaskCardRepository chatTaskCardRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Override
    public Long createTaskCard(CreateTaskCardCommand command) {
        log.info("event=task_card_create_시작 chatRoomId={}, assignerId={}", command.chatRoomId(),
                command.assignerId());
        try {
            List<Long> requestedAssigneeIds = command.assigneeIds() == null ? List.of() : command.assigneeIds();

            ChatRoom chatRoom = chatRoomRepository.findById(command.chatRoomId())
                    .orElseThrow(() -> new MessengerException(MessengerErrorCode.CHAT_ROOM_NOT_FOUND));
            if (!chatRoom.isMember(command.assignerId())) {
                throw new MessengerException(MessengerErrorCode.NOT_ROOM_MEMBER);
            }
            boolean allAssigneesAreMembers = requestedAssigneeIds.stream().allMatch(chatRoom::isMember);
            if (!allAssigneesAreMembers) {
                throw new MessengerException(MessengerErrorCode.NOT_ROOM_MEMBER);
            }

            LocalDateTime createdAt = LocalDateTime.now(clock);
            ChatTaskCard chatTaskCard = ChatTaskCard.create(command.chatRoomId(), command.assignerId(),
                    command.content(), command.dueDate(), requestedAssigneeIds, createdAt);
            ChatTaskCard saved = chatTaskCardRepository.save(chatTaskCard);
            List<Long> assigneeIds = saved.getAssignees().stream().map(ChatTaskAssignee::getUserId).toList();
            eventPublisher.publishEvent(new TaskCardCreatedEvent(saved.getChatRoomId(), saved.getId(),
                    saved.getAssignerUserId(), saved.getContent(), saved.getDueDate(), assigneeIds,
                    saved.getCreatedAt()));
            log.info("event=task_card_create_완료 chatRoomId={}, assignerId={}, cardId={}", command.chatRoomId(),
                    command.assignerId(), saved.getId());
            return saved.getId();
        } catch (RuntimeException e) {
            log.warn("event=task_card_create_실패 chatRoomId={}, assignerId={}, reason={}", command.chatRoomId(),
                    command.assignerId(), e.getMessage());
            throw e;
        }
    }
}
