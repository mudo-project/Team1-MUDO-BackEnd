package com.academy.mudogroupware.rollcall.application.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.rollcall.application.port.EnrolledStudentRef;
import com.academy.mudogroupware.rollcall.application.port.LectureEnrollmentPort;
import com.academy.mudogroupware.rollcall.application.port.LectureRef;
import com.academy.mudogroupware.rollcall.application.query.RosterEntryView;
import com.academy.mudogroupware.rollcall.application.query.RosterSummaryView;
import com.academy.mudogroupware.rollcall.application.query.RosterView;
import com.academy.mudogroupware.rollcall.application.usecase.GetLectureRosterUseCase;
import com.academy.mudogroupware.rollcall.domain.exception.RollcallLectureNotFoundException;
import com.academy.mudogroupware.rollcall.domain.model.AttendanceEntry;
import com.academy.mudogroupware.rollcall.domain.repository.AttendanceEntryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetLectureRosterService implements GetLectureRosterUseCase {

    private final LectureEnrollmentPort lectureEnrollmentPort;
    private final AttendanceEntryRepository attendanceEntryRepository;

    @Override
    public RosterView getRoster(Long lectureId, Long academyId, LocalDate date) {
        LectureRef lecture = lectureEnrollmentPort.findLecture(lectureId)
                .filter(ref -> ref.academyId().equals(academyId))
                .orElseThrow(RollcallLectureNotFoundException::new);

        List<EnrolledStudentRef> students = lectureEnrollmentPort.getEnrolledStudents(lectureId);
        Map<Long, AttendanceEntry> entriesByStudent = attendanceEntryRepository
                .findByLectureIdAndDate(lectureId, date).stream()
                .collect(Collectors.toMap(AttendanceEntry::getStudentId, Function.identity()));

        List<RosterEntryView> entries = students.stream()
                .map(student -> {
                    AttendanceEntry entry = entriesByStudent.get(student.studentId());
                    return new RosterEntryView(student.studentId(), student.name(), student.grade(),
                            student.parentPhone(), entry != null ? entry.getStatus() : null,
                            entry != null ? entry.getNote() : null);
                })
                .toList();

        return new RosterView(lecture.id(), lecture.name(), date, entries, summarize(entries));
    }

    private RosterSummaryView summarize(List<RosterEntryView> entries) {
        int present = 0;
        int absent = 0;
        int late = 0;
        int online = 0;
        int etc = 0;
        for (RosterEntryView entry : entries) {
            if (entry.status() == null) {
                continue;
            }
            switch (entry.status()) {
                case PRESENT -> present++;
                case ABSENT -> absent++;
                case LATE -> late++;
                case ONLINE -> online++;
                case ETC -> etc++;
            }
        }
        return new RosterSummaryView(entries.size(), present, absent, late, online, etc);
    }
}
