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

    // MySQL IN절 파라미터 개수·실행계획 저하를 피하기 위해 1000건 단위로 분할 조회한다.
    private static final int BATCH_SIZE = 1000;

    private final EnrollmentJpaRepository enrollmentJpaRepository;

    @Override
    public Map<Long, Long> findLectureIdsByEnrollmentIds(List<Long> enrollmentIds) {
        if (enrollmentIds == null || enrollmentIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> result = new java.util.HashMap<>();
        for (int from = 0; from < enrollmentIds.size(); from += BATCH_SIZE) {
            int to = Math.min(from + BATCH_SIZE, enrollmentIds.size());
            List<EnrollmentJpaRepository.EnrollmentLectureIdProjection> batchResult =
                    enrollmentJpaRepository.findLectureIdsByIdIn(enrollmentIds.subList(from, to));
            batchResult.forEach(row -> result.put(row.getId(), row.getLectureId()));
        }
        return result;
    }
}
