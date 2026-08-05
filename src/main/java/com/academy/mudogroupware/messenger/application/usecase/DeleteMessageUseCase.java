package com.academy.mudogroupware.messenger.application.usecase;

public interface DeleteMessageUseCase {

    void delete(Long chatRoomId, Long messageId, Long requesterId);
}
