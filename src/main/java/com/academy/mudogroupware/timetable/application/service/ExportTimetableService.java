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
import com.academy.mudogroupware.timetable.domain.model.TimetableClassroom;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportFormat;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportOptions;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportTimetableService implements ExportTimetableUseCase {

    private final GetTimetableSetUseCase getTimetableSetUseCase;
    private final GetTimetableSlotsUseCase getTimetableSlotsUseCase;
    private final List<TimetableExportRenderer> renderers;

    @Override
    public byte[] export(ExportTimetableCommand command) {
        log.info("event=timetable_export_시작 timetableSetId={}, format={}",
                command.timetableSetId(), command.format());

        TimetableSetDetailView set = getTimetableSetUseCase
                .getTimetableSet(command.timetableSetId());

        TimetableExportOptions options = new TimetableExportOptions(command.density());

        List<TimetableSlotView> allSortedSlots = getTimetableSlotsUseCase
                .getSlots(command.timetableSetId()).stream()
                .sorted(Comparator.comparing(TimetableSlotView::dayOfWeek)
                        .thenComparing(TimetableSlotView::startTime))
                .toList();

        // PDF는 인쇄용 고정 산출물이라 화면의 필터 상태와 무관하게 항상 세트 전체를 내보낸다.
        List<TimetableSlotView> slotsToRender = command.format() == TimetableExportFormat.PDF
                ? allSortedSlots
                : applyFilters(command, set, allSortedSlots);

        TimetableExportRenderer renderer = renderers.stream()
                .filter(r -> r.supports(command.format()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("지원하지 않는 내보내기 형식: " + command.format()));

        byte[] rendered = renderer.render(set.name(), slotsToRender, options);
        log.info("event=timetable_export_완료 timetableSetId={}, format={}, bytes={}",
                command.timetableSetId(), command.format(), rendered.length);
        return rendered;
    }

    private List<TimetableSlotView> applyFilters(
            ExportTimetableCommand command, TimetableSetDetailView set, List<TimetableSlotView> slots) {
        Map<String, String> floorByClassroomCode = set.classrooms().stream()
                .collect(Collectors.toMap(TimetableClassroom::code, TimetableClassroom::floor, (a, b) -> a));

        return slots.stream()
                .filter(slot -> command.dayOfWeek() == null || slot.dayOfWeek() == command.dayOfWeek())
                .filter(slot -> command.classType() == null || slot.classType() == command.classType())
                .filter(slot -> command.floor() == null
                        || command.floor().equals(floorByClassroomCode.get(slot.classroomCode())))
                .toList();
    }
}
