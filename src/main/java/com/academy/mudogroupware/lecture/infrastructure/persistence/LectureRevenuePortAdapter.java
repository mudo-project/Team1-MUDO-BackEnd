package com.academy.mudogroupware.lecture.infrastructure.persistence;

import java.util.List;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.lecture.domain.model.Lecture;
import com.academy.mudogroupware.lecture.domain.repository.LectureRepository;
import com.academy.mudogroupware.revenuereport.application.port.LectureRevenueInfo;
import com.academy.mudogroupware.revenuereport.application.port.LectureRevenuePort;

import lombok.RequiredArgsConstructor;

/**
 * Consumer: revenuereport
 * Purpose: 매출 리포트 집계에 필요한 강의별 가격·강사명을 제공한다.
 */
@Component
@RequiredArgsConstructor
public class LectureRevenuePortAdapter implements LectureRevenuePort {

    private final LectureRepository lectureRepository;

    @Override
    public List<LectureRevenueInfo> findAll() {
        return lectureRepository.findAll().stream()
                .map(this::toInfo)
                .toList();
    }

    private LectureRevenueInfo toInfo(Lecture lecture) {
        return new LectureRevenueInfo(lecture.getId(), lecture.getName(), lecture.getTeacherName(),
                lecture.getFeeAmount());
    }
}
