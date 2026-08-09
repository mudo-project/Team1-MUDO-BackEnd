package com.academy.mudogroupware.timetable.application.port;

import java.util.List;

import com.academy.mudogroupware.timetable.application.query.TimetableSlotView;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportFormat;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportOptions;

public interface TimetableExportRenderer {

    boolean supports(TimetableExportFormat format);

    byte[] render(String timetableSetName, List<TimetableSlotView> sortedSlots, TimetableExportOptions options);
}
