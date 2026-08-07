package com.academy.mudogroupware.attendance.application.query;

public record MyAttendanceDashboardView(MyMonthlyAttendanceView calendar,
                                        MyTodayAttendanceView today,
                                        MyLeaveSummaryView leave,
                                        MyEmploymentSummaryView employment) {
}
