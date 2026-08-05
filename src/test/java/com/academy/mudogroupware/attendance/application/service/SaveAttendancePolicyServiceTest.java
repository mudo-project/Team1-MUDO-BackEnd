package com.academy.mudogroupware.attendance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.attendance.application.command.SaveAttendancePolicyCommand;
import com.academy.mudogroupware.attendance.application.result.SaveAttendancePolicyResult;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;
import com.academy.mudogroupware.attendance.domain.model.AttendancePolicy;
import com.academy.mudogroupware.attendance.domain.model.AttendancePolicyWeekday;
import com.academy.mudogroupware.attendance.domain.model.OwnedAcademy;
import com.academy.mudogroupware.attendance.domain.repository.AcademyRepository;
import com.academy.mudogroupware.attendance.domain.repository.AttendancePolicyRepository;

@ExtendWith(MockitoExtension.class)
class SaveAttendancePolicyServiceTest {

    @Mock
    private AcademyRepository academyRepository;

    @Mock
    private AttendancePolicyRepository attendancePolicyRepository;

    @Test
    void createsPolicyWithEmptyWeekdaysWhenWeekdaysAreOmitted() {
        SaveAttendancePolicyService service =
                new SaveAttendancePolicyService(academyRepository, attendancePolicyRepository);
        when(academyRepository.findByOwnerUserId(10L))
                .thenReturn(Optional.of(new OwnedAcademy(1L, 10L)));
        when(attendancePolicyRepository.findByAcademyId(1L)).thenReturn(Optional.empty());
        when(attendancePolicyRepository.save(any(AttendancePolicy.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SaveAttendancePolicyResult result = service.save(new SaveAttendancePolicyCommand(
                10L, LocalTime.of(9, 0), LocalTime.of(18, 0), 10, false, null));

        assertFalse(result.weekdayExceptionEnabled());
        assertEquals(List.of(), result.weekdays());
    }

    @Test
    void preservesWeekdaysWhenTheyAreOmittedDuringUpdate() {
        SaveAttendancePolicyService service =
                new SaveAttendancePolicyService(academyRepository, attendancePolicyRepository);
        AttendancePolicy existing = AttendancePolicy.restore(
                5L, 1L, LocalTime.of(9, 0), LocalTime.of(18, 0), 0, true,
                List.of(new AttendancePolicyWeekday(
                        6, false, null, null)),
                LocalDateTime.now().minusDays(1), LocalDateTime.now().minusDays(1));
        when(academyRepository.findByOwnerUserId(10L))
                .thenReturn(Optional.of(new OwnedAcademy(1L, 10L)));
        when(attendancePolicyRepository.findByAcademyId(1L))
                .thenReturn(Optional.of(existing));
        when(attendancePolicyRepository.save(any(AttendancePolicy.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SaveAttendancePolicyResult result = service.save(new SaveAttendancePolicyCommand(
                10L, LocalTime.of(10, 0), LocalTime.of(19, 0), 5, false, null));

        assertFalse(result.weekdayExceptionEnabled());
        assertEquals(1, result.weekdays().size());
        assertEquals(6, result.weekdays().get(0).dayOfWeek());
    }

    @Test
    void replacesWeekdaysWithEmptyList() {
        SaveAttendancePolicyService service =
                new SaveAttendancePolicyService(academyRepository, attendancePolicyRepository);
        AttendancePolicy existing = AttendancePolicy.restore(
                5L, 1L, LocalTime.of(9, 0), LocalTime.of(18, 0), 0, true,
                List.of(new AttendancePolicyWeekday(6, false, null, null)),
                LocalDateTime.now().minusDays(1), LocalDateTime.now().minusDays(1));
        when(academyRepository.findByOwnerUserId(10L))
                .thenReturn(Optional.of(new OwnedAcademy(1L, 10L)));
        when(attendancePolicyRepository.findByAcademyId(1L))
                .thenReturn(Optional.of(existing));
        when(attendancePolicyRepository.save(any(AttendancePolicy.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SaveAttendancePolicyResult result = service.save(new SaveAttendancePolicyCommand(
                10L, LocalTime.of(9, 0), LocalTime.of(18, 0), 0, true, List.of()));

        assertEquals(List.of(), result.weekdays());
    }

    @Test
    void preservesWeekdaysWhenExceptionsAreDisabledWithEmptyList() {
        SaveAttendancePolicyService service =
                new SaveAttendancePolicyService(academyRepository, attendancePolicyRepository);
        AttendancePolicy existing = AttendancePolicy.restore(
                5L, 1L, LocalTime.of(9, 0), LocalTime.of(18, 0), 0, true,
                List.of(new AttendancePolicyWeekday(6, false, null, null)),
                LocalDateTime.now().minusDays(1), LocalDateTime.now().minusDays(1));
        when(academyRepository.findByOwnerUserId(10L))
                .thenReturn(Optional.of(new OwnedAcademy(1L, 10L)));
        when(attendancePolicyRepository.findByAcademyId(1L))
                .thenReturn(Optional.of(existing));
        when(attendancePolicyRepository.save(any(AttendancePolicy.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SaveAttendancePolicyResult result = service.save(new SaveAttendancePolicyCommand(
                10L, LocalTime.of(9, 0), LocalTime.of(18, 0), 0, false, List.of()));

        assertFalse(result.weekdayExceptionEnabled());
        assertEquals(1, result.weekdays().size());
        assertEquals(6, result.weekdays().get(0).dayOfWeek());
    }

    @Test
    void rejectsSaveWhenRequesterDoesNotOwnAcademy() {
        SaveAttendancePolicyService service =
                new SaveAttendancePolicyService(academyRepository, attendancePolicyRepository);
        when(academyRepository.findByOwnerUserId(10L)).thenReturn(Optional.empty());

        AttendanceException exception = assertThrows(
                AttendanceException.class,
                () -> service.save(new SaveAttendancePolicyCommand(
                        10L, LocalTime.of(9, 0), LocalTime.of(18, 0), 0, false, null)));

        assertSame(AttendanceErrorCode.ATTENDANCE_POLICY_SAVE_FORBIDDEN,
                exception.getErrorCode());
        verifyNoInteractions(attendancePolicyRepository);
    }
}
