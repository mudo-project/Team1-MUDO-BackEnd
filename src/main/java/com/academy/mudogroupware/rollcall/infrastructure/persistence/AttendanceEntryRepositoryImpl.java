package com.academy.mudogroupware.rollcall.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.rollcall.domain.model.AttendanceEntry;
import com.academy.mudogroupware.rollcall.domain.repository.AttendanceEntryRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AttendanceEntryRepositoryImpl implements AttendanceEntryRepository {

    private final AttendanceEntryJpaRepository attendanceEntryJpaRepository;

    @Override
    public Optional<AttendanceEntry> findByLectureIdAndStudentIdAndDate(Long lectureId, Long studentId,
                                                                         LocalDate date) {
        return attendanceEntryJpaRepository.findByLectureIdAndStudentIdAndDate(lectureId, studentId, date)
                .map(this::toDomain);
    }

    @Override
    public List<AttendanceEntry> findByLectureIdAndDate(Long lectureId, LocalDate date) {
        return attendanceEntryJpaRepository.findAllByLectureIdAndDate(lectureId, date).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public AttendanceEntry save(AttendanceEntry entry) {
        AttendanceEntryEntity entity = entry.getId() != null ? updateExisting(entry) : toNewEntity(entry);
        return toDomain(attendanceEntryJpaRepository.save(entity));
    }

    private AttendanceEntryEntity toNewEntity(AttendanceEntry entry) {
        return AttendanceEntryEntity.builder()
                .academyId(entry.getAcademyId())
                .lectureId(entry.getLectureId())
                .studentId(entry.getStudentId())
                .date(entry.getDate())
                .status(entry.getStatus())
                .note(entry.getNote())
                .build();
    }

    private AttendanceEntryEntity updateExisting(AttendanceEntry entry) {
        AttendanceEntryEntity entity = attendanceEntryJpaRepository.getReferenceById(entry.getId());
        entity.changeStatus(entry.getStatus(), entry.getNote());
        return entity;
    }

    private AttendanceEntry toDomain(AttendanceEntryEntity entity) {
        return AttendanceEntry.restore(entity.getId(), entity.getAcademyId(), entity.getLectureId(),
                entity.getStudentId(), entity.getDate(), entity.getStatus(), entity.getNote(), entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
