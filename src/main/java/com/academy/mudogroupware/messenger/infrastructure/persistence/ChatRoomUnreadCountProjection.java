package com.academy.mudogroupware.messenger.infrastructure.persistence;

public interface ChatRoomUnreadCountProjection {

    Long getChatRoomId();

    Long getUnreadCount();
}
