package com.academy.mudogroupware.lecture.domain.repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.lecture.domain.model.Lecture;

public interface LectureRepository {

    Lecture save(Lecture lecture);

    Optional<Lecture> findById(Long id);

    List<Lecture> findAllById(List<Long> ids);

    List<Lecture> findAll();

    PageResult<Lecture> findAll(LectureFilter filter, int page, int size);

    boolean existsOverlap(String classroomCode, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime);
}
