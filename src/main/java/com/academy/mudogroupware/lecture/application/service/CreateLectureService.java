package com.academy.mudogroupware.lecture.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
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

        List<LectureSchedule> schedules = command.schedules().stream()
                .map(this::toSchedule)
                .toList();

        // 요청 안에서 이미 겹치는 게 확인되면, term/subject/classroom을 find-or-create하는
        // 부수효과(신규 생성 시 저장)를 만들기 전에 먼저 걸러낸다.
        validateNoInternalOverlap(schedules);

        Long termId = hasText(command.termName()) ? findOrCreateTerm(command.termName(), now) : null;
        Long subjectId = hasText(command.subjectName()) ? findOrCreateSubject(command.subjectName(), now) : null;
        Long classroomId = findOrCreateClassroom(command.classroomCode(), now);

        for (LectureSchedule schedule : schedules) {
            if (lectureRepository.existsOverlap(command.classroomCode(), schedule.getDayOfWeek(),
                    schedule.getStartTime(), schedule.getEndTime())) {
                throw new ClassroomTimeConflictException();
            }
        }

        Lecture lecture = Lecture.create(command.name(), command.classType(), command.classroomCode(),
                command.grade(), termId, subjectId, command.teacherId(), classroomId, command.teacherName(),
                command.subjectName(), command.feeType(), command.feeAmount(), schedules, now);

        return lectureRepository.save(lecture).getId();
    }

    private LectureSchedule toSchedule(ScheduleInput input) {
        return LectureSchedule.create(input.dayOfWeek(), input.startTime(), input.endTime());
    }

    // 아직 저장 전인 일정끼리는 existsOverlap(저장된 일정 대상 조회)로 못 잡는다. 한 요청 안에
    // 같은 요일에 겹치는 일정이 두 개 이상 들어오면 여기서 먼저 막는다.
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

    // 존재하는 강의실이면 findByNameForUpdate로 행을 잠가서, 그 뒤에 이어지는 시간 충돌 검사와
    // 강의 저장이 같은 트랜잭션 안에서 원자적으로 처리되게 한다(동시 등록 race condition 방지).
    //
    // 완전히 새 강의실이면 잠글 행이 아직 없어, 두 트랜잭션이 동시에 "없음"을 보고 둘 다 save를
    // 시도할 수 있다. 이때 DB 유니크 제약(uk_classroom_name)에 걸려 진 쪽은
    // DataIntegrityViolationException을 받는데, 그대로 두면 도메인과 무관한 409 에러로
    // 새어나간다. 다시 findByNameForUpdate로 조회하면 먼저 커밋된 강의실 행을 정상적으로
    // 찾아 잠그고, 이어지는 흐름(시간 충돌 검사)을 그대로 타게 된다.
    private Long findOrCreateClassroom(String name, LocalDateTime now) {
        Optional<Classroom> existing = classroomRepository.findByNameForUpdate(name);
        if (existing.isPresent()) {
            return existing.get().getId();
        }
        try {
            return classroomRepository.save(Classroom.create(name, now)).getId();
        } catch (DataIntegrityViolationException e) {
            return classroomRepository.findByNameForUpdate(name)
                    .map(Classroom::getId)
                    .orElseThrow(() -> e);
        }
    }
}
