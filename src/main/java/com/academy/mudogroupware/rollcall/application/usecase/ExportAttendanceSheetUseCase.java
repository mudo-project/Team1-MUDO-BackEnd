package com.academy.mudogroupware.rollcall.application.usecase;

import java.time.LocalDate;

public interface ExportAttendanceSheetUseCase {

    byte[] exportSheet(Long lectureId, Long academyId, LocalDate date);
}
