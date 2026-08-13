package com.academy.mudogroupware.notification.application.port;

import java.util.List;
import java.util.Set;

import com.academy.mudogroupware.notification.application.query.NotificationUserInfo;

public interface NotificationUserInfoPort {

    List<NotificationUserInfo> findUserInfo(Set<Long> userIds);
}
