package com.academy.mudogroupware.messenger.application.usecase;

import com.academy.mudogroupware.messenger.application.command.CreateChatRoomCommand;

public interface CreateChatRoomUseCase {

    Long createRoom(CreateChatRoomCommand command);
}
