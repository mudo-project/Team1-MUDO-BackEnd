package com.academy.mudogroupware.lecture.infrastructure.persistence;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.lecture.domain.model.Lecture;
import com.academy.mudogroupware.lecture.domain.model.LectureSchedule;
import com.academy.mudogroupware.lecture.domain.repository.LectureFilter;
import com.academy.mudogroupware.lecture.domain.repository.LectureRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class LectureRepositoryImpl implements LectureRepository {

    private final LectureJpaRepository lectureJpaRepository;

    @Override
    public Lecture save(Lecture lecture) {
        LectureEntity entity = LectureEntity.builder()
                .academyId(lecture.getAcademyId())
                .name(lecture.getName())
                .grade(lecture.getGrade())
                .termId(lecture.getTermId())
                .subjectId(lecture.getSubjectId())
                .teacherId(lecture.getTeacherId())
                .classroomId(lecture.getClassroomId())
                .feeType(lecture.getFeeType())
                .feeAmount(lecture.getFeeAmount())
                .build();
        lecture.getSchedules().forEach(schedule -> entity.addSchedule(toScheduleEntity(schedule)));
        return toDomain(lectureJpaRepository.save(entity));
    }

    @Override
    public Optional<Lecture> findById(Long id) {
        return lectureJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Lecture> findAllById(List<Long> ids) {
        return lectureJpaRepository.findAllById(ids).stream().map(this::toDomain).toList();
    }

    @Override
    public PageResult<Lecture> findAll(Long academyId, LectureFilter filter, int page, int size) {
        Slice<LectureEntity> slice = lectureJpaRepository.findAllByFilter(academyId, filter.termId(),
                filter.grade(), filter.subjectId(), filter.teacherId(), filter.classroomId(), filter.dayOfWeek(),
                PageRequest.of(page, size));
        List<Lecture> content = slice.getContent().stream().map(this::toDomain).toList();
        return PageResult.of(content, slice.getNumber(), slice.getSize(), slice.hasNext());
    }

    @Override
    public boolean existsOverlap(Long classroomId, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        return lectureJpaRepository.existsOverlap(classroomId, dayOfWeek, startTime, endTime);
    }

    private LectureScheduleEntity toScheduleEntity(LectureSchedule schedule) {
        return LectureScheduleEntity.builder()
                .dayOfWeek(schedule.getDayOfWeek())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .build();
    }

    private Lecture toDomain(LectureEntity entity) {
        List<LectureSchedule> schedules = entity.getSchedules().stream()
                .map(s -> LectureSchedule.restore(s.getId(), s.getDayOfWeek(), s.getStartTime(), s.getEndTime()))
                .toList();
        return Lecture.restore(entity.getId(), entity.getAcademyId(), entity.getName(), entity.getGrade(),
                entity.getTermId(), entity.getSubjectId(), entity.getTeacherId(), entity.getClassroomId(),
                entity.getFeeType(), entity.getFeeAmount(), schedules, entity.getCreatedAt());
    }
}
