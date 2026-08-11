package com.academy.mudogroupware.lecture.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

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

    // 존재 여부는 잠금 없는 findByName으로 먼저 확인한다. 존재하지 않는 행에
    // findByNameForUpdate(SELECT ... FOR UPDATE)를 바로 쓰면 InnoDB가 그 자리에 갭 락을
    // 잡는데, 두 트랜잭션이 동시에 같은 신규 강의실 이름을 조회하면 서로의 갭 락을 기다리다
    // 데드락이 난다(MySQLTransactionRollbackException, CI에서 실제로 재현됨) — 그래서
    // "잠긴 조회 -> 없으면 저장" 순서로는 못 만든다.
    //
    // 존재하면 findByNameForUpdate로 그 행을 잠가서, 이어지는 시간 충돌 검사 + 강의 저장이
    // 같은 트랜잭션 안에서 원자적으로 처리되게 한다(동시 등록 race condition 방지). 완전히
    // 새 강의실이면 잠금 없이 바로 save를 시도하고, 그 사이 다른 트랜잭션이 먼저 같은 이름으로
    // 커밋했으면 유니크 제약(uk_classroom_name)에 걸려 DataIntegrityViolationException을
    // 받는다 — 이 시점엔 상대방이 이미 커밋을 마쳤으므로 findByNameForUpdate로 안전하게
    // 다시 조회해 잠그고 이어간다.
    private Long findOrCreateClassroom(String name, LocalDateTime now) {
        if (classroomRepository.findByName(name).isEmpty()) {
            try {
                return classroomRepository.save(Classroom.create(name, now)).getId();
            } catch (DataIntegrityViolationException e) {
                // 아래 findByNameForUpdate로 넘어가서 방금 커밋된 행을 잠그고 이어간다.
            }
        }
        return classroomRepository.findByNameForUpdate(name)
                .map(Classroom::getId)
                .orElseThrow(() -> new IllegalStateException("강의실 조회/생성에 실패했습니다: " + name));
    }
}
