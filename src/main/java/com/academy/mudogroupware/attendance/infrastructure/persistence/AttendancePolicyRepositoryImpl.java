package com.academy.mudogroupware.attendance.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.attendance.domain.model.AttendancePolicy;
import com.academy.mudogroupware.attendance.domain.model.AttendancePolicyWeekday;
import com.academy.mudogroupware.attendance.domain.repository.AttendancePolicyRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AttendancePolicyRepositoryImpl implements AttendancePolicyRepository {

    private final AttendancePolicyJpaRepository attendancePolicyJpaRepository;
    private final AttendancePolicyWeekdayJpaRepository weekdayJpaRepository;

    @Override
    public Optional<AttendancePolicy> findByAcademyId(Long academyId) {
        return attendancePolicyJpaRepository.findByAcademyId(academyId).map(this::toDomain);
    }

    @Override
    public AttendancePolicy save(AttendancePolicy policy) {
        AttendancePolicyJpaEntity saved = attendancePolicyJpaRepository.saveAndFlush(
                AttendancePolicyJpaEntity.builder()
                        .id(policy.getId())
                        .academyId(policy.getAcademyId())
                        .defaultStartTime(policy.getDefaultStartTime())
                        .defaultEndTime(policy.getDefaultEndTime())
                        .lateGraceMinutes(policy.getLateGraceMinutes())
                        .weekdayExceptionEnabled(policy.isWeekdayExceptionEnabled())
                        .createdAt(policy.getCreatedAt())
                        .updatedAt(policy.getUpdatedAt())
                        .build());

        weekdayJpaRepository.deleteAllByIdPolicyId(saved.getId());
        weekdayJpaRepository.flush();
        List<AttendancePolicyWeekdayJpaEntity> weekdays = policy.getWeekdays().stream()
                .map(weekday -> toEntity(saved.getId(), weekday))
                .toList();
        weekdayJpaRepository.saveAllAndFlush(weekdays);

        return toDomain(saved);
    }

    private AttendancePolicy toDomain(AttendancePolicyJpaEntity entity) {
        List<AttendancePolicyWeekday> weekdays = weekdayJpaRepository
                .findAllByIdPolicyIdOrderByIdDayOfWeek(entity.getId()).stream()
                .map(weekday -> new AttendancePolicyWeekday(
                        weekday.getId().getDayOfWeek(), weekday.isWorkday(),
                        weekday.getStartTime(), weekday.getEndTime()))
                .toList();
        return AttendancePolicy.restore(
                entity.getId(), entity.getAcademyId(), entity.getDefaultStartTime(),
                entity.getDefaultEndTime(), entity.getLateGraceMinutes(),
                entity.isWeekdayExceptionEnabled(), weekdays,
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private AttendancePolicyWeekdayJpaEntity toEntity(
            Long policyId, AttendancePolicyWeekday weekday) {
        return AttendancePolicyWeekdayJpaEntity.builder()
                .id(new AttendancePolicyWeekdayId(policyId, weekday.dayOfWeek()))
                .workday(weekday.workday())
                .startTime(weekday.startTime())
                .endTime(weekday.endTime())
                .build();
    }
}
