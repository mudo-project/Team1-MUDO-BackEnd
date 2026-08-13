package com.academy.mudogroupware.lecture.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.lecture.application.command.ScheduleInput;
import com.academy.mudogroupware.lecture.application.command.UpdateLectureCommand;
import com.academy.mudogroupware.lecture.application.usecase.UpdateLectureUseCase;
import com.academy.mudogroupware.lecture.domain.exception.ClassroomTimeConflictException;
import com.academy.mudogroupware.lecture.domain.exception.LectureNotFoundException;
import com.academy.mudogroupware.lecture.domain.model.Classroom;
import com.academy.mudogroupware.lecture.domain.model.Lecture;
import com.academy.mudogroupware.lecture.domain.model.LectureSchedule;
import com.academy.mudogroupware.lecture.domain.model.Subject;
import com.academy.mudogroupware.lecture.domain.model.Term;
import com.academy.mudogroupware.lecture.domain.repository.ClassroomRepository;
import com.academy.mudogroupware.lecture.domain.repository.LectureRepository;
import com.academy.mudogroupware.lecture.domain.repository.SubjectRepository;
import com.academy.mudogroupware.lecture.domain.repository.TermRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateLectureService implements UpdateLectureUseCase {

    private final TermRepository termRepository;
    private final SubjectRepository subjectRepository;
    private final ClassroomRepository classroomRepository;
    private final LectureRepository lectureRepository;
    private final Clock clock;

    @Override
    public void updateLecture(UpdateLectureCommand command) {
        Lecture existing = lectureRepository.findById(command.lectureId())
                .orElseThrow(LectureNotFoundException::new);
        LocalDateTime now = LocalDateTime.now(clock);

        List<LectureSchedule> schedules = command.schedules().stream()
                .map(this::toSchedule)
                .toList();
        validateNoInternalOverlap(schedules);

        Long termId = hasText(command.termName()) ? findOrCreateTerm(command.termName(), now) : null;
        Long subjectId = hasText(command.subjectName()) ? findOrCreateSubject(command.subjectName(), now) : null;
        Long classroomId = findOrCreateClassroom(command.classroomCode(), now);

        for (LectureSchedule schedule : schedules) {
            if (lectureRepository.existsOverlapExcludingLecture(command.lectureId(), command.classroomCode(),
                    schedule.getDayOfWeek(), schedule.getStartTime(), schedule.getEndTime())) {
                throw new ClassroomTimeConflictException();
            }
        }

        Lecture updated = Lecture.restore(command.lectureId(), command.name(), command.classType(),
                command.classroomCode(), command.grade(), termId, subjectId, command.teacherId(), classroomId,
                command.teacherName(), command.subjectName(), command.feeType(), command.feeAmount(), schedules,
                existing.getCreatedAt());

        lectureRepository.save(updated);
    }

    private LectureSchedule toSchedule(ScheduleInput input) {
        return LectureSchedule.create(input.dayOfWeek(), input.startTime(), input.endTime());
    }

    private void validateNoInternalOverlap(List<LectureSchedule> schedules) {
        for (int i = 0; i < schedules.size(); i++) {
            for (int j = i + 1; j < schedules.size(); j++) {
                if (schedules.get(i).overlaps(schedules.get(j))) {
                    throw new ClassroomTimeConflictException();
                }
            }
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Long findOrCreateTerm(String name, LocalDateTime now) {
        return termRepository.findByName(name)
                .map(Term::getId)
                .orElseGet(() -> termRepository.save(Term.create(name, now)).getId());
    }

    private Long findOrCreateSubject(String name, LocalDateTime now) {
        return subjectRepository.findByName(name)
                .map(Subject::getId)
                .orElseGet(() -> subjectRepository.save(Subject.create(name, now)).getId());
    }

    private Long findOrCreateClassroom(String name, LocalDateTime now) {
        return classroomRepository.findByNameForUpdate(name)
                .map(Classroom::getId)
                .orElseGet(() -> classroomRepository.save(Classroom.create(name, now)).getId());
    }
}
