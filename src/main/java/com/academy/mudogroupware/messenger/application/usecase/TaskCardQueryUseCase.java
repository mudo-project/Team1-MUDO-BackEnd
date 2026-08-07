package com.academy.mudogroupware.messenger.application.usecase;

import java.time.LocalDateTime;

import com.academy.mudogroupware.messenger.application.query.TaskCardPageView;

public interface TaskCardQueryUseCase {

    // cursorCreatedAt/cursorCardId가 둘 다 null이면 첫 페이지(최신 카드부터) 조회를 의미한다.
    TaskCardPageView getTaskCards(Long chatRoomId, Long requesterId, LocalDateTime cursorCreatedAt,
                                   Long cursorCardId, int size);
}
