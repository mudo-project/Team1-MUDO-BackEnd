package com.academy.mudogroupware.messenger.application.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.messenger.application.command.UpdateTaskCardCommand;
import com.academy.mudogroupware.messenger.application.usecase.UpdateTaskCardUseCase;
import com.academy.mudogroupware.messenger.domain.event.TaskCardUpdatedEvent;
import com.academy.mudogroupware.messenger.domain.exception.MessengerErrorCode;
import com.academy.mudogroupware.messenger.domain.exception.MessengerException;
import com.academy.mudogroupware.messenger.domain.model.ChatRoom;
import com.academy.mudogroupware.messenger.domain.model.ChatTaskAssignee;
import com.academy.mudogroupware.messenger.domain.model.ChatTaskCard;
import com.academy.mudogroupware.messenger.domain.repository.ChatRoomRepository;
import com.academy.mudogroupware.messenger.domain.repository.ChatTaskCardRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateTaskCardService implements UpdateTaskCardUseCase {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatTaskCardRepository chatTaskCardRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void update(UpdateTaskCardCommand command) {
        ChatRoom chatRoom = chatRoomRepository.findById(command.chatRoomId())
                .orElseThrow(() -> new MessengerException(MessengerErrorCode.CHAT_ROOM_NOT_FOUND));
        ChatTaskCard chatTaskCard = chatTaskCardRepository.findById(command.cardId())
                .orElseThrow(() -> new MessengerException(MessengerErrorCode.TASK_CARD_NOT_FOUND));
        if (!chatTaskCard.getChatRoomId().equals(command.chatRoomId())) {
            throw new MessengerException(MessengerErrorCode.TASK_CARD_NOT_FOUND);
        }
        boolean allAssigneesAreMembers = command.assigneeIds().stream().allMatch(chatRoom::isMember);
        if (!allAssigneesAreMembers) {
            throw new MessengerException(MessengerErrorCode.NOT_ROOM_MEMBER);
        }

        List<Long> beforeAssigneeIds = chatTaskCard.getAssignees().stream()
                .map(ChatTaskAssignee::getUserId).toList();

        chatTaskCard.update(command.requesterId(), command.content(), command.dueDate(), command.assigneeIds());

        List<Long> afterAssigneeIds = chatTaskCard.getAssignees().stream()
                .map(ChatTaskAssignee::getUserId).toList();
        Set<Long> beforeSet = new HashSet<>(beforeAssigneeIds);
        Set<Long> afterSet = new HashSet<>(afterAssigneeIds);
        List<Long> addedUserIds = afterAssigneeIds.stream().filter(id -> !beforeSet.contains(id)).toList();
        List<Long> removedUserIds = beforeAssigneeIds.stream().filter(id -> !afterSet.contains(id)).toList();

        chatTaskCardRepository.updateContent(chatTaskCard.getId(), chatTaskCard.getContent(),
                chatTaskCard.getDueDate());
        chatTaskCardRepository.replaceAssignees(chatTaskCard.getId(), addedUserIds, removedUserIds);

        eventPublisher.publishEvent(new TaskCardUpdatedEvent(chatTaskCard.getChatRoomId(), chatTaskCard.getId(),
                chatTaskCard.getContent(), chatTaskCard.getDueDate(), afterAssigneeIds));
    }
}
