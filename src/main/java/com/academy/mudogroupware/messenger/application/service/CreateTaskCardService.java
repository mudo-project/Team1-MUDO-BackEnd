package com.academy.mudogroupware.messenger.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.messenger.application.command.CreateTaskCardCommand;
import com.academy.mudogroupware.messenger.application.usecase.CreateTaskCardUseCase;
import com.academy.mudogroupware.messenger.domain.exception.MessengerErrorCode;
import com.academy.mudogroupware.messenger.domain.exception.MessengerException;
import com.academy.mudogroupware.messenger.domain.model.ChatRoom;
import com.academy.mudogroupware.messenger.domain.model.ChatTaskCard;
import com.academy.mudogroupware.messenger.domain.repository.ChatRoomRepository;
import com.academy.mudogroupware.messenger.domain.repository.ChatTaskCardRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateTaskCardService implements CreateTaskCardUseCase {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatTaskCardRepository chatTaskCardRepository;
    private final Clock clock;

    @Override
    public Long createTaskCard(CreateTaskCardCommand command) {
        ChatRoom chatRoom = chatRoomRepository.findById(command.chatRoomId())
                .orElseThrow(() -> new MessengerException(MessengerErrorCode.CHAT_ROOM_NOT_FOUND));
        if (!chatRoom.isMember(command.assignerId())) {
            throw new MessengerException(MessengerErrorCode.NOT_ROOM_MEMBER);
        }
        boolean allAssigneesAreMembers = command.assigneeIds().stream().allMatch(chatRoom::isMember);
        if (!allAssigneesAreMembers) {
            throw new MessengerException(MessengerErrorCode.NOT_ROOM_MEMBER);
        }

        ChatTaskCard chatTaskCard = ChatTaskCard.create(command.chatRoomId(), command.assignerId(),
                command.content(), command.dueDate(), command.assigneeIds(), LocalDateTime.now(clock));
        return chatTaskCardRepository.save(chatTaskCard).getId();
    }
}
