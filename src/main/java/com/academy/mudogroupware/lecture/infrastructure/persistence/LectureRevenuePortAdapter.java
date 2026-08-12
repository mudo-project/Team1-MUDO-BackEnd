package com.academy.mudogroupware.lecture.infrastructure.persistence;

import java.util.List;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.revenuereport.application.port.LectureRevenueInfo;
import com.academy.mudogroupware.revenuereport.application.port.LectureRevenuePort;

import lombok.RequiredArgsConstructor;

/**
 * Consumer: revenuereport
 * Purpose: 매출 리포트 집계에 필요한 강의별 가격·강사명을 제공한다. Lecture aggregate 전체를
 * 복원하지 않고 projection 쿼리로 필요한 열만 뽑는다(schedules 지연로딩 문제 회피 겸 성능).
 */
@Component
@RequiredArgsConstructor
public class LectureRevenuePortAdapter implements LectureRevenuePort {

    private final LectureJpaRepository lectureJpaRepository;

    @Override
    public List<LectureRevenueInfo> findAll() {
        return lectureJpaRepository.findAllRevenueProjection().stream()
                .map(this::toInfo)
                .toList();
    }

    private LectureRevenueInfo toInfo(LectureJpaRepository.LectureRevenueProjection projection) {
        return new LectureRevenueInfo(projection.getId(), projection.getName(), projection.getTeacherName(),
                projection.getFeeAmount());
    }
}
