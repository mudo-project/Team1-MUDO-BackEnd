package com.academy.mudogroupware.student.infrastructure.persistence;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.revenuereport.application.port.EnrollmentLectureLookupPort;

import lombok.RequiredArgsConstructor;

/**
 * Consumer: revenuereport
 * Purpose: payment.enrollmentId로부터 강의별 실 매출을 집계하기 위한 매핑을 제공한다.
 */
@Component
@RequiredArgsConstructor
public class EnrollmentLectureLookupPortAdapter implements EnrollmentLectureLookupPort {

    private final EnrollmentJpaRepository enrollmentJpaRepository;

    @Override
    public Map<Long, Long> findLectureIdsByEnrollmentIds(List<Long> enrollmentIds) {
        if (enrollmentIds == null || enrollmentIds.isEmpty()) {
            return Map.of();
        }
        return enrollmentJpaRepository.findAllById(enrollmentIds).stream()
                .collect(Collectors.toMap(EnrollmentEntity::getId, EnrollmentEntity::getLectureId));
    }
}
