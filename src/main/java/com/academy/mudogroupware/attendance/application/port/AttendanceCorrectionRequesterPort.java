package com.academy.mudogroupware.attendance.application.port;

import java.util.Map;
import java.util.Set;

public interface AttendanceCorrectionRequesterPort {
    Map<Long, Requester> findByUserIds(Set<Long> userIds);

    record Requester(Long userId, String name, String roleName) {}
}
