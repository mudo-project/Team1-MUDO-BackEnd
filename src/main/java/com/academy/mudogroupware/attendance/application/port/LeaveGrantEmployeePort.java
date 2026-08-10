package com.academy.mudogroupware.attendance.application.port;

import java.util.List;

public interface LeaveGrantEmployeePort {

    List<LeaveGrantEmployee> findActiveEmployeesWithJoinedDate();
}
