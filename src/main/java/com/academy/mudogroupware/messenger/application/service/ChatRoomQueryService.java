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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomQueryService implements ChatRoomQueryUseCase {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMemberDirectoryPort chatMemberDirectoryPort;

    @Override
    public List<ChatRoomSummaryView> getRooms(Long requesterId) {
        ChatMemberInfo requester = chatMemberDirectoryPort.getMember(requesterId);
        List<ChatRoom> chatRooms = chatRoomRepository.findAllByMember(requester.academyId(), requesterId);

        List<Long> otherMemberIds = chatRooms.stream()
                .filter(chatRoom -> chatRoom.getType() == ChatRoomType.DM)
                .map(chatRoom -> findOtherMemberId(chatRoom, requesterId))
                .toList();
        Map<Long, ChatMemberInfo> otherMembers = chatMemberDirectoryPort.getMembers(otherMemberIds);

        Map<Long, ChatMessage> latestMessages = chatMessageRepository.findLatestByChatRoomIds(
                chatRooms.stream().map(ChatRoom::getId).toList());

        return chatRooms.stream()
                .map(chatRoom -> toSummaryView(chatRoom, requesterId, otherMembers, latestMessages))
                .sorted(Comparator.comparing(ChatRoomQueryService::sortKey).reversed())
                .toList();
    }

    private ChatRoomSummaryView toSummaryView(ChatRoom chatRoom, Long requesterId,
                                               Map<Long, ChatMemberInfo> otherMembers,
                                               Map<Long, ChatMessage> latestMessages) {
        String name = chatRoom.getType() == ChatRoomType.DM
                ? findMemberName(otherMembers, findOtherMemberId(chatRoom, requesterId))
                : chatRoom.getName();

        ChatRoomMember member = chatRoom.findMember(requesterId).orElseThrow();
        long unreadCount = chatMessageRepository.countUnread(chatRoom.getId(), member.getLastReadAt());

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
