package com.academy.mudogroupware.lecture.infrastructure.persistence;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.lecture.domain.exception.LectureNotFoundException;
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
        LectureEntity entity = lecture.getId() == null ? toNewEntity(lecture) : updateExisting(lecture);
        return toDomain(lectureJpaRepository.save(entity));
    }

    @Override
    public Optional<Lecture> findById(Long id) {
        return lectureJpaRepository.findByIdAndDeletedAtIsNull(id).map(this::toDomain);
    }

    @Override
    public List<Lecture> findAllById(List<Long> ids) {
        return lectureJpaRepository.findAllByIdInAndDeletedAtIsNull(ids).stream().map(this::toDomain).toList();
    }

    @Override
    public PageResult<Lecture> findAll(LectureFilter filter, int page, int size) {
        Slice<LectureEntity> slice = lectureJpaRepository.findAllByFilter(filter.termId(),
                filter.grade(), filter.subjectName(), filter.teacherName(), filter.classroomCode(), filter.dayOfWeek(),
                PageRequest.of(page, size));
        List<Lecture> content = slice.getContent().stream().map(this::toDomain).toList();
        return PageResult.of(content, slice.getNumber(), slice.getSize(), slice.hasNext());
    }

    @Override
    public boolean existsOverlap(String classroomCode, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        return lectureJpaRepository.existsOverlap(classroomCode, dayOfWeek, startTime, endTime);
    }

    @Override
    public boolean existsOverlapExcludingLecture(Long lectureId, String classroomCode, DayOfWeek dayOfWeek,
                                                 LocalTime startTime, LocalTime endTime) {
        return lectureJpaRepository.existsOverlapExcludingLecture(lectureId, classroomCode, dayOfWeek,
                startTime, endTime);
    }

    @Override
    public void deleteById(Long id, LocalDateTime deletedAt) {
        LectureEntity entity = lectureJpaRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(LectureNotFoundException::new);
        entity.markDeleted(deletedAt);
    }

    @Override
    public List<String> findDistinctTeacherNames() {
        return lectureJpaRepository.findDistinctTeacherNames();
    }

    @Override
    public List<String> findDistinctSubjectNames() {
        return lectureJpaRepository.findDistinctSubjectNames();
    }

    @Override
    public List<Long> findDistinctTermIds() {
        return lectureJpaRepository.findDistinctTermIds();
    }

    @Override
    public List<String> findDistinctClassroomCodes() {
        return lectureJpaRepository.findDistinctClassroomCodes();
    }

    private LectureEntity toNewEntity(Lecture lecture) {
        LectureEntity entity = LectureEntity.builder()
                .name(lecture.getName())
                .classType(lecture.getClassType())
                .classroomCode(lecture.getClassroomCode())
                .grade(lecture.getGrade())
                .termId(lecture.getTermId())
                .subjectId(lecture.getSubjectId())
                .subjectName(lecture.getSubjectName())
                .teacherId(lecture.getTeacherId())
                .teacherName(lecture.getTeacherName())
                .classroomId(lecture.getClassroomId())
                .feeType(lecture.getFeeType())
                .feeAmount(lecture.getFeeAmount())
                .build();
        lecture.getSchedules().forEach(schedule -> entity.addSchedule(toScheduleEntity(schedule)));
        return entity;
    }

    private LectureEntity updateExisting(Lecture lecture) {
        LectureEntity entity = lectureJpaRepository.findByIdAndDeletedAtIsNull(lecture.getId())
                .orElseThrow(LectureNotFoundException::new);
        entity.update(lecture.getName(), lecture.getClassType(), lecture.getClassroomCode(), lecture.getGrade(),
                lecture.getTermId(), lecture.getSubjectId(), lecture.getSubjectName(), lecture.getTeacherId(),
                lecture.getTeacherName(), lecture.getClassroomId(), lecture.getFeeType(), lecture.getFeeAmount());
        entity.replaceSchedules(lecture.getSchedules().stream().map(this::toScheduleEntity).toList());
        return entity;
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
        return Lecture.restore(entity.getId(), entity.getName(), entity.getClassType(), entity.getClassroomCode(),
                entity.getGrade(), entity.getTermId(), entity.getSubjectId(), entity.getTeacherId(),
                entity.getClassroomId(), entity.getTeacherName(), entity.getSubjectName(), entity.getFeeType(),
                entity.getFeeAmount(), schedules, entity.getCreatedAt());
    }
}
