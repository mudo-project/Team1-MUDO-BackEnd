package com.academy.mudogroupware.timetable.domain.repository;

import java.util.List;
import java.util.Optional;

import com.academy.mudogroupware.timetable.domain.model.TimetableSet;

public interface TimetableSetRepository {

    TimetableSet save(TimetableSet timetableSet);

    Optional<TimetableSet> findById(Long id);

    List<TimetableSet> findAll();

    void deleteById(Long id);
}
