package com.academy.mudogroupware.student.infrastructure.persistence;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.revenuereport.application.port.ActiveEnrollmentCountPort;
import com.academy.mudogroupware.student.domain.model.EnrollmentStatus;

import lombok.RequiredArgsConstructor;

/**
 * Consumer: revenuereport
 * Purpose: 강의별 활성 수강 등록 수(예상 매출·학생 수 계산용)를 제공한다.
 */
@Component
@RequiredArgsConstructor
public class ActiveEnrollmentCountPortAdapter implements ActiveEnrollmentCountPort {

    private final EnrollmentJpaRepository enrollmentJpaRepository;

    @Override
    public Map<Long, Long> countActiveByLectureIds(List<Long> lectureIds) {
        if (lectureIds == null || lectureIds.isEmpty()) {
            return Map.of();
        }
        return enrollmentJpaRepository.countByLectureIdsAndStatus(lectureIds, EnrollmentStatus.ACTIVE).stream()
                .collect(java.util.stream.Collectors.toMap(
                        EnrollmentJpaRepository.LectureEnrollmentCount::getLectureId,
                        EnrollmentJpaRepository.LectureEnrollmentCount::getCount));
    }
}
