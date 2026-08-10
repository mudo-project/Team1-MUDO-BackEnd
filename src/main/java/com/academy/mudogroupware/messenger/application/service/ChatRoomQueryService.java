package com.academy.mudogroupware.messenger.application.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.messenger.application.port.ChatMemberDirectoryPort;
import com.academy.mudogroupware.messenger.application.port.ChatMemberInfo;
import com.academy.mudogroupware.messenger.application.query.ChatRoomSummaryView;
import com.academy.mudogroupware.messenger.application.usecase.ChatRoomQueryUseCase;
import com.academy.mudogroupware.messenger.domain.model.ChatMessage;
import com.academy.mudogroupware.messenger.domain.model.ChatRoom;
import com.academy.mudogroupware.messenger.domain.model.ChatRoomMember;
import com.academy.mudogroupware.messenger.domain.model.ChatRoomType;
import com.academy.mudogroupware.messenger.domain.repository.ChatMessageRepository;
import com.academy.mudogroupware.messenger.domain.repository.ChatRoomRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomQueryService implements ChatRoomQueryUseCase {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMemberDirectoryPort chatMemberDirectoryPort;

    @Override
    public List<ChatRoomSummaryView> getRooms(Long requesterId) {
        log.info("event=chat_room_list_시작 requesterId={}", requesterId);
        ChatMemberInfo requester = chatMemberDirectoryPort.getMember(requesterId);
        List<ChatRoom> chatRooms = chatRoomRepository.findAllByMember(requester.academyId(), requesterId);

        List<Long> otherMemberIds = chatRooms.stream()
                .filter(chatRoom -> chatRoom.getType() == ChatRoomType.DM)
                .map(chatRoom -> findOtherMemberId(chatRoom, requesterId))
                .toList();
        Map<Long, ChatMemberInfo> otherMembers = chatMemberDirectoryPort.getMembers(otherMemberIds);

        List<Long> chatRoomIds = chatRooms.stream().map(ChatRoom::getId).toList();
        Map<Long, ChatMessage> latestMessages = chatMessageRepository.findLatestByChatRoomIds(chatRoomIds);
        Map<Long, Long> unreadCounts = chatMessageRepository.countUnreadByRequester(requesterId, chatRoomIds);

        List<ChatRoomSummaryView> views = chatRooms.stream()
                .map(chatRoom -> toSummaryView(chatRoom, requesterId, otherMembers, latestMessages, unreadCounts))
                .sorted(Comparator.comparing(ChatRoomQueryService::sortKey)
                        .thenComparing(ChatRoomSummaryView::id)
                        .reversed())
                .toList();
        log.info("event=chat_room_list_완료 requesterId={}, count={}", requesterId, views.size());
        return views;
    }

    private ChatRoomSummaryView toSummaryView(ChatRoom chatRoom, Long requesterId,
                                               Map<Long, ChatMemberInfo> otherMembers,
                                               Map<Long, ChatMessage> latestMessages,
                                               Map<Long, Long> unreadCounts) {
        String name = chatRoom.getType() == ChatRoomType.DM
                ? findMemberName(otherMembers, findOtherMemberId(chatRoom, requesterId))
                : chatRoom.getName();

        long unreadCount = unreadCounts.getOrDefault(chatRoom.getId(), 0L);

        ChatMessage latestMessage = latestMessages.get(chatRoom.getId());
        String preview = latestMessage != null ? toPreviewText(latestMessage) : null;
        LocalDateTime lastMessageAt = latestMessage != null ? latestMessage.getCreatedAt() : null;

        return new ChatRoomSummaryView(chatRoom.getId(), name, chatRoom.getType(), unreadCount, preview,
                lastMessageAt, chatRoom.getCreatedAt());
    }

    private String toPreviewText(ChatMessage message) {
        return switch (message.getMessageType()) {
            case TEXT -> message.getContent();
            case IMAGE -> "사진을 보냈습니다.";
            case FILE -> "파일을 보냈습니다.";
        };
    }

    private static LocalDateTime sortKey(ChatRoomSummaryView view) {
        return view.lastMessageAt() != null ? view.lastMessageAt() : view.createdAt();
    }

    private Long findOtherMemberId(ChatRoom chatRoom, Long requesterId) {
        return chatRoom.getMembers().stream()
                .map(ChatRoomMember::getUserId)
                .filter(userId -> !userId.equals(requesterId))
                .findFirst()
                .orElse(requesterId);
    }

    private String findMemberName(Map<Long, ChatMemberInfo> members, Long userId) {
        ChatMemberInfo memberInfo = members.get(userId);
        return memberInfo != null ? memberInfo.name() : null;
    }
}
