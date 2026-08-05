package com.academy.mudogroupware.lecture.infrastructure.persistence;

import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.lecture.domain.model.Lecture;
import com.academy.mudogroupware.lecture.domain.model.LectureSchedule;
import com.academy.mudogroupware.lecture.domain.repository.LectureRepository;
import com.academy.mudogroupware.student.application.port.LectureCatalogInfo;
import com.academy.mudogroupware.student.application.port.LectureCatalogPort;

import lombok.RequiredArgsConstructor;

/**
 * Consumer: student
 * Purpose: 학생 상세의 수강 강의 목록에 필요한 강의명/일정/수강료 정보 조회.
 * 담당 선생님 이름은 users 모듈 직원 검색 Port 승인 전까지 제공하지 않는다(null).
 */
@Component
@RequiredArgsConstructor
public class LectureCatalogPortAdapter implements LectureCatalogPort {

    private final LectureRepository lectureRepository;

    @Override
    public Map<Long, LectureCatalogInfo> findByIds(Long academyId, List<Long> lectureIds) {
        Map<Long, LectureCatalogInfo> result = new HashMap<>();
        for (Lecture lecture : lectureRepository.findAllById(lectureIds)) {
            if (!lecture.getAcademyId().equals(academyId)) {
                continue;
            }
            result.put(lecture.getId(), new LectureCatalogInfo(
                    lecture.getId(),
                    lecture.getName(),
                    null,
                    formatSchedule(lecture.getSchedules()),
                    lecture.getFeeType() != null ? lecture.getFeeType().name() : null,
                    lecture.getFeeAmount()));
        }
        return result;
    }

    private String formatSchedule(List<LectureSchedule> schedules) {
        return schedules.stream()
                .map(s -> dayLabel(s.getDayOfWeek()) + " " + s.getStartTime() + "-" + s.getEndTime())
                .collect(Collectors.joining(", "));
    }

    private String dayLabel(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "월";
            case TUESDAY -> "화";
            case WEDNESDAY -> "수";
            case THURSDAY -> "목";
            case FRIDAY -> "금";
            case SATURDAY -> "토";
            case SUNDAY -> "일";
        };
    }
}
