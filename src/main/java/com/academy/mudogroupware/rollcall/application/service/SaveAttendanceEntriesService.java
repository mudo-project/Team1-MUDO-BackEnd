package com.academy.mudogroupware.rollcall.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.rollcall.application.command.AttendanceEntryInput;
import com.academy.mudogroupware.rollcall.application.command.SaveAttendanceEntriesCommand;
import com.academy.mudogroupware.rollcall.application.port.LectureEnrollmentPort;
import com.academy.mudogroupware.rollcall.application.usecase.SaveAttendanceEntriesUseCase;
import com.academy.mudogroupware.rollcall.domain.exception.RollcallLectureNotFoundException;
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
                .filter(ref -> ref.academyId().equals(command.academyId()))
                .orElseThrow(RollcallLectureNotFoundException::new);

        LocalDateTime now = LocalDateTime.now(clock);
        for (AttendanceEntryInput input : command.entries()) {
            Optional<AttendanceEntry> existing = attendanceEntryRepository.findByLectureIdAndStudentIdAndDate(
                    command.lectureId(), input.studentId(), command.date());
            if (existing.isPresent()) {
                AttendanceEntry entry = existing.get();
                entry.changeStatus(input.status(), input.note(), now);
                attendanceEntryRepository.save(entry);
            } else {
                AttendanceEntry entry = AttendanceEntry.create(command.academyId(), command.lectureId(),
                        input.studentId(), command.date(), input.status(), input.note(), now);
                attendanceEntryRepository.save(entry);
            }
        }
    }
}
