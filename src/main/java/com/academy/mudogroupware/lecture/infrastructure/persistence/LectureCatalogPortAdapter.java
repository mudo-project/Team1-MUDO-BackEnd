package com.academy.mudogroupware.lecture.infrastructure.persistence;

import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        Map<Long, TeacherInfo> teachers = teacherDirectoryPort.findTeachers(
                lectures.stream().map(Lecture::getTeacherId).distinct().toList());

        Map<Long, LectureCatalogInfo> result = new HashMap<>();
        for (Lecture lecture : lectures) {
            result.put(lecture.getId(), new LectureCatalogInfo(
                    lecture.getId(),
                    lecture.getName(),
                    teacherName(teachers, lecture.getTeacherId()),
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

    private String teacherName(Map<Long, TeacherInfo> teachers, Long teacherId) {
        TeacherInfo teacher = teachers.get(teacherId);
        return teacher != null ? teacher.name() : null;
    }
}
