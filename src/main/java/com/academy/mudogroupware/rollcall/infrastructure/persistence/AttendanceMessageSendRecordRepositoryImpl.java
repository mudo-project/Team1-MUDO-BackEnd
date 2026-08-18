package com.academy.mudogroupware.rollcall.infrastructure.persistence;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.rollcall.domain.model.AttendanceMessageSendRecord;
import com.academy.mudogroupware.rollcall.domain.model.AttendanceMessageSendStatus;
import com.academy.mudogroupware.rollcall.domain.model.AttendanceStatus;
import com.academy.mudogroupware.rollcall.domain.repository.AttendanceMessageSendRecordRepository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AttendanceMessageSendRecordRepositoryImpl implements AttendanceMessageSendRecordRepository {

    // SENDING을 선점한 요청이 완료 전에 죽으면(서버 크래시 등) 이 시간이 지나도록 끝나지 않은 레코드는
    // "결과를 알 수 없는" 것으로 보고 INDETERMINATE로 전환한다. 공급자 상태 조회 없이 만료됐다는
    // 이유만으로 자동 재발송하면 그 사이 실제로 발송이 끝났을 경우 중복 발송이 되므로, 자동 재시도는
    // 여전히 차단하고(INDETERMINATE 정책 재사용) 관리자 확인을 거치도록 한다.
    private static final Duration CLAIM_STALE_AFTER = Duration.ofMinutes(5);
    private static final String STALE_SENDING_REASON = "발송 처리 중 응답이 끊겨(서버 재시작 추정) 실제 발송 여부를 확인할 수 없습니다.";

    private final AttendanceMessageSendRecordJpaRepository attendanceMessageSendRecordJpaRepository;
    private final EntityManager entityManager;
    private final Clock clock;

    @Override
    public AttendanceMessageSendRecord createOrGetExisting(Long lectureId, Long studentId, LocalDate date,
                                                            AttendanceStatus attendanceStatus) {
        // (lecture_id, student_id, entry_date, attendance_status) 유니크 제약을 이용한 insert-first 패턴.
        AttendanceMessageSendRecordEntity entity = AttendanceMessageSendRecordEntity.builder()
                .lectureId(lectureId)
                .studentId(studentId)
                .date(date)
                .attendanceStatus(attendanceStatus)
                .status(AttendanceMessageSendStatus.PENDING)
                .build();
        try {
            return toDomain(attendanceMessageSendRecordJpaRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException e) {
            // save 실패로 식별자 없는 엔티티가 영속성 컨텍스트에 남아있으면, 그 상태로 이어서 조회할 때
            // Hibernate가 "세션이 오염됐다"는 AssertionFailure를 던진다 — 조회 전에 반드시 비워야 한다.
            // 운영 기본 설정(open-in-view: false, 이 메서드와 호출부 모두 @Transactional 아님)에서는
            // save와 이 조회가 이미 서로 다른 영속성 컨텍스트라 애초에 이 문제가 생기지 않는다 — clear()는
            // @DataJpaTest처럼 하나의 트랜잭션/영속성 컨텍스트를 공유하는 호출 맥락을 위한 방어다. 이 메서드가
            // 나중에 @Transactional로 감싸이면(예: claim과 묶는 방향으로 바뀌면) 운영에서도 다시 필요해질 수 있다.
            entityManager.clear();
            AttendanceMessageSendRecordEntity existing = attendanceMessageSendRecordJpaRepository
                    .findByLectureIdAndStudentIdAndDateAndAttendanceStatus(lectureId, studentId, date, attendanceStatus)
                    .orElseThrow(() -> e);
            return toDomain(reconcileIfStale(existing));
        }
    }

    private AttendanceMessageSendRecordEntity reconcileIfStale(AttendanceMessageSendRecordEntity entity) {
        boolean isStale = entity.getStatus() == AttendanceMessageSendStatus.SENDING
                && entity.getClaimedAt() != null
                && entity.getClaimedAt().isBefore(LocalDateTime.now(clock).minus(CLAIM_STALE_AFTER));
        if (!isStale) {
            return entity;
        }
        entity.changeStatus(AttendanceMessageSendStatus.INDETERMINATE, STALE_SENDING_REASON);
        return attendanceMessageSendRecordJpaRepository.saveAndFlush(entity);
    }

    @Override
    public boolean claimForSending(Long id) {
        int updated = attendanceMessageSendRecordJpaRepository.claimForSending(id, AttendanceMessageSendStatus.SENDING,
                List.of(AttendanceMessageSendStatus.PENDING, AttendanceMessageSendStatus.FAILED),
                LocalDateTime.now(clock));
        return updated == 1;
    }

    @Override
    @Transactional
    public AttendanceMessageSendRecord save(AttendanceMessageSendRecord record) {
        AttendanceMessageSendRecordEntity entity = attendanceMessageSendRecordJpaRepository
                .getReferenceById(record.getId());
        entity.changeStatus(record.getStatus(), record.getFailureReason());
        return toDomain(attendanceMessageSendRecordJpaRepository.saveAndFlush(entity));
    }

    private AttendanceMessageSendRecord toDomain(AttendanceMessageSendRecordEntity entity) {
        return AttendanceMessageSendRecord.restore(entity.getId(), entity.getLectureId(), entity.getStudentId(),
                entity.getDate(), entity.getAttendanceStatus(), entity.getStatus(), entity.getFailureReason(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
