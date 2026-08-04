package com.academy.mudogroupware.messenger.application.usecase;

import com.academy.mudogroupware.messenger.application.query.ChatMessagePageView;

public interface ChatMessageQueryUseCase {

    ChatMessagePageView getMessages(Long chatRoomId, Long requesterId, int page, int size);
}
