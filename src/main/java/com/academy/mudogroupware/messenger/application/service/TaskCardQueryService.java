package com.academy.mudogroupware.messenger.application.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.messenger.application.port.ChatMemberDirectoryPort;
import com.academy.mudogroupware.messenger.application.port.ChatMemberInfo;
import com.academy.mudogroupware.messenger.application.query.TaskAssigneeView;
import com.academy.mudogroupware.messenger.application.query.TaskCardView;
import com.academy.mudogroupware.messenger.application.usecase.TaskCardQueryUseCase;
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
@Transactional(readOnly = true)
public class TaskCardQueryService implements TaskCardQueryUseCase {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatTaskCardRepository chatTaskCardRepository;
    private final ChatMemberDirectoryPort chatMemberDirectoryPort;

    @Override
    public List<TaskCardView> getTaskCards(Long chatRoomId, Long requesterId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new MessengerException(MessengerErrorCode.CHAT_ROOM_NOT_FOUND));
        if (!chatRoom.isMember(requesterId)) {
            throw new MessengerException(MessengerErrorCode.NOT_ROOM_MEMBER);
        }

        List<ChatTaskCard> chatTaskCards = chatTaskCardRepository.findAllByChatRoomId(chatRoomId);

        List<Long> memberIds = chatTaskCards.stream()
                .flatMap(card -> Stream.concat(Stream.of(card.getAssignerUserId()),
                        card.getAssignees().stream().map(ChatTaskAssignee::getUserId)))
                .distinct()
                .toList();
        Map<Long, ChatMemberInfo> members = chatMemberDirectoryPort.getMembers(memberIds);

        return chatTaskCards.stream().map(chatTaskCard -> toView(chatTaskCard, members)).toList();
    }

    private TaskCardView toView(ChatTaskCard chatTaskCard, Map<Long, ChatMemberInfo> members) {
        List<TaskAssigneeView> assignees = chatTaskCard.getAssignees().stream()
                .map(assignee -> toAssigneeView(assignee, members))
                .toList();
        String assignerName = findName(members, chatTaskCard.getAssignerUserId());

        return new TaskCardView(chatTaskCard.getId(), chatTaskCard.getChatRoomId(),
                chatTaskCard.getAssignerUserId(), assignerName, chatTaskCard.getContent(),
                chatTaskCard.getDueDate(), assignees, chatTaskCard.getCompletedCount(),
                chatTaskCard.getAssigneeCount(), chatTaskCard.isFullyCompleted(), chatTaskCard.getCreatedAt());
    }

    private TaskAssigneeView toAssigneeView(ChatTaskAssignee assignee, Map<Long, ChatMemberInfo> members) {
        return new TaskAssigneeView(assignee.getUserId(), findName(members, assignee.getUserId()),
                assignee.getCompletedAt());
    }

    private String findName(Map<Long, ChatMemberInfo> members, Long userId) {
        ChatMemberInfo memberInfo = members.get(userId);
        return memberInfo != null ? memberInfo.name() : null;
    }
}
