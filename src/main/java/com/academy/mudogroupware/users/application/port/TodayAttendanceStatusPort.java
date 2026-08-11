package com.academy.mudogroupware.users.application.port;

import java.util.List;

public interface TodayAttendanceStatusPort {
    List<MemberTodayAttendanceStatus> findTodayStatusByUserIds(List<Long> userIds);
}
