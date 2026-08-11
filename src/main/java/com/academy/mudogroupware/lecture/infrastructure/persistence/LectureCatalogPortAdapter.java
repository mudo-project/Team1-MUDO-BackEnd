package com.academy.mudogroupware.lecture.infrastructure.persistence;

import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.lecture.application.port.TeacherDirectoryPort;
import com.academy.mudogroupware.lecture.application.port.TeacherInfo;
import com.academy.mudogroupware.lecture.domain.model.Lecture;
import com.academy.mudogroupware.lecture.domain.model.LectureSchedule;
import com.academy.mudogroupware.lecture.domain.repository.LectureRepository;
import com.academy.mudogroupware.student.application.port.LectureCatalogInfo;
import com.academy.mudogroupware.student.application.port.LectureCatalogPort;

import lombok.RequiredArgsConstructor;

/**
 * Consumer: student
 * Purpose: Resolve lecture catalog info for student detail, including teacher names through users.
 */
@Component
@RequiredArgsConstructor
public class LectureCatalogPortAdapter implements LectureCatalogPort {

    private final LectureRepository lectureRepository;
    private final TeacherDirectoryPort teacherDirectoryPort;

    @Override
    public Map<Long, LectureCatalogInfo> findByIds(List<Long> lectureIds) {
        List<Lecture> lectures = lectureRepository.findAllById(lectureIds);
        List<Long> teacherIds = lectures.stream()
                .filter(lecture -> !hasText(lecture.getTeacherName()))
                .map(Lecture::getTeacherId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, TeacherInfo> teachers = teacherIds.isEmpty() ? Map.of() : teacherDirectoryPort.findTeachers(teacherIds);

        Map<Long, LectureCatalogInfo> result = new HashMap<>();
        for (Lecture lecture : lectures) {
            result.put(lecture.getId(), new LectureCatalogInfo(
                    lecture.getId(),
                    lecture.getName(),
                    teacherName(lecture, teachers),
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

    private String teacherName(Lecture lecture, Map<Long, TeacherInfo> teachers) {
        if (hasText(lecture.getTeacherName())) {
            return lecture.getTeacherName();
        }
        TeacherInfo teacher = lecture.getTeacherId() != null ? teachers.get(lecture.getTeacherId()) : null;
        return teacher != null ? teacher.name() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
