package com.academy.mudogroupware.messenger.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.messenger.application.command.CreateChatRoomCommand;
import com.academy.mudogroupware.messenger.application.port.ChatMemberDirectoryPort;
import com.academy.mudogroupware.messenger.application.port.ChatMemberInfo;
import com.academy.mudogroupware.messenger.application.usecase.CreateChatRoomUseCase;
import com.academy.mudogroupware.messenger.domain.exception.MessengerErrorCode;
import com.academy.mudogroupware.messenger.domain.exception.MessengerException;
import com.academy.mudogroupware.messenger.domain.model.ChatRoom;
import com.academy.mudogroupware.messenger.domain.repository.ChatRoomRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateChatRoomService implements CreateChatRoomUseCase {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberDirectoryPort chatMemberDirectoryPort;
    private final Clock clock;

    @Override
    public Long createRoom(CreateChatRoomCommand command) {
        ChatMemberInfo requester = chatMemberDirectoryPort.getMember(command.requesterId());

        List<Long> participantIds = command.participantIds();
        Map<Long, ChatMemberInfo> participants = chatMemberDirectoryPort.getMembers(participantIds);
        if (participants.size() < participantIds.stream().distinct().count()) {
            throw new MessengerException(MessengerErrorCode.INVALID_PARTICIPANT);
        }
        boolean crossAcademy = participants.values().stream()
                .anyMatch(participant -> !participant.academyId().equals(requester.academyId()));
        if (crossAcademy) {
            throw new MessengerException(MessengerErrorCode.CROSS_ACADEMY_INVITE);
        }

        Set<Long> inviteeIds = new LinkedHashSet<>(participantIds);
        inviteeIds.remove(requester.userId());
        if (inviteeIds.size() == 1) {
            Long otherUserId = inviteeIds.iterator().next();
            return chatRoomRepository.findDirectMessage(requester.academyId(), requester.userId(), otherUserId)
                    .map(ChatRoom::getId)
                    .orElseGet(() -> createRoom(requester, participantIds, command.name()));
        }

        return createRoom(requester, participantIds, command.name());
    }

    private Long createRoom(ChatMemberInfo requester, List<Long> participantIds, String name) {
        ChatRoom chatRoom = ChatRoom.create(requester.academyId(), requester.userId(), participantIds,
                name, LocalDateTime.now(clock));
        return chatRoomRepository.save(chatRoom).getId();
    }
}
