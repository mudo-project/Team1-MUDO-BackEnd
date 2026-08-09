package com.academy.mudogroupware.timetable.application.port;

import java.awt.Color;
import java.util.List;
import java.util.Map;

import com.academy.mudogroupware.timetable.application.query.TimetableSlotView;
import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportFormat;

public interface TimetableExportRenderer {

    boolean supports(TimetableExportFormat format);

    byte[] render(String timetableSetName, List<TimetableSlotView> sortedSlots, Map<ClassType, Color> colorsByClassType);
}
