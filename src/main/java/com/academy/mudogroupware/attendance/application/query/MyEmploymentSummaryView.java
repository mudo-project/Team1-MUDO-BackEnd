package com.academy.mudogroupware.attendance.application.query;

import java.time.LocalDate;

public record MyEmploymentSummaryView(LocalDate hireDate, long tenureDays) {
}
