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

    // 존재하는 강의실이면 findByNameForUpdate로 행을 잠가서, 이어지는 시간 충돌 검사 + 강의
    // 저장이 같은 트랜잭션 안에서 원자적으로 처리되게 한다(동시 등록 race condition 방지) —
    // 실사용에서 가장 흔한 경우(이미 등록된 강의실에 새 시간대를 추가)는 이걸로 안전하다.
    //
    // 알려진 한계: 완전히 새 강의실을 두 트랜잭션이 "동시에 처음" 만들려고 하면 여전히 경합이
    // 남는다. 이 findByNameForUpdate를 존재하지 않는 행에 먼저 걸면 InnoDB가 갭 락을 잡는데,
    // 두 트랜잭션이 동시에 그러면 서로의 갭 락을 기다리다 데드락이 난다(직접 재현 확인함).
    // 그렇다고 잠금 없는 findByName으로 먼저 존재를 확인하면, 같은 세션에서 뒤이은
    // findByNameForUpdate가 1차 캐시에 걸려 실제로 DB에 다시 잠금을 걸지 않는 Hibernate
    // 동작 때문에 "이미 존재하는 강의실" 케이스의 잠금 자체가 깨진다(둘 다 직접 재현 확인함).
    // 두 접근 모두 시도했다가 되돌렸다 — 제대로 고치려면 트랜잭션 재시도(Spring Retry 등)가
    // 필요한 수준이라 지금 스코프에서는 남겨두고, 신규 강의실 최초 등록이 정확히 동시에
    // 일어나는 드문 경우엔 한쪽이 DB 유니크 제약 위반(제네릭 409)으로 실패할 수 있다.
    private Long findOrCreateClassroom(String name, LocalDateTime now) {
        return classroomRepository.findByNameForUpdate(name)
                .map(Classroom::getId)
                .orElseGet(() -> classroomRepository.save(Classroom.create(name, now)).getId());
    }
}
