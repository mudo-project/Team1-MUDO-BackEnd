package com.academy.mudogroupware.messenger.infrastructure.persistence;

public interface MessageUnreadCountProjection {

    Long getMessageId();

    Long getUnreadCount();
}
