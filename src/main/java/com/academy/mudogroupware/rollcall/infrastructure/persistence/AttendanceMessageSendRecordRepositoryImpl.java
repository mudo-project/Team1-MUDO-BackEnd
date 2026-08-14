package com.academy.mudogroupware.rollcall.infrastructure.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.rollcall.domain.model.AttendanceMessageSendRecord;
import com.academy.mudogroupware.rollcall.domain.model.AttendanceMessageSendStatus;
import com.academy.mudogroupware.rollcall.domain.repository.AttendanceMessageSendRecordRepository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AttendanceMessageSendRecordRepositoryImpl implements AttendanceMessageSendRecordRepository {

    private final AttendanceMessageSendRecordJpaRepository attendanceMessageSendRecordJpaRepository;
    private final EntityManager entityManager;

    @Override
    public AttendanceMessageSendRecord createOrGetExisting(Long lectureId, Long studentId, LocalDate date,
                                                            LocalDateTime now) {
        // (lecture_id, student_id, entry_date) 유니크 제약을 이용한 insert-first 패턴.
        AttendanceMessageSendRecordEntity entity = AttendanceMessageSendRecordEntity.builder()
                .lectureId(lectureId)
                .studentId(studentId)
                .date(date)
                .status(AttendanceMessageSendStatus.PENDING)
                .build();
        try {
            return toDomain(attendanceMessageSendRecordJpaRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException e) {
            // save 실패로 식별자 없는 엔티티가 영속성 컨텍스트에 남아있으면, 그 상태로 이어서 조회할 때
            // Hibernate가 "세션이 오염됐다"는 AssertionFailure를 던진다 — 조회 전에 반드시 비워야 한다.
            entityManager.clear();
            return attendanceMessageSendRecordJpaRepository.findByLectureIdAndStudentIdAndDate(lectureId, studentId, date)
                    .map(this::toDomain)
                    .orElseThrow(() -> e);
        }
    }

    @Override
    public AttendanceMessageSendRecord save(AttendanceMessageSendRecord record) {
        AttendanceMessageSendRecordEntity entity = attendanceMessageSendRecordJpaRepository
                .getReferenceById(record.getId());
        entity.changeStatus(record.getStatus());
        return toDomain(attendanceMessageSendRecordJpaRepository.saveAndFlush(entity));
    }

    private AttendanceMessageSendRecord toDomain(AttendanceMessageSendRecordEntity entity) {
        return AttendanceMessageSendRecord.restore(entity.getId(), entity.getLectureId(), entity.getStudentId(),
                entity.getDate(), entity.getStatus(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
