package com.academy.mudogroupware.messenger.application.usecase;

import java.util.List;

import com.academy.mudogroupware.messenger.application.query.ChatRoomMemberView;

public interface ChatRoomMemberQueryUseCase {

    List<ChatRoomMemberView> getMembers(Long chatRoomId, Long requesterId);
}
