package com.academy.mudogroupware.timetable.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface TimetableSetJpaRepository extends JpaRepository<TimetableSetEntity, Long> {

    List<TimetableSetEntity> findAllByOrderByStartDateDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TimetableSetEntity t where t.id = :id")
    Optional<TimetableSetEntity> findByIdForUpdate(Long id);
}
