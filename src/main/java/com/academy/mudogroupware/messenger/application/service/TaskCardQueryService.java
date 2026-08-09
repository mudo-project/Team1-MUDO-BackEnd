package com.academy.mudogroupware.messenger.application.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.messenger.application.port.ChatMemberDirectoryPort;
import com.academy.mudogroupware.messenger.application.port.ChatMemberInfo;
import com.academy.mudogroupware.messenger.application.query.TaskAssigneeView;
import com.academy.mudogroupware.messenger.application.query.TaskCardPageView;
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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskCardQueryService implements TaskCardQueryUseCase {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatTaskCardRepository chatTaskCardRepository;
    private final ChatMemberDirectoryPort chatMemberDirectoryPort;

    @Override
    public TaskCardPageView getTaskCards(Long chatRoomId, Long requesterId, LocalDateTime cursorCreatedAt,
                                          Long cursorCardId, int size) {
        log.info("event=task_card_list_시작 chatRoomId={}, requesterId={}", chatRoomId, requesterId);
        try {
            if (size < 1 || size > 100) {
                throw new MessengerException(MessengerErrorCode.INVALID_TASK_CARD_PAGE_SIZE);
            }

            boolean cursorProvided = cursorCreatedAt != null || cursorCardId != null;
            boolean cursorComplete = cursorCreatedAt != null && cursorCardId != null;
            if (cursorProvided && !cursorComplete) {
                throw new MessengerException(MessengerErrorCode.INVALID_TASK_CARD_CURSOR);
            }

            ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                    .orElseThrow(() -> new MessengerException(MessengerErrorCode.CHAT_ROOM_NOT_FOUND));
            if (!chatRoom.isMember(requesterId)) {
                throw new MessengerException(MessengerErrorCode.NOT_ROOM_MEMBER);
            }

            List<ChatTaskCard> fetched = chatTaskCardRepository.findPage(chatRoomId, cursorCreatedAt, cursorCardId,
                    size);
            boolean hasNext = fetched.size() > size;
            List<ChatTaskCard> pageCards = hasNext ? fetched.subList(0, size) : fetched;

            List<Long> memberIds = pageCards.stream()
                    .flatMap(card -> Stream.concat(Stream.of(card.getAssignerUserId()),
                            card.getAssignees().stream().map(ChatTaskAssignee::getUserId)))
                    .distinct()
                    .toList();
            Map<Long, ChatMemberInfo> members = chatMemberDirectoryPort.getMembers(memberIds);

            List<TaskCardView> taskCardViews = pageCards.stream().map(card -> toView(card, members)).toList();

            ChatTaskCard lastInPage = pageCards.isEmpty() ? null : pageCards.get(pageCards.size() - 1);
            LocalDateTime nextCursorCreatedAt = hasNext ? lastInPage.getCreatedAt() : null;
            Long nextCursorCardId = hasNext ? lastInPage.getId() : null;

            log.info("event=task_card_list_완료 chatRoomId={}, requesterId={}, count={}, hasNext={}", chatRoomId,
                    requesterId, taskCardViews.size(), hasNext);
            return new TaskCardPageView(taskCardViews, hasNext, nextCursorCreatedAt, nextCursorCardId);
        } catch (RuntimeException e) {
            log.warn("event=task_card_list_실패 chatRoomId={}, requesterId={}, reason={}", chatRoomId, requesterId,
                    e.getMessage());
            throw e;
        }
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
