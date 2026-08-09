package com.academy.mudogroupware.timetable.application.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.academy.mudogroupware.timetable.application.command.ExportTimetableCommand;
import com.academy.mudogroupware.timetable.application.port.TimetableExportRenderer;
import com.academy.mudogroupware.timetable.application.query.TimetableSetDetailView;
import com.academy.mudogroupware.timetable.application.query.TimetableSlotView;
import com.academy.mudogroupware.timetable.application.usecase.ExportTimetableUseCase;
import com.academy.mudogroupware.timetable.application.usecase.GetTimetableSetUseCase;
import com.academy.mudogroupware.timetable.application.usecase.GetTimetableSlotsUseCase;
import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportColor;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExportTimetableService implements ExportTimetableUseCase {

    private final GetTimetableSetUseCase getTimetableSetUseCase;
    private final GetTimetableSlotsUseCase getTimetableSlotsUseCase;
    private final List<TimetableExportRenderer> renderers;

    @Override
    public byte[] export(ExportTimetableCommand command) {
        TimetableSetDetailView set = getTimetableSetUseCase
                .getTimetableSet(command.academyId(), command.timetableSetId());

        Map<ClassType, TimetableExportColor> colors = parseColors(command.colorHexByClassType());

        List<TimetableSlotView> sortedSlots = getTimetableSlotsUseCase
                .getSlots(command.academyId(), command.timetableSetId()).stream()
                .sorted(Comparator.comparing(TimetableSlotView::dayOfWeek)
                        .thenComparing(TimetableSlotView::startTime))
                .toList();

        TimetableExportRenderer renderer = renderers.stream()
                .filter(r -> r.supports(command.format()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("지원하지 않는 내보내기 형식: " + command.format()));

        return renderer.render(set.name(), sortedSlots, colors);
    }

    private Map<ClassType, TimetableExportColor> parseColors(Map<ClassType, String> colorHexByClassType) {
        return colorHexByClassType.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> TimetableExportColor.fromHex(entry.getValue())));
    }
}
