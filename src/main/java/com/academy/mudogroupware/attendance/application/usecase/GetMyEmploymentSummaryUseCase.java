package com.academy.mudogroupware.attendance.application.usecase;

import com.academy.mudogroupware.attendance.application.query.MyEmploymentSummaryView;

public interface GetMyEmploymentSummaryUseCase {
    MyEmploymentSummaryView getSummary(Long userId);
}
