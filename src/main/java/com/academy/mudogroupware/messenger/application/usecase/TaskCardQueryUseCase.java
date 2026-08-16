package com.academy.mudogroupware.messenger.application.usecase;

import java.time.LocalDateTime;

import com.academy.mudogroupware.messenger.application.query.TaskCardPageView;
import com.academy.mudogroupware.messenger.application.query.TaskCardRole;

public interface TaskCardQueryUseCase {

    // cursorCreatedAt/cursorCardId가 둘 다 null이면 첫 페이지(최신 카드부터) 조회를 의미한다.
    TaskCardPageView getTaskCards(Long chatRoomId, Long requesterId, LocalDateTime cursorCreatedAt,
                                   Long cursorCardId, int size);

    // 특정 방이 아니라 requesterId가 참여 중인 모든 방을 가로질러, role에 따라 내가 전달한/받은
    // 업무지시 카드를 조회한다. cursorCreatedAt/cursorCardId가 둘 다 null이면 첫 페이지를 의미한다.
    TaskCardPageView getMyTaskCards(Long requesterId, TaskCardRole role, LocalDateTime cursorCreatedAt,
                                     Long cursorCardId, int size);
}
