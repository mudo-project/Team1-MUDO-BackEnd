package com.academy.mudogroupware.rollcall.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.rollcall.application.command.AttendanceEntryInput;
import com.academy.mudogroupware.rollcall.application.command.SaveAttendanceEntriesCommand;
import com.academy.mudogroupware.rollcall.application.port.EnrolledStudentRef;
import com.academy.mudogroupware.rollcall.application.port.LectureEnrollmentPort;
import com.academy.mudogroupware.rollcall.application.usecase.SaveAttendanceEntriesUseCase;
import com.academy.mudogroupware.rollcall.domain.exception.DuplicateStudentInRequestException;
import com.academy.mudogroupware.rollcall.domain.exception.RollcallLectureNotFoundException;
import com.academy.mudogroupware.rollcall.domain.exception.StudentNotEnrolledException;
import com.academy.mudogroupware.rollcall.domain.model.AttendanceEntry;
import com.academy.mudogroupware.rollcall.domain.repository.AttendanceEntryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SaveAttendanceEntriesService implements SaveAttendanceEntriesUseCase {

    private final LectureEnrollmentPort lectureEnrollmentPort;
    private final AttendanceEntryRepository attendanceEntryRepository;
    private final Clock clock;

    @Override
    public void saveEntries(SaveAttendanceEntriesCommand command) {
        lectureEnrollmentPort.findLecture(command.lectureId())
                .orElseThrow(RollcallLectureNotFoundException::new);
        validateNoDuplicateStudents(command.entries());
        validateAllStudentsEnrolled(command.lectureId(), command.entries());

        LocalDateTime now = LocalDateTime.now(clock);
        Map<Long, AttendanceEntry> existingEntries = attendanceEntryRepository
                .findByLectureIdAndDate(command.lectureId(), command.date())
                .stream()
                .collect(Collectors.toMap(AttendanceEntry::getStudentId, Function.identity(), (a, b) -> a));

        for (AttendanceEntryInput input : command.entries()) {
            Optional<AttendanceEntry> existing = Optional.ofNullable(existingEntries.get(input.studentId()));
            if (existing.isPresent()) {
                AttendanceEntry entry = existing.orElseThrow();
                entry.changeStatus(input.status(), input.note(), now);
                attendanceEntryRepository.save(entry);
            } else {
                AttendanceEntry entry = AttendanceEntry.create(command.lectureId(), input.studentId(), command.date(),
                        input.status(), input.note(), now);
                attendanceEntryRepository.save(entry);
            }
        }
    }

    private void validateNoDuplicateStudents(List<AttendanceEntryInput> entries) {
        Set<Long> seen = new HashSet<>();
        for (AttendanceEntryInput input : entries) {
            if (!seen.add(input.studentId())) {
                throw new DuplicateStudentInRequestException();
            }
        }
    }

    private void validateAllStudentsEnrolled(Long lectureId, List<AttendanceEntryInput> entries) {
        Set<Long> enrolledStudentIds = lectureEnrollmentPort.getEnrolledStudents(lectureId).stream()
                .map(EnrolledStudentRef::studentId)
                .collect(Collectors.toSet());
        boolean allEnrolled = entries.stream().allMatch(input -> enrolledStudentIds.contains(input.studentId()));
        if (!allEnrolled) {
            throw new StudentNotEnrolledException();
        }
    }
}
