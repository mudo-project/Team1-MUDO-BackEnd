package com.academy.mudogroupware.attendance.application.query;

import com.academy.mudogroupware.attendance.application.port.AttendanceCorrectionRequesterPort.Requester;

public record AdminAttendanceCorrectionView(
        AttendanceCorrectionView correction, Requester requester) {}
