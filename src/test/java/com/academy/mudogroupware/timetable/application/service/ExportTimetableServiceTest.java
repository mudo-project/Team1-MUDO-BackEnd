package com.academy.mudogroupware.timetable.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
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
import com.academy.mudogroupware.timetable.domain.model.TimetableExportColor;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportColorCriterion;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportDensity;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportFormat;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportOptions;
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

    private Map<String, String> validColors() {
        return Map.of("601", "FFCC00", "602", "00AACC");
    }

    private TimetableSetDetailView setWithClassrooms() {
        return new TimetableSetDetailView(
                1L, "2026 여름특강", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16),
                LocalTime.of(8, 30), LocalTime.of(22, 0), Set.of(DayOfWeek.MONDAY), 30,
                List.of(new TimetableClassroom("6층", "601"), new TimetableClassroom("5층", "602")),
                TimetableSetStatus.ACTIVE);
    }

    private ExportTimetableCommand command(TimetableExportFormat format, DayOfWeek dayOfWeek, String floor,
                                            ClassType classType) {
        return new ExportTimetableCommand(
                1L, 1L, format, TimetableExportColorCriterion.CLASSROOM, validColors(),
                TimetableExportDensity.NORMAL, dayOfWeek, floor, classType);
    }

    @Test
    void exportDelegatesToSupportingRendererWithSortedSlots() {
        when(getTimetableSetUseCase.getTimetableSet(1L, 1L)).thenReturn(setWithClassrooms());

        TimetableSlotView tuesdaySlot = new TimetableSlotView(
                200L, ClassType.CLASS, DayOfWeek.TUESDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                "고3", "정T", "미적분");
        TimetableSlotView mondaySlot = new TimetableSlotView(
                100L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                "고3", "정T", "미적분");
        when(getTimetableSlotsUseCase.getSlots(1L, 1L)).thenReturn(List.of(tuesdaySlot, mondaySlot));

        when(excelRenderer.supports(TimetableExportFormat.EXCEL)).thenReturn(true);
        when(excelRenderer.render(any(), any(), any())).thenReturn(new byte[] {1, 2, 3});

        byte[] result = service.export(command(TimetableExportFormat.EXCEL, null, null, null));

        assertThat(result).containsExactly(1, 2, 3);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TimetableSlotView>> slotsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<TimetableExportOptions> optionsCaptor = ArgumentCaptor.forClass(TimetableExportOptions.class);
        verify(excelRenderer).render(eq("2026 여름특강"), slotsCaptor.capture(), optionsCaptor.capture());
        assertThat(slotsCaptor.getValue()).containsExactly(mondaySlot, tuesdaySlot);

        TimetableExportOptions options = optionsCaptor.getValue();
        assertThat(options.colorCriterion()).isEqualTo(TimetableExportColorCriterion.CLASSROOM);
        assertThat(options.density()).isEqualTo(TimetableExportDensity.NORMAL);
        assertThat(options.colorFor("601", null, null)).isEqualTo(TimetableExportColor.fromHex("FFCC00"));
        assertThat(options.colorFor("602", null, null)).isEqualTo(TimetableExportColor.fromHex("00AACC"));
    }

    @Test
    void exportAppliesClassTypeFilterForNonPdfFormat() {
        when(getTimetableSetUseCase.getTimetableSet(1L, 1L)).thenReturn(setWithClassrooms());

        TimetableSlotView classSlot = new TimetableSlotView(
                100L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                "고3", "정T", "미적분");
        TimetableSlotView specialSlot = new TimetableSlotView(
                200L, ClassType.SPECIAL, DayOfWeek.MONDAY, "601", LocalTime.of(13, 0), LocalTime.of(15, 0),
                "고2", "오T", "물리");
        when(getTimetableSlotsUseCase.getSlots(1L, 1L)).thenReturn(List.of(classSlot, specialSlot));

        when(excelRenderer.supports(TimetableExportFormat.EXCEL)).thenReturn(true);
        when(excelRenderer.render(any(), any(), any())).thenReturn(new byte[0]);

        service.export(command(TimetableExportFormat.EXCEL, null, null, ClassType.CLASS));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TimetableSlotView>> captor = ArgumentCaptor.forClass(List.class);
        verify(excelRenderer).render(any(), captor.capture(), any());
        assertThat(captor.getValue()).containsExactly(classSlot);
    }

    @Test
    void exportAppliesDayOfWeekFilterForNonPdfFormat() {
        when(getTimetableSetUseCase.getTimetableSet(1L, 1L)).thenReturn(setWithClassrooms());

        TimetableSlotView mondaySlot = new TimetableSlotView(
                100L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                "고3", "정T", "미적분");
        TimetableSlotView tuesdaySlot = new TimetableSlotView(
                200L, ClassType.CLASS, DayOfWeek.TUESDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                "고3", "정T", "미적분");
        when(getTimetableSlotsUseCase.getSlots(1L, 1L)).thenReturn(List.of(mondaySlot, tuesdaySlot));

        when(excelRenderer.supports(TimetableExportFormat.EXCEL)).thenReturn(true);
        when(excelRenderer.render(any(), any(), any())).thenReturn(new byte[0]);

        service.export(command(TimetableExportFormat.EXCEL, DayOfWeek.MONDAY, null, null));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TimetableSlotView>> captor = ArgumentCaptor.forClass(List.class);
        verify(excelRenderer).render(any(), captor.capture(), any());
        assertThat(captor.getValue()).containsExactly(mondaySlot);
    }

    @Test
    void exportAppliesFloorFilterUsingSetClassrooms() {
        when(getTimetableSetUseCase.getTimetableSet(1L, 1L)).thenReturn(setWithClassrooms());

        TimetableSlotView floor6Slot = new TimetableSlotView(
                100L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                "고3", "정T", "미적분");
        TimetableSlotView floor5Slot = new TimetableSlotView(
                200L, ClassType.CLASS, DayOfWeek.MONDAY, "602", LocalTime.of(13, 0), LocalTime.of(15, 0),
                "고2", "오T", "물리");
        when(getTimetableSlotsUseCase.getSlots(1L, 1L)).thenReturn(List.of(floor6Slot, floor5Slot));

        when(excelRenderer.supports(TimetableExportFormat.EXCEL)).thenReturn(true);
        when(excelRenderer.render(any(), any(), any())).thenReturn(new byte[0]);

        service.export(command(TimetableExportFormat.EXCEL, null, "6층", null));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TimetableSlotView>> captor = ArgumentCaptor.forClass(List.class);
        verify(excelRenderer).render(any(), captor.capture(), any());
        assertThat(captor.getValue()).containsExactly(floor6Slot);
    }

    @Test
    void exportIgnoresFiltersForPdfFormat() {
        when(getTimetableSetUseCase.getTimetableSet(1L, 1L)).thenReturn(setWithClassrooms());

        TimetableSlotView mondaySlot = new TimetableSlotView(
                100L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                "고3", "정T", "미적분");
        TimetableSlotView tuesdaySlot = new TimetableSlotView(
                200L, ClassType.CLASS, DayOfWeek.TUESDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                "고3", "정T", "미적분");
        when(getTimetableSlotsUseCase.getSlots(1L, 1L)).thenReturn(List.of(mondaySlot, tuesdaySlot));

        when(pdfRenderer.supports(TimetableExportFormat.PDF)).thenReturn(true);
        when(pdfRenderer.render(any(), any(), any())).thenReturn(new byte[0]);

        // 스코프와 무관하게 PDF는 항상 인쇄용 전체를 내보낸다.
        service.export(command(TimetableExportFormat.PDF, DayOfWeek.MONDAY, null, null));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TimetableSlotView>> captor = ArgumentCaptor.forClass(List.class);
        verify(pdfRenderer).render(any(), captor.capture(), any());
        assertThat(captor.getValue()).containsExactly(mondaySlot, tuesdaySlot);
    }

    @Test
    void exportThrowsWhenColorIsNotValidHex() {
        when(getTimetableSetUseCase.getTimetableSet(1L, 1L)).thenReturn(setWithClassrooms());
        ExportTimetableCommand command = new ExportTimetableCommand(
                1L, 1L, TimetableExportFormat.EXCEL, TimetableExportColorCriterion.CLASSROOM,
                Map.of("601", "ZZZZZZ"), TimetableExportDensity.NORMAL, null, null, null);

        assertThatThrownBy(() -> service.export(command))
                .isInstanceOf(InvalidExportColorException.class);
    }

    @Test
    void exportPropagatesNotFoundFromGetTimetableSetUseCase() {
        when(getTimetableSetUseCase.getTimetableSet(1L, 999L)).thenThrow(new TimetableSetNotFoundException());
        ExportTimetableCommand command = new ExportTimetableCommand(
                1L, 999L, TimetableExportFormat.EXCEL, TimetableExportColorCriterion.CLASSROOM, validColors(),
                TimetableExportDensity.NORMAL, null, null, null);

        assertThatThrownBy(() -> service.export(command))
                .isInstanceOf(TimetableSetNotFoundException.class);
    }
}
