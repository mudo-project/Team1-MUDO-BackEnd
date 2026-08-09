package com.academy.mudogroupware.timetable.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.timetable.application.command.ExportTimetableCommand;
import com.academy.mudogroupware.timetable.application.port.TimetableExportRenderer;
import com.academy.mudogroupware.timetable.application.query.TimetableSetDetailView;
import com.academy.mudogroupware.timetable.application.query.TimetableSlotView;
import com.academy.mudogroupware.timetable.application.usecase.GetTimetableSetUseCase;
import com.academy.mudogroupware.timetable.application.usecase.GetTimetableSlotsUseCase;
import com.academy.mudogroupware.timetable.domain.exception.InvalidExportColorException;
import com.academy.mudogroupware.timetable.domain.exception.TimetableSetNotFoundException;
import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.TimetableClassroom;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportFormat;
import com.academy.mudogroupware.timetable.domain.model.TimetableSetStatus;

@ExtendWith(MockitoExtension.class)
class ExportTimetableServiceTest {

    @Mock private GetTimetableSetUseCase getTimetableSetUseCase;
    @Mock private GetTimetableSlotsUseCase getTimetableSlotsUseCase;
    @Mock private TimetableExportRenderer excelRenderer;
    @Mock private TimetableExportRenderer pdfRenderer;

    private ExportTimetableService service;

    @BeforeEach
    void setUp() {
        service = new ExportTimetableService(
                getTimetableSetUseCase, getTimetableSlotsUseCase, List.of(excelRenderer, pdfRenderer));
    }

    private Map<ClassType, String> validColors() {
        Map<ClassType, String> colors = new EnumMap<>(ClassType.class);
        colors.put(ClassType.CLASS, "FFCC00");
        colors.put(ClassType.SPECIAL, "00AACC");
        colors.put(ClassType.CLINIC, "AA00CC");
        colors.put(ClassType.STANDING, "888888");
        colors.put(ClassType.EXAM, "FF0000");
        return colors;
    }

    @Test
    void exportDelegatesToSupportingRendererWithSortedSlots() {
        TimetableSetDetailView set = new TimetableSetDetailView(
                1L, "2026 여름특강", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16),
                LocalTime.of(8, 30), LocalTime.of(22, 0), Set.of(DayOfWeek.MONDAY), 30,
                List.of(new TimetableClassroom("6층", "601")), TimetableSetStatus.ACTIVE);
        when(getTimetableSetUseCase.getTimetableSet(1L, 1L)).thenReturn(set);

        TimetableSlotView tuesdaySlot = new TimetableSlotView(
                200L, ClassType.CLASS, DayOfWeek.TUESDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                "고3", "정T", "미적분");
        TimetableSlotView mondaySlot = new TimetableSlotView(
                100L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                "고3", "정T", "미적분");
        when(getTimetableSlotsUseCase.getSlots(1L, 1L)).thenReturn(List.of(tuesdaySlot, mondaySlot));

        when(excelRenderer.supports(TimetableExportFormat.EXCEL)).thenReturn(true);
        when(excelRenderer.render(any(), any(), any())).thenReturn(new byte[] {1, 2, 3});

        byte[] result = service.export(new ExportTimetableCommand(1L, 1L, TimetableExportFormat.EXCEL, validColors()));

        assertThat(result).containsExactly(1, 2, 3);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TimetableSlotView>> captor = ArgumentCaptor.forClass(List.class);
        verify(excelRenderer).render(eq("2026 여름특강"), captor.capture(), anyMap());
        assertThat(captor.getValue()).containsExactly(mondaySlot, tuesdaySlot);
    }

    @Test
    void exportThrowsWhenColorIsNotValidHex() {
        TimetableSetDetailView set = new TimetableSetDetailView(
                1L, "이름", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16),
                LocalTime.of(8, 30), LocalTime.of(22, 0), Set.of(DayOfWeek.MONDAY), 30,
                List.of(new TimetableClassroom("6층", "601")), TimetableSetStatus.ACTIVE);
        when(getTimetableSetUseCase.getTimetableSet(1L, 1L)).thenReturn(set);
        Map<ClassType, String> invalidColors = validColors();
        invalidColors.put(ClassType.CLASS, "ZZZZZZ");

        assertThatThrownBy(() -> service.export(
                new ExportTimetableCommand(1L, 1L, TimetableExportFormat.EXCEL, invalidColors)))
                .isInstanceOf(InvalidExportColorException.class);
    }

    @Test
    void exportPropagatesNotFoundFromGetTimetableSetUseCase() {
        when(getTimetableSetUseCase.getTimetableSet(1L, 999L)).thenThrow(new TimetableSetNotFoundException());

        assertThatThrownBy(() -> service.export(
                new ExportTimetableCommand(1L, 999L, TimetableExportFormat.EXCEL, validColors())))
                .isInstanceOf(TimetableSetNotFoundException.class);
    }
}
