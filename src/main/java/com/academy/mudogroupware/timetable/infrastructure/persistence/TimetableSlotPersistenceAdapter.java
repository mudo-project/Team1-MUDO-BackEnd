package com.academy.mudogroupware.timetable.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.timetable.domain.model.TimetableSlot;
import com.academy.mudogroupware.timetable.domain.repository.TimetableSlotRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class TimetableSlotPersistenceAdapter implements TimetableSlotRepository {

    private final TimetableSlotJpaRepository timetableSlotJpaRepository;

    @Override
    public TimetableSlot save(TimetableSlot slot) {
        TimetableSlotEntity entity = slot.getId() != null ? updateExisting(slot) : toEntity(slot);
        return toDomain(timetableSlotJpaRepository.save(entity));
    }

    @Override
    public Optional<TimetableSlot> findById(Long id) {
        return timetableSlotJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<TimetableSlot> findAllByTimetableSetId(Long timetableSetId) {
        return timetableSlotJpaRepository.findAllByTimetableSetId(timetableSetId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<TimetableSlot> findAllByTimetableSetIdAndClassroomCode(Long timetableSetId, String classroomCode) {
        return timetableSlotJpaRepository.findAllByTimetableSetIdAndClassroomCode(timetableSetId, classroomCode)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        timetableSlotJpaRepository.deleteById(id);
    }

    private TimetableSlotEntity updateExisting(TimetableSlot domain) {
        TimetableSlotEntity entity = timetableSlotJpaRepository.getReferenceById(domain.getId());
        entity.update(domain.getClassType(), domain.getDayOfWeek(), domain.getClassroomCode(),
                domain.getStartTime(), domain.getEndTime(), domain.getGrade(), domain.getTeacherName(),
                domain.getSubjectName(), domain.getColor(), domain.getEffectiveFrom(), domain.getEffectiveUntil());
        return entity;
    }

    private TimetableSlotEntity toEntity(TimetableSlot domain) {
        return TimetableSlotEntity.builder()
                .id(domain.getId())
                .timetableSetId(domain.getTimetableSetId())
                .classType(domain.getClassType())
                .dayOfWeek(domain.getDayOfWeek())
                .classroomCode(domain.getClassroomCode())
                .startTime(domain.getStartTime())
                .endTime(domain.getEndTime())
                .grade(domain.getGrade())
                .teacherName(domain.getTeacherName())
                .subjectName(domain.getSubjectName())
                .color(domain.getColor())
                .effectiveFrom(domain.getEffectiveFrom())
                .effectiveUntil(domain.getEffectiveUntil())
                .build();
    }

    private TimetableSlot toDomain(TimetableSlotEntity entity) {
        return TimetableSlot.restore(
                entity.getId(), entity.getTimetableSetId(), entity.getClassType(), entity.getDayOfWeek(),
                entity.getClassroomCode(), entity.getStartTime(), entity.getEndTime(), entity.getGrade(),
                entity.getTeacherName(), entity.getSubjectName(), entity.getColor(), entity.getEffectiveFrom(),
                entity.getEffectiveUntil(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
