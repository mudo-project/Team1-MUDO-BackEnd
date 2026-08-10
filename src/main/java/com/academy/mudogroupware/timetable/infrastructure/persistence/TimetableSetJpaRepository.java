package com.academy.mudogroupware.timetable.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TimetableSetJpaRepository extends JpaRepository<TimetableSetEntity, Long> {

    List<TimetableSetEntity> findAllByOrderByStartDateDesc();
}
