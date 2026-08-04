package com.academy.mudogroupware.messenger.application.usecase;

import java.util.List;

import com.academy.mudogroupware.messenger.application.query.ChatRoomSummaryView;

public interface ChatRoomQueryUseCase {

    List<ChatRoomSummaryView> getRooms(Long requesterId);
}
