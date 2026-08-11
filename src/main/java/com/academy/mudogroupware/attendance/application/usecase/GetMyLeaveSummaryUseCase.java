package com.academy.mudogroupware.attendance.application.usecase;

import com.academy.mudogroupware.attendance.application.query.MyLeaveSummaryView;

public interface GetMyLeaveSummaryUseCase {
    MyLeaveSummaryView getSummary(Long userId);
}
