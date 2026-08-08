package com.academy.mudogroupware.messenger.application.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.messenger.application.port.ChatMemberDirectoryPort;
import com.academy.mudogroupware.messenger.application.port.ChatMemberInfo;
import com.academy.mudogroupware.messenger.application.query.ChatRoomMemberView;
import com.academy.mudogroupware.messenger.application.usecase.ChatRoomMemberQueryUseCase;
import com.academy.mudogroupware.messenger.domain.exception.MessengerErrorCode;
import com.academy.mudogroupware.messenger.domain.exception.MessengerException;
import com.academy.mudogroupware.messenger.domain.model.ChatRoom;
import com.academy.mudogroupware.messenger.domain.model.ChatRoomMember;
import com.academy.mudogroupware.messenger.domain.repository.ChatRoomRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomMemberQueryService implements ChatRoomMemberQueryUseCase {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberDirectoryPort chatMemberDirectoryPort;

    @Override
    public List<ChatRoomMemberView> getMembers(Long chatRoomId, Long requesterId) {
        log.info("event=chat_room_member_list_시작 chatRoomId={}, requesterId={}", chatRoomId, requesterId);
        try {
            ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                    .orElseThrow(() -> new MessengerException(MessengerErrorCode.CHAT_ROOM_NOT_FOUND));
            if (!chatRoom.isMember(requesterId)) {
                throw new MessengerException(MessengerErrorCode.NOT_ROOM_MEMBER);
            }

            List<Long> memberIds = chatRoom.getMembers().stream()
                    .map(ChatRoomMember::getUserId)
                    .toList();
            Map<Long, ChatMemberInfo> members = chatMemberDirectoryPort.getMembers(memberIds);

            List<ChatRoomMemberView> views = chatRoom.getMembers().stream()
                    .map(member -> toMemberView(member, members))
                    .toList();
            log.info("event=chat_room_member_list_완료 chatRoomId={}, requesterId={}, count={}", chatRoomId,
                    requesterId, views.size());
            return views;
        } catch (RuntimeException e) {
            log.warn("event=chat_room_member_list_실패 chatRoomId={}, requesterId={}, reason={}", chatRoomId,
                    requesterId, e.getMessage());
            throw e;
        }
    }

    private ChatRoomMemberView toMemberView(ChatRoomMember member, Map<Long, ChatMemberInfo> members) {
        ChatMemberInfo memberInfo = members.get(member.getUserId());
        return new ChatRoomMemberView(member.getUserId(), memberInfo != null ? memberInfo.name() : null,
                member.getLastReadAt());
    }
}
