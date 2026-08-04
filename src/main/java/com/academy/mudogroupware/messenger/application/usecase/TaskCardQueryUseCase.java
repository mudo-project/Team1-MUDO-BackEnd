package com.academy.mudogroupware.messenger.application.usecase;

import java.util.List;

import com.academy.mudogroupware.messenger.application.query.TaskCardView;

public interface TaskCardQueryUseCase {

    List<TaskCardView> getTaskCards(Long chatRoomId, Long requesterId);
}
