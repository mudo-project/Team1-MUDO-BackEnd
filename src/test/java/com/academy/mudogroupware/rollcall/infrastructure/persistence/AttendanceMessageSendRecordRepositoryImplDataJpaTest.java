package com.academy.mudogroupware.rollcall.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.global.infrastructure.config.TimeConfig;
import com.academy.mudogroupware.rollcall.domain.model.AttendanceMessageSendRecord;
import com.academy.mudogroupware.rollcall.domain.model.AttendanceMessageSendStatus;
import com.academy.mudogroupware.rollcall.domain.model.AttendanceStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({AttendanceMessageSendRecordRepositoryImpl.class, TimeConfig.class})
class AttendanceMessageSendRecordRepositoryImplDataJpaTest {

    private static final LocalDate DATE = LocalDate.of(2026, 8, 5);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 5, 9, 0);
    private static final AttendanceStatus ATTENDANCE_STATUS = AttendanceStatus.ABSENT;

    @Autowired
    private AttendanceMessageSendRecordRepositoryImpl attendanceMessageSendRecordRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void createsANewPendingRecordWhenNoneExistsYet() {
        AttendanceMessageSendRecord record = attendanceMessageSendRecordRepository
                .createOrGetExisting(1L, 10L, DATE, ATTENDANCE_STATUS);

        assertThat(record.getId()).isNotNull();
        assertThat(record.getStatus()).isEqualTo(AttendanceMessageSendStatus.PENDING);
    }

    @Test
    void returnsTheExistingRecordInsteadOfCreatingADuplicate() {
        AttendanceMessageSendRecord first = attendanceMessageSendRecordRepository
                .createOrGetExisting(1L, 10L, DATE, ATTENDANCE_STATUS);
        first.markResult(AttendanceMessageSendStatus.SENT, null, NOW);
        attendanceMessageSendRecordRepository.save(first);

        AttendanceMessageSendRecord second = attendanceMessageSendRecordRepository
                .createOrGetExisting(1L, 10L, DATE, ATTENDANCE_STATUS);

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(second.getStatus()).isEqualTo(AttendanceMessageSendStatus.SENT);
    }

    @Test
    void savePersistsTheUpdatedStatus() {
        AttendanceMessageSendRecord record = attendanceMessageSendRecordRepository
                .createOrGetExisting(1L, 10L, DATE, ATTENDANCE_STATUS);

        record.markResult(AttendanceMessageSendStatus.INDETERMINATE, "타임아웃", NOW);
        attendanceMessageSendRecordRepository.save(record);

        AttendanceMessageSendRecord reloaded = attendanceMessageSendRecordRepository
                .createOrGetExisting(1L, 10L, DATE, ATTENDANCE_STATUS);
        assertThat(reloaded.getStatus()).isEqualTo(AttendanceMessageSendStatus.INDETERMINATE);
        assertThat(reloaded.getFailureReason()).isEqualTo("타임아웃");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void savePersistsTheUpdatedStatusWhenCalledOutsideCallerTransaction() {
        AttendanceMessageSendRecord record = attendanceMessageSendRecordRepository
                .createOrGetExisting(1L, 10L, DATE, ATTENDANCE_STATUS);

        record.markResult(AttendanceMessageSendStatus.SENT, null, NOW);

        assertThatCode(() -> attendanceMessageSendRecordRepository.save(record))
                .doesNotThrowAnyException();

        AttendanceMessageSendRecord reloaded = attendanceMessageSendRecordRepository
                .createOrGetExisting(1L, 10L, DATE, ATTENDANCE_STATUS);
        assertThat(reloaded.getStatus()).isEqualTo(AttendanceMessageSendStatus.SENT);
    }

    @Test
    void treatsACorrectedAttendanceStatusAsANewSendTarget() {
        AttendanceMessageSendRecord absentSend = attendanceMessageSendRecordRepository
                .createOrGetExisting(1L, 10L, DATE, AttendanceStatus.ABSENT);
        absentSend.markResult(AttendanceMessageSendStatus.SENT, null, NOW);
        attendanceMessageSendRecordRepository.save(absentSend);

        AttendanceMessageSendRecord lateSend = attendanceMessageSendRecordRepository
                .createOrGetExisting(1L, 10L, DATE, AttendanceStatus.LATE);

        assertThat(lateSend.getId()).isNotEqualTo(absentSend.getId());
        assertThat(lateSend.getStatus()).isEqualTo(AttendanceMessageSendStatus.PENDING);
    }

    @Test
    void claimForSendingSucceedsOnceForAPendingRecord() {
        AttendanceMessageSendRecord record = attendanceMessageSendRecordRepository
                .createOrGetExisting(1L, 10L, DATE, ATTENDANCE_STATUS);

        boolean firstClaim = attendanceMessageSendRecordRepository.claimForSending(record.getId());
        boolean secondClaim = attendanceMessageSendRecordRepository.claimForSending(record.getId());

        assertThat(firstClaim).isTrue();
        assertThat(secondClaim).isFalse();
    }

    @Test
    void claimForSendingFailsWhenRecordIsIndeterminate() {
        AttendanceMessageSendRecord record = attendanceMessageSendRecordRepository
                .createOrGetExisting(1L, 10L, DATE, ATTENDANCE_STATUS);
        record.markResult(AttendanceMessageSendStatus.INDETERMINATE, "타임아웃", NOW);
        attendanceMessageSendRecordRepository.save(record);

        boolean claimed = attendanceMessageSendRecordRepository.claimForSending(record.getId());

        assertThat(claimed).isFalse();
    }

    @Test
    void claimForSendingSucceedsAgainAfterAPreviousFailure() {
        AttendanceMessageSendRecord record = attendanceMessageSendRecordRepository
                .createOrGetExisting(1L, 10L, DATE, ATTENDANCE_STATUS);
        attendanceMessageSendRecordRepository.claimForSending(record.getId());
        record.markResult(AttendanceMessageSendStatus.FAILED, "인증 오류", NOW);
        attendanceMessageSendRecordRepository.save(record);

        boolean claimed = attendanceMessageSendRecordRepository.claimForSending(record.getId());

        assertThat(claimed).isTrue();
    }

    @Test
    void claimForSendingFailsForARecentlyClaimedSendingRecord() {
        AttendanceMessageSendRecord record = attendanceMessageSendRecordRepository
                .createOrGetExisting(1L, 10L, DATE, ATTENDANCE_STATUS);
        attendanceMessageSendRecordRepository.claimForSending(record.getId());

        boolean reclaimed = attendanceMessageSendRecordRepository.claimForSending(record.getId());

        assertThat(reclaimed).isFalse();
    }

    @Test
    @Transactional
    void neverReclaimsAStaleSendingRecordForAnotherLiveSendAttempt() {
        AttendanceMessageSendRecord record = attendanceMessageSendRecordRepository
                .createOrGetExisting(1L, 10L, DATE, ATTENDANCE_STATUS);
        attendanceMessageSendRecordRepository.claimForSending(record.getId());
        backdateClaimedAt(record.getId(), LocalDateTime.now().minusMinutes(10));

        // claimForSending 자체는 만료됐다고 다시 SENDING을 내주지 않는다 — 공급자 상태 조회 없이
        // 시간만으로 재발송을 허용하면, 원래 요청이 실제로는 살아있는 경우 중복 발송이 될 수 있다.
        boolean reclaimed = attendanceMessageSendRecordRepository.claimForSending(record.getId());

        assertThat(reclaimed).isFalse();
    }

    @Test
    @Transactional
    void treatsAStaleSendingRecordAsIndeterminateOnNextLookupInsteadOfAutoResending() {
        AttendanceMessageSendRecord record = attendanceMessageSendRecordRepository
                .createOrGetExisting(1L, 10L, DATE, ATTENDANCE_STATUS);
        attendanceMessageSendRecordRepository.claimForSending(record.getId());
        backdateClaimedAt(record.getId(), LocalDateTime.now().minusMinutes(10));

        AttendanceMessageSendRecord reconciled = attendanceMessageSendRecordRepository
                .createOrGetExisting(1L, 10L, DATE, ATTENDANCE_STATUS);

        assertThat(reconciled.getId()).isEqualTo(record.getId());
        assertThat(reconciled.getStatus()).isEqualTo(AttendanceMessageSendStatus.INDETERMINATE);
        assertThat(reconciled.isIndeterminate()).isTrue();
    }

    @Test
    @Transactional
    void doesNotReconcileASendingRecordThatIsStillWithinTheClaimWindow() {
        AttendanceMessageSendRecord record = attendanceMessageSendRecordRepository
                .createOrGetExisting(1L, 10L, DATE, ATTENDANCE_STATUS);
        attendanceMessageSendRecordRepository.claimForSending(record.getId());

        AttendanceMessageSendRecord result = attendanceMessageSendRecordRepository
                .createOrGetExisting(1L, 10L, DATE, ATTENDANCE_STATUS);

        assertThat(result.getStatus()).isEqualTo(AttendanceMessageSendStatus.SENDING);
    }

    private void backdateClaimedAt(Long id, LocalDateTime claimedAt) {
        // 발송 도중 프로세스가 죽은 상황을 흉내낸다: claimed_at을 충분히 오래된 시각으로 되돌린다.
        entityManager.createQuery("update AttendanceMessageSendRecordEntity e set e.claimedAt = :claimedAt where e.id = :id")
                .setParameter("claimedAt", claimedAt)
                .setParameter("id", id)
                .executeUpdate();
        entityManager.clear();
    }
}
