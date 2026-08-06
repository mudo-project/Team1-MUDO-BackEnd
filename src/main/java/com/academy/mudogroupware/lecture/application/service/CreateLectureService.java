package com.academy.mudogroupware.lecture.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.lecture.application.command.CreateLectureCommand;
import com.academy.mudogroupware.lecture.application.command.ScheduleInput;
import com.academy.mudogroupware.lecture.application.usecase.CreateLectureUseCase;
import com.academy.mudogroupware.lecture.domain.exception.ClassroomTimeConflictException;
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
public class CreateLectureService implements CreateLectureUseCase {

    private final TermRepository termRepository;
    private final SubjectRepository subjectRepository;
    private final ClassroomRepository classroomRepository;
    private final LectureRepository lectureRepository;
    private final Clock clock;

    @Override
    public Long createLecture(CreateLectureCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);

        Long termId = findOrCreateTerm(command.academyId(), command.termName(), now);
        Long subjectId = findOrCreateSubject(command.academyId(), command.subjectName(), now);
        Long classroomId = findOrCreateClassroom(command.academyId(), command.classroomName(), now);

        List<LectureSchedule> schedules = command.schedules().stream()
                .map(this::toSchedule)
                .toList();

        for (LectureSchedule schedule : schedules) {
            if (lectureRepository.existsOverlap(classroomId, schedule.getDayOfWeek(), schedule.getStartTime(),
                    schedule.getEndTime())) {
                throw new ClassroomTimeConflictException();
            }
        }

        Lecture lecture = Lecture.create(command.academyId(), command.name(), command.grade(), termId, subjectId,
                command.teacherId(), classroomId, command.feeType(), command.feeAmount(), schedules, now);

        return lectureRepository.save(lecture).getId();
    }

    private LectureSchedule toSchedule(ScheduleInput input) {
        return LectureSchedule.create(input.dayOfWeek(), input.startTime(), input.endTime());
    }

    private Long findOrCreateTerm(Long academyId, String name, LocalDateTime now) {
        return termRepository.findByAcademyIdAndName(academyId, name)
                .map(Term::getId)
                .orElseGet(() -> termRepository.save(Term.create(academyId, name, now)).getId());
    }

    private Long findOrCreateSubject(Long academyId, String name, LocalDateTime now) {
        return subjectRepository.findByAcademyIdAndName(academyId, name)
                .map(Subject::getId)
                .orElseGet(() -> subjectRepository.save(Subject.create(academyId, name, now)).getId());
    }

    private Long findOrCreateClassroom(Long academyId, String name, LocalDateTime now) {
        return classroomRepository.findByAcademyIdAndName(academyId, name)
                .map(Classroom::getId)
                .orElseGet(() -> classroomRepository.save(Classroom.create(academyId, name, now)).getId());
    }
}
