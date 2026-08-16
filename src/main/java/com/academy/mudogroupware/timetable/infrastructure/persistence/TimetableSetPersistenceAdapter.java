package com.academy.mudogroupware.timetable.infrastructure.persistence;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.timetable.domain.exception.TimetableSetNotFoundException;
import com.academy.mudogroupware.timetable.domain.exception.TimetableSetUpdateConflictException;
import com.academy.mudogroupware.timetable.domain.model.TimetableClassroom;
import com.academy.mudogroupware.timetable.domain.model.TimetableSet;
import com.academy.mudogroupware.timetable.domain.repository.TimetableSetRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class TimetableSetPersistenceAdapter implements TimetableSetRepository {

    private final TimetableSetJpaRepository timetableSetJpaRepository;

    @Override
    public TimetableSet save(TimetableSet timetableSet) {
        if (timetableSet.getId() != null) {
            try {
                TimetableSetEntity entity = updateExisting(timetableSet);
                timetableSetJpaRepository.flush();
                return toDomain(entity);
            } catch (EntityNotFoundException exception) {
                throw new TimetableSetNotFoundException();
            } catch (OptimisticLockingFailureException exception) {
                throw new TimetableSetUpdateConflictException(exception);
            }
        }
        return toDomain(timetableSetJpaRepository.save(toEntity(timetableSet)));
    }

    @Override
    public Optional<TimetableSet> findById(Long id) {
        return timetableSetJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<TimetableSet> findByIdForUpdate(Long id) {
        return timetableSetJpaRepository.findByIdForUpdate(id).map(this::toDomain);
    }

    @Override
    public List<TimetableSet> findAll() {
        return timetableSetJpaRepository.findAllByOrderByStartDateDesc().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        timetableSetJpaRepository.deleteById(id);
    }

    private TimetableSetEntity updateExisting(TimetableSet domain) {
        TimetableSetEntity entity = timetableSetJpaRepository.getReferenceById(domain.getId());
        entity.update(domain.getName(), domain.getStartDate(), domain.getEndDate(),
                domain.getOperatingStartTime(), domain.getOperatingEndTime(), joinDays(domain.getOperatingDays()),
                domain.getSlotUnitMinutes(), toEmbeddables(domain.getClassrooms()));
        return entity;
    }

    private TimetableSetEntity toEntity(TimetableSet domain) {
        return TimetableSetEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .startDate(domain.getStartDate())
                .endDate(domain.getEndDate())
                .operatingStartTime(domain.getOperatingStartTime())
                .operatingEndTime(domain.getOperatingEndTime())
                .operatingDays(joinDays(domain.getOperatingDays()))
                .slotUnitMinutes(domain.getSlotUnitMinutes())
                .classrooms(toEmbeddables(domain.getClassrooms()))
                .build();
    }

    private TimetableSet toDomain(TimetableSetEntity entity) {
        return TimetableSet.restore(
                entity.getId(), entity.getName(), entity.getStartDate(), entity.getEndDate(),
                entity.getOperatingStartTime(), entity.getOperatingEndTime(), splitDays(entity.getOperatingDays()),
                entity.getSlotUnitMinutes(), toDomainClassrooms(entity.getClassrooms()),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private String joinDays(Set<DayOfWeek> days) {
        return days.stream().map(DayOfWeek::name).collect(Collectors.joining(","));
    }

    private Set<DayOfWeek> splitDays(String value) {
        return Arrays.stream(value.split(",")).map(DayOfWeek::valueOf).collect(Collectors.toSet());
    }

    private List<TimetableClassroomEmbeddable> toEmbeddables(List<TimetableClassroom> classrooms) {
        return classrooms.stream()
                .map(c -> new TimetableClassroomEmbeddable(c.floor(), c.code()))
                .toList();
    }

    private List<TimetableClassroom> toDomainClassrooms(List<TimetableClassroomEmbeddable> embeddables) {
        return embeddables.stream()
                .map(e -> new TimetableClassroom(e.getFloor(), e.getCode()))
                .toList();
    }
}
