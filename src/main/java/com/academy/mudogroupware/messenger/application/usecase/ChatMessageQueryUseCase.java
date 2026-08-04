package com.academy.mudogroupware.messenger.application.usecase;

import java.time.LocalDateTime;

import com.academy.mudogroupware.messenger.application.query.ChatMessagePageView;

public interface ChatMessageQueryUseCase {

    // cursorCreatedAt/cursorMessageId가 둘 다 null이면 첫 페이지(최신 메시지부터) 조회를 의미한다.
    ChatMessagePageView getMessages(Long chatRoomId, Long requesterId, LocalDateTime cursorCreatedAt,
                                     Long cursorMessageId, int size);
}
