package com.academy.mudogroupware.messenger.application.service;

import java.util.List;
import java.util.Map;

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

        ChatRoom chatRoom = ChatRoom.create(requester.academyId(), requester.userId(), participantIds,
                command.name());
        return chatRoomRepository.save(chatRoom).getId();
    }
}
